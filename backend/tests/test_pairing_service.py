from datetime import UTC, datetime, timedelta
from pathlib import Path

import pytest

from app.pairing.database import PairingRepository
from app.pairing.domain import Device, hash_secret
from app.pairing.service import PairingError, PairingService

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)


class FakeClock:
    """Reloj determinista que permite avanzar el tiempo en las pruebas."""

    def __init__(self, start: datetime) -> None:
        self.now = start

    def __call__(self) -> datetime:
        return self.now

    def advance(self, seconds: float) -> None:
        self.now += timedelta(seconds=seconds)


@pytest.fixture
def env(tmp_path: Path) -> tuple[PairingService, PairingRepository, FakeClock]:
    repository = PairingRepository(tmp_path / "data" / "test.db")
    clock = FakeClock(NOW)
    service = PairingService(repository, code_ttl_s=600.0, max_attempts=3, clock=clock)
    service.initialize()
    return service, repository, clock


def test_create_code_stores_only_hash(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, repository, _ = env

    code = service.create_pairing_code()

    stored = repository.get_by_code_hash(hash_secret(code))
    assert stored is not None
    assert stored.code_hash != code
    assert not stored.is_claimed()


def test_claim_returns_token_and_verifies(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, _, _ = env
    code = service.create_pairing_code()

    device_token = service.claim_pairing(code, "device-1", "realme GT Neo 2")

    assert len(device_token) >= 32
    device = service.get_device_by_token(device_token)
    assert device is not None
    assert device.device_id == "device-1"
    assert device.device_name == "realme GT Neo 2"
    assert device.token_hash == hash_secret(device_token)


def test_claim_invalid_code_raises(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, _, _ = env

    with pytest.raises(PairingError):
        service.claim_pairing("RF-000000", "device-1", "realme")


def test_claim_expired_code_raises(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, _, clock = env
    code = service.create_pairing_code()
    clock.advance(601.0)

    with pytest.raises(PairingError):
        service.claim_pairing(code, "device-1", "realme")


def test_claim_cannot_be_reused(env: tuple[PairingService, PairingRepository, FakeClock]) -> None:
    service, _, _ = env
    code = service.create_pairing_code()
    service.claim_pairing(code, "device-1", "realme")

    with pytest.raises(PairingError):
        service.claim_pairing(code, "device-2", "otro")


def test_reclaim_same_device_revokes_previous_token(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, _, _ = env
    code = service.create_pairing_code()
    first = service.claim_pairing(code, "device-1", "realme")
    code2 = service.create_pairing_code()
    second = service.claim_pairing(code2, "device-1", "realme")

    assert service.get_device_by_token(first) is None
    assert service.get_device_by_token(second) is not None


def test_revoke_kills_token(env: tuple[PairingService, PairingRepository, FakeClock]) -> None:
    service, _, _ = env
    code = service.create_pairing_code()
    token = service.claim_pairing(code, "device-1", "realme")

    assert service.revoke("device-1") is True
    assert service.get_device_by_token(token) is None
    assert service.revoke("device-1") is False


def test_revoke_all_revokes_every_device(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, _, _ = env
    token1 = service.claim_pairing(service.create_pairing_code(), "d1", "a")
    token2 = service.claim_pairing(service.create_pairing_code(), "d2", "b")

    assert service.revoke_all() == 2
    assert service.get_device_by_token(token1) is None
    assert service.get_device_by_token(token2) is None


def test_unpaired_token_is_rejected(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, _, _ = env

    assert service.get_device_by_token("invalid-token") is None


def test_invalidate_fcm_token_clears_only_that_device(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, repository, _ = env
    repository.add_device(_device(device_id="d1"))
    repository.add_device(_device(device_id="d2"))
    repository.update_fcm_token("d1", "t-1")
    repository.update_fcm_token("d2", "t-2")

    assert service.invalidate_fcm_token("t-1") is True
    assert service.invalidate_fcm_token("t-1") is False
    assert service.invalidate_fcm_token("t-desconocido") is False

    assert repository.get_device_by_fcm_token("t-1") is None
    others = repository.get_device_by_fcm_token("t-2")
    assert others is not None
    assert others.device_id == "d2"


def test_clear_fcm_token_keeps_pairing(
    env: tuple[PairingService, PairingRepository, FakeClock],
) -> None:
    service, repository, _ = env
    repository.add_device(_device())
    repository.update_fcm_token("d1", "t-1")

    assert service.clear_fcm_token("d1") is True
    cleared = repository.get_device_by_id("d1")
    assert cleared is not None
    assert cleared.fcm_token is None


def _device(device_id: str = "d1") -> Device:
    return Device(
        device_id=device_id,
        device_name="realme",
        token_hash=f"hash-{device_id}",
        created_at=datetime.now(UTC),
    )
