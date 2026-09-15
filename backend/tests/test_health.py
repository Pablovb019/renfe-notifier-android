from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app


def test_health_endpoint_reports_only_basic_status() -> None:
    app = create_app(Settings(environment="test"))

    with TestClient(app) as client:
        response = client.get("/health")

    assert response.status_code == 200
    assert response.json()["status"] == "ok"
    assert set(response.json()) == {"status", "uptime_s"}
