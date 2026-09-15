from dataclasses import dataclass
from datetime import UTC, date, datetime, timedelta
from pathlib import Path

import pytest

from app.followups.database import FollowUpRepository
from app.followups.domain import FollowUp, FollowUpMode
from app.reminders.domain import (
    REPLACED,
    STALE,
    ReminderEvent,
    ReminderStatus,
)
from app.reminders.queue import AlertQueue

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)
EXPIRES = datetime(2026, 3, 15, tzinfo=UTC)


@dataclass
class Env:
    queue: AlertQueue
    repo: FollowUpRepository
    episode_id: int

    def add_followup(self, followup_id: str, *, origin: str = "00001", episode: int = 1) -> None:
        followup = _followup(followup_id, origin)
        self.repo.create(followup)
        self.repo.add_episode(followup_id, episode, NOW, frozenset({"real:100"}))


@pytest.fixture
def env(tmp_path: Path) -> Env:
    repo = FollowUpRepository(tmp_path / "data" / "test.db")
    repo.initialize()
    queue = AlertQueue(tmp_path / "data" / "test.db", interval_s=60.0)
    queue.initialize()
    repo.create(_followup())
    episode = repo.add_episode("followup-1", 1, NOW, frozenset({"real:100"}))
    return Env(queue=queue, repo=repo, episode_id=episode.episode_id)


def _followup(followup_id: str = "followup-1", origin_code: str = "00001") -> FollowUp:
    return FollowUp.create(
        followup_id=followup_id,
        origin_code=origin_code,
        destination_code="00002",
        travel_date=date(2026, 3, 15),
        mode=FollowUpMode.SPECIFIC,
        now=NOW,
        specific_train_id="real:100",
    )


def _event(
    env: Env,
    event_id: str = "event-1",
    followup_id: str = "followup-1",
    *,
    observed_at: datetime = NOW,
    initial_delay_s: float = 60.0,
    max_attempts: int = 3,
) -> ReminderEvent:
    return ReminderEvent.create(
        event_id=event_id,
        followup_id=followup_id,
        episode_id=env.episode_id,
        observed_at=observed_at,
        expires_at=EXPIRES,
        initial_delay_s=initial_delay_s,
        max_attempts=max_attempts,
        now=observed_at,
    )


def test_enqueue_and_get_roundtrip(env: Env) -> None:
    assert env.queue.enqueue(_event(env))
    stored = env.queue.get("event-1")

    assert stored is not None
    assert stored.followup_id == "followup-1"
    assert stored.episode_id == env.episode_id
    assert stored.observed_at == NOW
    assert stored.expires_at == EXPIRES
    assert stored.status is ReminderStatus.PENDING
    assert stored.attempts == 0
    assert stored.max_attempts == 3
    assert stored.remind_at == NOW + timedelta(seconds=60)


def test_enqueue_duplicate_is_idempotent(env: Env) -> None:
    assert env.queue.enqueue(_event(env))
    assert not env.queue.enqueue(_event(env))

    assert env.queue.count_pending() == 1
    stored = env.queue.get("event-1")
    assert stored is not None
    assert stored.attempts == 0


def test_enqueue_replaces_previous_event_of_same_followup(env: Env) -> None:
    env.queue.enqueue(_event(env))
    assert env.queue.enqueue(_event(env, event_id="event-2"))

    replaced = env.queue.get("event-1")
    assert replaced is not None
    assert replaced.status is ReminderStatus.CANCELLED
    assert replaced.cancelled_reason == REPLACED
    assert [e.event_id for e in env.queue.due_events(NOW + timedelta(seconds=61), 10)] == [
        "event-2"
    ]


def test_enqueue_keeps_events_of_other_followups(env: Env) -> None:
    env.add_followup("followup-2")
    env.queue.enqueue(_event(env))
    env.queue.enqueue(_event(env, event_id="event-2", followup_id="followup-2"))

    assert env.queue.count_pending() == 2


def test_due_events_respects_initial_delay(env: Env) -> None:
    env.queue.enqueue(_event(env))

    assert env.queue.due_events(NOW, 10) == ()
    due = env.queue.due_events(NOW + timedelta(seconds=60), 10)
    assert [e.event_id for e in due] == ["event-1"]


def test_deliver_consumes_attempt_and_programs_next(env: Env) -> None:
    env.queue.enqueue(_event(env, max_attempts=3))
    delivered_at = NOW + timedelta(seconds=61)

    assert env.queue.deliver("event-1", delivered_at)
    first = env.queue.get("event-1")
    assert first is not None
    assert first.attempts == 1
    assert first.status is ReminderStatus.PENDING
    assert first.remind_at == delivered_at + timedelta(seconds=60)
    assert env.queue.due_events(delivered_at, 10) == ()

    assert env.queue.deliver("event-1", delivered_at + timedelta(seconds=61))
    second = env.queue.get("event-1")
    assert second is not None
    assert second.attempts == 2

    assert env.queue.deliver("event-1", delivered_at + timedelta(seconds=121))
    done = env.queue.get("event-1")
    assert done is not None
    assert done.attempts == 3
    assert done.status is ReminderStatus.DELIVERED
    assert done.delivered_at == delivered_at + timedelta(seconds=121)
    assert env.queue.count_pending() == 0


def test_deliver_is_bounded_by_max_attempts(env: Env) -> None:
    env.queue.enqueue(_event(env, max_attempts=2))
    window = NOW + timedelta(seconds=61)

    assert env.queue.deliver("event-1", window)
    assert env.queue.deliver("event-1", window + timedelta(seconds=60))
    assert not env.queue.deliver("event-1", window + timedelta(seconds=120))

    done = env.queue.get("event-1")
    assert done is not None
    assert done.attempts == 2
    assert done.delivered_at == window + timedelta(seconds=60)


def test_deliver_unknown_or_delivered_is_noop(env: Env) -> None:
    assert not env.queue.deliver("missing", NOW)


def test_mark_failed_flags_event_once(env: Env) -> None:
    env.queue.enqueue(_event(env))
    assert env.queue.mark_failed("event-1", "transport_error")

    failed = env.queue.get("event-1")
    assert failed is not None
    assert failed.status is ReminderStatus.FAILED
    assert failed.cancelled_reason == "transport_error"
    assert not env.queue.mark_failed("event-1", "again")
    assert env.queue.due_events(NOW + timedelta(seconds=61), 10) == ()


@pytest.mark.parametrize(
    "reason",
    ["acknowledged", "paused", "expired", "unavailable"],
)
def test_cancel_for_followup_sets_reason(env: Env, reason: str) -> None:
    env.add_followup("followup-2", origin="00002")
    env.queue.enqueue(_event(env))
    env.queue.enqueue(_event(env, event_id="event-2", followup_id="followup-2"))

    assert env.queue.cancel_for_followup("followup-1", reason) == 1

    cancelled = env.queue.get("event-1")
    assert cancelled is not None
    assert cancelled.status is ReminderStatus.CANCELLED
    assert cancelled.cancelled_reason == reason
    assert env.queue.count_pending() == 1
    due_ids = {e.event_id for e in env.queue.due_events(NOW + timedelta(seconds=61), 10)}
    assert "event-1" not in due_ids
    assert "event-2" in due_ids


def test_suspend_hides_pending_events(env: Env) -> None:
    env.add_followup("followup-2", origin="00002")
    env.queue.enqueue(_event(env))
    env.queue.enqueue(_event(env, event_id="event-2", followup_id="followup-2"))

    assert env.queue.suspend_for_followup("followup-1") == 1

    suspended = env.queue.get("event-1")
    assert suspended is not None
    assert suspended.status is ReminderStatus.SUSPENDED
    assert env.queue.count_pending() == 1
    due_ids = {e.event_id for e in env.queue.due_events(NOW + timedelta(seconds=61), 10)}
    assert "event-1" not in due_ids
    assert "event-2" in due_ids


def test_suspend_can_be_cancelled_after_stale(env: Env) -> None:
    env.queue.enqueue(_event(env))
    env.queue.suspend_for_followup("followup-1")

    assert env.queue.cancel_for_followup("followup-1", STALE) == 1

    done = env.queue.get("event-1")
    assert done is not None
    assert done.status is ReminderStatus.CANCELLED
    assert done.cancelled_reason == STALE


def test_restart_recovers_pending_state(env: Env, tmp_path: Path) -> None:
    env.queue.enqueue(_event(env, max_attempts=3))
    env.queue.deliver("event-1", NOW + timedelta(seconds=61))

    restarted_queue = AlertQueue(tmp_path / "data" / "test.db", interval_s=60.0)
    restarted_queue.initialize()
    recovered = restarted_queue.get("event-1")

    assert recovered is not None
    assert recovered.attempts == 1
    assert recovered.remind_at == NOW + timedelta(seconds=61) + timedelta(seconds=60)
    due = restarted_queue.due_events(NOW + timedelta(seconds=121), 10)
    assert [e.event_id for e in due] == ["event-1"]
    assert restarted_queue.deliver("event-1", NOW + timedelta(seconds=121))


def test_events_are_attached_to_persisted_episodes(env: Env) -> None:
    env.queue.enqueue(_event(env, event_id="event-1"))

    stored = env.queue.get("event-1")
    assert stored is not None
    assert stored.followup_id == "followup-1"
    assert stored.episode_id == env.episode_id
