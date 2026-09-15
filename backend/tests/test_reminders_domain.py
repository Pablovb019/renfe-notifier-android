from datetime import UTC, datetime, timedelta

import pytest

from app.reminders.domain import (
    REPLACED,
    STALE,
    ReminderEvent,
    ReminderStatus,
    first_remind_at,
    from_epoch,
    next_remind_at,
    to_epoch,
)

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)
EXPIRES = datetime(2026, 3, 15, tzinfo=UTC)


def test_create_schedules_first_reminder_after_delay() -> None:
    event = ReminderEvent.create(
        event_id="event-1",
        followup_id="followup-1",
        episode_id=7,
        observed_at=NOW,
        expires_at=EXPIRES,
        initial_delay_s=60.0,
        max_attempts=3,
        now=NOW,
    )

    assert event.status is ReminderStatus.PENDING
    assert event.attempts == 0
    assert event.max_attempts == 3
    assert event.remind_at == NOW + timedelta(seconds=60)


def test_create_rejects_naive_datetimes() -> None:
    with pytest.raises(ValueError):
        ReminderEvent.create(
            event_id="event-1",
            followup_id="followup-1",
            episode_id=7,
            observed_at=datetime(2026, 3, 1, 12),
            expires_at=EXPIRES,
            initial_delay_s=60.0,
            max_attempts=3,
            now=NOW,
        )


def test_first_remind_at_keeps_timezone() -> None:
    assert first_remind_at(NOW, 5.0) == NOW + timedelta(seconds=5)


def test_next_remind_at_keeps_timezone() -> None:
    assert next_remind_at(NOW, 300.0) == NOW + timedelta(seconds=300)


def test_to_and_from_epoch_are_inverse() -> None:
    assert from_epoch(to_epoch(NOW)) == NOW


def test_replacement_and_stale_reasons_exist() -> None:
    assert REPLACED == "replaced"
    assert STALE == "stale"


def test_status_string_values() -> None:
    assert ReminderStatus.PENDING.value == "pending"
    assert ReminderStatus.SUSPENDED.value == "suspended"
    assert ReminderStatus.CANCELLED.value == "cancelled"
    assert ReminderStatus.DELIVERED.value == "delivered"
    assert ReminderStatus.FAILED.value == "failed"
