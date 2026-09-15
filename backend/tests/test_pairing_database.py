import sqlite3
from datetime import UTC, datetime, timedelta
from pathlib import Path

import pytest

from app.pairing.database import PairingRepository
from app.pairing.domain import Device, PairingCode, hash_secret

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)


def _code(*, code_hash: str = "hash-1", expires_at: datetime | None = None) -> PairingCode:
    return PairingCode(
        code_hash=code_hash,
        expires_at=expires_at or NOW + timedelta(seconds=600),
        max_attempts=3,
        created_at=NOW,
    )


def _device(*, device_id: str = "d1") -> Device:
    return Device(
        device_id=device_id,
        device_name="realme",
        token_hash=f"hash-{device_id}",
        created_at=NOW,
    )


@pytest.fixture
def repo(tmp_path: Path) -> PairingRepository:
    repository = PairingRepository(tmp_path / "data" / "test.db")
    repository.initialize()
    return repository


def _schema_versions(db_path: Path) -> list[int]:
    connection = sqlite3.connect(db_path)
    try:
        return [row[0] for row in connection.execute("SELECT version FROM schema_migrations")]
    finally:
        connection.close()


def test_initialize_applies_migrations(repo: PairingRepository, tmp_path: Path) -> None:
    assert _schema_versions(tmp_path / "data" / "test.db") == [1, 2, 3, 4, 5]


def test_create_and_get_code_roundtrip(repo: PairingRepository) -> None:
    repo.create_code(_code())

    loaded = repo.get_by_code_hash("hash-1")

    assert loaded is not None
    assert loaded.code_hash == "hash-1"
    assert loaded.max_attempts == 3
    assert loaded.attempts == 0
    assert loaded.claimed_at is None
    assert loaded.created_at == NOW


def test_save_code_persists_attempts_and_claim(repo: PairingRepository) -> None:
    repo.create_code(_code())
    repo.save_code(_code(code_hash="hash-1").fail(NOW).claim(NOW + timedelta(seconds=5)))

    loaded = repo.get_by_code_hash("hash-1")
    assert loaded is not None
    assert loaded.attempts == 1
    assert loaded.claimed_at == NOW + timedelta(seconds=5)


def test_purge_expired_codes_removes_only_expired(repo: PairingRepository) -> None:
    repo.create_code(_code(code_hash="expired", expires_at=NOW - timedelta(seconds=1)))
    repo.create_code(_code(code_hash="valid"))

    assert repo.purge_expired_codes(NOW + timedelta(seconds=10)) == 1
    assert repo.get_by_code_hash("expired") is None
    assert repo.get_by_code_hash("valid") is not None


def test_add_and_find_device(repo: PairingRepository) -> None:
    repo.add_device(_device())

    by_token = repo.get_device_by_token_hash("hash-d1")
    by_id = repo.get_device_by_id("d1")

    assert by_token is not None
    assert by_token.device_id == "d1"
    assert by_id is not None
    assert by_id.token_hash == "hash-d1"


def test_save_device_persists_revocation(repo: PairingRepository) -> None:
    repo.add_device(_device())
    device = repo.get_device_by_id("d1")
    assert device is not None
    repo.save_device(device.revoke(NOW + timedelta(minutes=1)))

    revoked = repo.get_device_by_id("d1")
    assert revoked is not None
    assert revoked.revoked_at == NOW + timedelta(minutes=1)


def test_list_devices_returns_in_creation_order(repo: PairingRepository) -> None:
    repo.add_device(_device(device_id="d1"))
    repo.add_device(_device(device_id="d2"))

    assert [d.device_id for d in repo.list_devices()] == ["d1", "d2"]


def test_fcm_token_roundtrip_and_count(repo: PairingRepository) -> None:
    repo.add_device(_device())

    assert repo.count_devices() == 1
    assert repo.update_fcm_token("d1", "APA91b-token-abc123") is True
    device = repo.get_device_by_id("d1")
    assert device is not None
    assert device.fcm_token == "APA91b-token-abc123"
    assert repo.update_fcm_token("missing", "APA91b-otro") is False


def test_reclaim_device_resets_fcm_token(repo: PairingRepository) -> None:
    repo.add_device(_device())
    repo.update_fcm_token("d1", "APA91b-viejo")
    repo.add_device(_device(device_id="d1"))

    device = repo.get_device_by_id("d1")
    assert device is not None
    assert device.fcm_token is None


def test_clear_and_lookup_by_fcm_token(repo: PairingRepository) -> None:
    repo.add_device(_device(device_id="d1"))
    repo.update_fcm_token("d1", "APA91b-token-abc123")

    assert repo.get_device_by_fcm_token("APA91b-token-abc123") is not None
    assert repo.get_device_by_fcm_token("APA91b-desconocido") is None
    assert repo.clear_fcm_token("d1") is True
    assert repo.clear_fcm_token("missing") is False

    device = repo.get_device_by_id("d1")
    assert device is not None
    assert device.fcm_token is None
    assert repo.get_device_by_fcm_token("APA91b-token-abc123") is None


def test_only_hash_is_stored(repo: PairingRepository, tmp_path: Path) -> None:
    repo.create_code(_code(code_hash=hash_secret("RF-123456")))

    connection = sqlite3.connect(tmp_path / "data" / "test.db")
    try:
        row = connection.execute("SELECT code_hash FROM pairing_codes").fetchone()
    finally:
        connection.close()

    stored = str(row[0])
    assert stored == hash_secret("RF-123456")
    assert stored != "RF-123456"
