from pathlib import Path
from typing import cast

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.middleware.logging import redact
from app.pairing.service import PairingService


def make_app(db_path: Path, *, rate_limit_max_requests: int = 1000) -> TestClient:
    settings = Settings(
        environment="test",
        database_path=db_path,
        rate_limit_max_requests=rate_limit_max_requests,
    )
    return TestClient(create_app(settings))


def _service(client: TestClient) -> PairingService:
    app = cast(FastAPI, client.app)
    service = app.state.pairing_service
    if not isinstance(service, PairingService):
        raise RuntimeError("PairingService no inicializado")
    return service


def test_claim_with_valid_code_returns_token(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        service = _service(client)
        code = service.create_pairing_code()

        response = client.post(
            "/api/v1/pairing/claim",
            json={"code": code, "device_id": "device-1", "device_name": "realme"},
        )

    assert response.status_code == 200
    body = response.json()
    assert body["device_id"] == "device-1"
    assert len(body["device_token"]) >= 32
    assert "Bearer" not in response.text


def test_claim_with_invalid_code_returns_401(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        response = client.post(
            "/api/v1/pairing/claim",
            json={"code": "RF-000000", "device_id": "device-1", "device_name": "realme"},
        )

    assert response.status_code == 401
    assert "no válido" in response.json()["detail"]


def test_claim_rejects_malformed_payload(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        response = client.post(
            "/api/v1/pairing/claim",
            json={"code": "", "device_id": "", "device_name": ""},
        )

    assert response.status_code == 422


def test_devices_requires_valid_bearer(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        no_header = client.get("/api/v1/pairing/devices")
        bad_token = client.get(
            "/api/v1/pairing/devices", headers={"Authorization": "Bearer invalid-token"}
        )

    assert no_header.status_code == 401
    assert bad_token.status_code == 401


def test_devices_succeeds_with_valid_device_token(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        service = _service(client)
        code = service.create_pairing_code()
        claim = client.post(
            "/api/v1/pairing/claim",
            json={"code": code, "device_id": "device-1", "device_name": "realme"},
        )
        token = claim.json()["device_token"]

        response = client.get(
            "/api/v1/pairing/devices", headers={"Authorization": f"Bearer {token}"}
        )

    assert response.status_code == 200
    devices = response.json()
    assert len(devices) == 1
    assert devices[0]["device_id"] == "device-1"
    assert "token" not in str(devices).lower()
    assert "hash" not in str(devices).lower()


def test_revoked_device_is_rejected(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        service = _service(client)
        code = service.create_pairing_code()
        claim = client.post(
            "/api/v1/pairing/claim",
            json={"code": code, "device_id": "device-1", "device_name": "realme"},
        )
        token = claim.json()["device_token"]
        service.revoke("device-1")

        response = client.get(
            "/api/v1/pairing/devices", headers={"Authorization": f"Bearer {token}"}
        )

    assert response.status_code == 401


def test_claim_is_rate_limited_per_client(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db", rate_limit_max_requests=3) as client:
        service = _service(client)
        codes = [service.create_pairing_code() for _ in range(6)]
        statuses = [
            client.post(
                "/api/v1/pairing/claim",
                json={"code": code, "device_id": f"d{i}", "device_name": "realme"},
            ).status_code
            for i, code in enumerate(codes)
        ]

    assert statuses[:3] == [200, 200, 200]
    assert statuses[3:] == [429, 429, 429]


def test_log_redaction_hides_secrets() -> None:
    assert redact("Authorization: Bearer abcDEF123_xyz") == "Authorization: Bearer ********"
    assert redact("codigo RF-123456 revisado") == "codigo RF-****** revisado"
    assert redact("device_token=xYzAbCdEfGh1234567890") == "device_token=********"
    assert redact("cookie: session_id=abc123") == "cookie: ********"


def test_token_never_echoes_pairing_code(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        service = _service(client)
        code = service.create_pairing_code()
        text = client.post(
            "/api/v1/pairing/claim",
            json={"code": code, "device_id": "d", "device_name": "n"},
        ).text

    assert code not in text
