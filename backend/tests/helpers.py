"""Utilidades compartidas por los tests de la API (dependencias simuladas)."""

from pathlib import Path
from typing import cast

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.pairing.service import PairingService


class FakeSender:
    """Emisor FCM de prueba; registra los envíos y devuelve un id fijo."""

    def __init__(self) -> None:
        self.sent: list[str] = []

    async def send_test(self, *, fcm_token: str) -> str:
        self.sent.append(fcm_token)
        return "message-test-1"


def make_app(db_path: Path, *, rate_limit_max_requests: int = 1000) -> TestClient:
    settings = Settings(
        environment="test",
        database_path=db_path,
        rate_limit_max_requests=rate_limit_max_requests,
    )
    return TestClient(create_app(settings))


def pairing_service(client: TestClient) -> PairingService:
    app = cast(FastAPI, client.app)
    service = app.state.pairing_service
    if not isinstance(service, PairingService):
        raise RuntimeError("PairingService no inicializado")
    return service


def claim_device(
    client: TestClient, device_id: str = "device-1", device_name: str = "realme"
) -> dict[str, str]:
    """Empareja un dispositivo de prueba y devuelve el cuerpo de la reclamación."""
    service = pairing_service(client)
    code = service.create_pairing_code()
    response = client.post(
        "/api/v1/pairing/claim",
        json={"code": code, "device_id": device_id, "device_name": device_name},
    )
    assert response.status_code == 200, response.text
    return cast(dict[str, str], response.json())


def auth_headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}
