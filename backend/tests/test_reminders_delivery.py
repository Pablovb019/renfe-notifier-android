"""Pruebas del bucle de entrega de avisos (sin red real, sender simulado)."""

from dataclasses import dataclass
from datetime import UTC, date, datetime, timedelta
from pathlib import Path

import pytest

from app.followups.database import FollowUpRepository
from app.followups.domain import FollowUp, FollowUpMode
from app.notifications.alerts import build_alert_message
from app.notifications.fcm import FcmResult
from app.pairing.database import PairingRepository
from app.pairing.domain import Device, hash_secret
from app.pairing.service import PairingService
from app.reminders.delivery import AlertDeliveryService
from app.reminders.domain import ReminderEvent, ReminderStatus
from app.reminders.queue import AlertQueue
from app.renfe.stations import Station, StationCatalog

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)
EXPIRES = datetime(2026, 3, 15, tzinfo=UTC)
TRAVEL_DATE = date(2026, 3, 15)


@dataclass
class Env:
    queue: AlertQueue
    repo: FollowUpRepository
    pairing: PairingService
    pairing_repo: PairingRepository
    catalog: StationCatalog
    followup_id: str
    episode_id: int
    origin: Station
    destination: Station

    def device(self, device_id: str = "dev-1", *, fcm_token: str = "fcm-1") -> Device:
        self.pairing_repo.add_device(
            Device(
                device_id=device_id,
                device_name="realme GT Neo 2",
                token_hash=hash_secret("shhh"),
                created_at=NOW,
                fcm_token=fcm_token,
            )
        )
        device = self.pairing_repo.get_device_by_id(device_id)
        assert device is not None
        return device


@pytest.fixture
def env(tmp_path: Path) -> Env:
    db = tmp_path / "data" / "test.db"
    repo = FollowUpRepository(db)
    repo.initialize()
    queue = AlertQueue(db, interval_s=60.0)
    queue.initialize()
    pairing_repo = PairingRepository(db)
    pairing_repo.initialize()
    pairing = PairingService(pairing_repo)
    catalog = StationCatalog()
    concrete = [station for station in catalog.stations if not station.is_group]
    origin, destination = concrete[0], concrete[1]
    followup = FollowUp.create(
        followup_id="followup-1",
        origin_code=origin.code,
        destination_code=destination.code,
        travel_date=TRAVEL_DATE,
        mode=FollowUpMode.FIRST,
        now=NOW,
    )
    repo.create(followup)
    episode = repo.add_episode("followup-1", 1, NOW, frozenset({"real:100"}))
    return Env(
        queue=queue,
        repo=repo,
        pairing=pairing,
        pairing_repo=pairing_repo,
        catalog=catalog,
        followup_id="followup-1",
        episode_id=episode.episode_id,
        origin=origin,
        destination=destination,
    )


def _event(env: Env, event_id: str = "event-1") -> ReminderEvent:
    return ReminderEvent.create(
        event_id=event_id,
        followup_id=env.followup_id,
        episode_id=env.episode_id,
        observed_at=NOW,
        expires_at=EXPIRES,
        initial_delay_s=60.0,
        max_attempts=3,
        now=NOW,
    )


def _service(env: Env, sender: object, *, batch: int = 20) -> AlertDeliveryService:
    return AlertDeliveryService(
        queue=env.queue,
        followups=env.repo,
        pairing=env.pairing,
        catalog=env.catalog,
        sender=sender,  # type: ignore[arg-type]
        clock=lambda: NOW + timedelta(seconds=61),
        batch=batch,
    )


class FakeSender:
    def __init__(self, result: FcmResult) -> None:
        self._result = result
        self.calls: list[tuple[str, object]] = []

    async def send_alert(self, *, token: str, alert: object) -> FcmResult:
        self.calls.append((token, alert))
        return self._result


class RaisingSender:
    def __init__(self, error: Exception) -> None:
        self._error = error
        self.calls = 0

    async def send_alert(self, *, token: str, alert: object) -> FcmResult:
        self.calls += 1
        raise self._error


@pytest.mark.asyncio
async def test_run_once_without_sender_is_noop(env: Env) -> None:
    env.queue.enqueue(_event(env))

    service = _service(env, None)
    assert await service.run_once() == 0
    assert env.queue.get("event-1") is not None


@pytest.mark.asyncio
async def test_run_once_delivers_due_event_to_active_device(env: Env) -> None:
    env.queue.enqueue(_event(env))
    env.device()

    sender = FakeSender(FcmResult(accepted=True, message_id="m-1"))
    service = _service(env, sender)

    assert await service.run_once() == 1
    assert (sender.calls[0][0]) == "fcm-1"
    event = env.queue.get("event-1")
    assert event is not None
    assert event.status is ReminderStatus.PENDING
    assert event.attempts == 1


@pytest.mark.asyncio
async def test_run_once_builds_alert_with_catalog_names(env: Env) -> None:
    env.queue.enqueue(_event(env))
    env.device()

    sender = FakeSender(FcmResult(accepted=True, message_id="m-1"))
    service = _service(env, sender)

    await service.run_once()

    _, alert = sender.calls[0]
    data = alert.as_data()  # type: ignore[attr-defined]
    assert data["type"] == "alert"
    assert data["followup_id"] == "followup-1"
    assert data["episode_id"] == str(env.episode_id)
    assert data["origin"] == env.origin.name
    assert data["destination"] == env.destination.name
    assert data["travel_date"] == TRAVEL_DATE.isoformat()
    assert "collapse_key" not in data  # el collapse_key va en el mensaje, no en el payload


@pytest.mark.asyncio
async def test_run_once_clears_invalid_token_and_fails(env: Env) -> None:
    env.queue.enqueue(_event(env))
    env.device()

    sender = FakeSender(FcmResult(accepted=False, token_invalid=True, error_code="UNREGISTERED"))
    service = _service(env, sender)

    assert await service.run_once() == 1
    device = env.pairing_repo.get_device_by_id("dev-1")
    assert device is not None
    assert device.fcm_token is None
    event = env.queue.get("event-1")
    assert event is not None
    assert event.status is ReminderStatus.FAILED
    assert event.cancelled_reason == "token_invalid"


@pytest.mark.asyncio
async def test_run_once_leaves_pending_when_no_devices(env: Env) -> None:
    env.queue.enqueue(_event(env))

    sender = FakeSender(FcmResult(accepted=True, message_id="m-1"))
    service = _service(env, sender)

    assert await service.run_once() == 0
    assert sender.calls == []
    event = env.queue.get("event-1")
    assert event is not None
    assert event.status is ReminderStatus.PENDING


@pytest.mark.asyncio
async def test_run_once_cancels_when_followup_is_inactive(env: Env) -> None:
    followup = env.repo.get(env.followup_id)
    assert followup is not None
    env.repo.save(followup.pause(), now=NOW)
    env.queue.enqueue(_event(env))
    env.device()

    sender = FakeSender(FcmResult(accepted=True, message_id="m-1"))
    service = _service(env, sender)

    assert await service.run_once() == 1
    assert sender.calls == []
    event = env.queue.get("event-1")
    assert event is not None
    assert event.status is ReminderStatus.CANCELLED


@pytest.mark.asyncio
async def test_run_once_cancels_when_event_expired(env: Env) -> None:
    env.queue.enqueue(_event(env))
    env.device()

    service = AlertDeliveryService(
        queue=env.queue,
        followups=env.repo,
        pairing=env.pairing,
        catalog=env.catalog,
        sender=FakeSender(FcmResult(accepted=True, message_id="m-1")),  # type: ignore[arg-type]
        clock=lambda: EXPIRES + timedelta(days=1),
    )

    assert await service.run_once() == 1
    event = env.queue.get("event-1")
    assert event is not None
    assert event.status is ReminderStatus.CANCELLED
    assert event.cancelled_reason == "expired"


@pytest.mark.asyncio
async def test_run_once_keeps_pending_on_retryable_error(env: Env) -> None:
    from app.notifications.fcm import FcmRejectedError

    env.queue.enqueue(_event(env))
    env.device()

    sender = RaisingSender(FcmRejectedError(code="UNAVAILABLE", message="503", retryable=True))
    service = _service(env, sender)

    assert await service.run_once() == 0
    event = env.queue.get("event-1")
    assert event is not None
    assert event.status is ReminderStatus.PENDING
    assert event.attempts == 0


@pytest.mark.asyncio
async def test_run_once_marks_failed_on_non_retryable_error(env: Env) -> None:
    from app.notifications.fcm import FcmRejectedError

    env.queue.enqueue(_event(env))
    env.device()

    sender = RaisingSender(FcmRejectedError(code="INVALID_ARGUMENT", message="400"))
    service = _service(env, sender)

    assert await service.run_once() == 1
    event = env.queue.get("event-1")
    assert event is not None
    assert event.status is ReminderStatus.FAILED
    assert event.cancelled_reason == "rejected:INVALID_ARGUMENT"


@pytest.mark.asyncio
async def test_run_once_skips_events_without_active_followup(env: Env) -> None:
    env.queue.enqueue(_event(env))
    followup = env.repo.get(env.followup_id)
    assert followup is not None
    env.repo.save(followup.delete(), now=NOW)
    env.device()

    sender = FakeSender(FcmResult(accepted=True, message_id="m-1"))
    service = _service(env, sender)

    assert await service.run_once() == 1
    assert sender.calls == []


@pytest.mark.asyncio
async def test_run_forever_cancels_tasks_cleanly(env: Env) -> None:
    env.device()
    env.queue.enqueue(_event(env))
    sleep_count = 0

    async def stopping_sleep(delay: float) -> None:
        nonlocal sleep_count
        sleep_count += 1
        if sleep_count >= 2:
            raise TimeoutError("basta")

    service = AlertDeliveryService(
        queue=env.queue,
        followups=env.repo,
        pairing=env.pairing,
        catalog=env.catalog,
        sender=FakeSender(FcmResult(accepted=True, message_id="m-1")),  # type: ignore[arg-type]
        clock=lambda: NOW + timedelta(seconds=61),
        sleep=stopping_sleep,
    )

    with pytest.raises(TimeoutError):
        await service.run_forever()
    assert sleep_count == 2


def test_build_alert_message_contract(env: Env) -> None:
    alert = build_alert_message(
        event=_event(env),
        followup=env.repo.get(env.followup_id),  # type: ignore[arg-type]
        catalog=env.catalog,
    )
    data = alert.as_data()
    assert data["type"] == "alert"
    assert data["event_id"] == "event-1"
    assert data["followup_id"] == "followup-1"
    assert data["episode_id"] == str(env.episode_id)
    assert data["origin_code"] == env.origin.code
    assert data["origin"] == env.origin.name
    assert data["destination_code"] == env.destination.code
    assert data["destination"] == env.destination.name
    assert data["travel_date"] == TRAVEL_DATE.isoformat()
    assert data["observed_at"] == NOW.isoformat()
    assert data["expires_at"] == EXPIRES.isoformat()
    assert data["channel_id"] == "disponibilidad_plazas_v2"
    assert data["priority"] == "high"
    assert data["title"] == "Plazas disponibles"
