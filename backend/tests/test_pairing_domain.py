import re
from datetime import UTC, datetime, timedelta

import pytest

from app.pairing.domain import (
    Device,
    PairingCode,
    as_utc,
    generate_pairing_code,
    hash_secret,
)

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)


def test_generate_pairing_code_has_rf_format() -> None:
    for _ in range(20):
        assert re.fullmatch(r"RF-\d{6}", generate_pairing_code())


def test_hash_secret_is_deterministic_and_not_reversible() -> None:
    digest = hash_secret("RF-123456")
    assert digest == hash_secret("RF-123456")
    assert len(digest) == 64
    assert "RF-123456" not in digest


def test_as_utc_requires_aware_datetimes() -> None:
    with pytest.raises(ValueError):
        as_utc(datetime(2026, 3, 1, 12))


def test_as_utc_normalizes_timezone() -> None:
    local = datetime(2026, 3, 1, 12, tzinfo=UTC)
    assert as_utc(local) == NOW


def test_pairing_code_expiry_and_attempts() -> None:
    code = PairingCode(code_hash="h", expires_at=NOW + timedelta(seconds=600), max_attempts=3)

    assert not code.is_expired(NOW)
    assert code.is_expired(NOW + timedelta(seconds=600))
    assert code.can_try()

    failed = code.fail(NOW).fail(NOW).fail(NOW)
    assert not failed.can_try()
    assert failed.attempts == 3


def test_pairing_code_claim_sets_claimed_at() -> None:
    code = PairingCode(code_hash="h", expires_at=NOW + timedelta(seconds=600), max_attempts=3)

    claimed = code.claim(NOW)

    assert claimed.is_claimed()
    assert claimed.claimed_at == NOW


def test_device_revoke_sets_revoked_at() -> None:
    device = Device(
        device_id="d1",
        device_name="realme",
        token_hash="h",
        created_at=NOW,
    )

    revoked = device.revoke(NOW + timedelta(minutes=1))

    assert revoked.is_active is False
    assert revoked.revoked_at == NOW + timedelta(minutes=1)
