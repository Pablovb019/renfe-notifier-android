from datetime import datetime, timedelta
from pathlib import Path
from typing import cast

import httpx
from fastapi import FastAPI

from app.followups.domain import MADRID
from app.notifications.fcm import (
    FcmNotificationSender,
    GceMetadataTokenProvider,
    StaticTokenProvider,
)
from tests.helpers import FakeSender, auth_headers, claim_device, make_app

FCM_TOKEN = "APA91bDz8q-FZm1x2y3w4v5u6t7s8r9q0p1o2n3m4l5k6j7i8h9g0f1e2d3c4b5a6z7y8x9" * 2


def _future_date(days: int = 5) -> str:
    return (datetime.now(MADRID).date() + timedelta(days=days)).isoformat()


def test_public_health(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        response = client.get("/api/v1/diagnostics/health")

    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_diagnostics_requires_auth(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        response = client.get("/api/v1/diagnostics")

    assert response.status_code == 401


def test_diagnostics_reports_counts(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        created = client.post(
            "/api/v1/followups",
            json={
                "origin_code": "60000",
                "destination_code": "71801",
                "travel_date": _future_date(),
                "mode": "first",
            },
            headers=auth_headers(token),
        )
        assert created.status_code == 201
        response = client.get("/api/v1/diagnostics", headers=auth_headers(token))

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["app_version"] == "0.1.0"
    assert body["db_ok"] is True
    assert body["devices"] == 1
    assert body["followups"]["active"] == 1
    assert body["followups"]["total"] == 1
    assert body["episodes"] == 0
    assert body["alerts_pending"] == 0
    stats = body["search_stats"]
    assert set(stats) == {"logical_queries", "http_requests", "bytes_received"}
    assert stats["logical_queries"] >= 0
    assert stats["http_requests"] >= 0
    assert stats["bytes_received"] >= 0


def test_test_notification_requires_auth_and_sender(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        headers = auth_headers(token)
        unauth = client.post("/api/v1/diagnostics/test-notification")
        no_sender = client.post("/api/v1/diagnostics/test-notification", headers=headers)
        cast(FastAPI, client.app).state.test_sender = FakeSender()
        no_fcm_token = client.post("/api/v1/diagnostics/test-notification", headers=headers)

    assert unauth.status_code == 401
    assert no_sender.status_code == 503
    assert no_fcm_token.status_code == 409


def test_test_notification_sends_and_reports_message_id(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        headers = auth_headers(token)
        registered = client.put("/api/v1/fcm/token", json={"fcm_token": FCM_TOKEN}, headers=headers)
        sender = FakeSender()
        cast(FastAPI, client.app).state.test_sender = sender
        response = client.post("/api/v1/diagnostics/test-notification", headers=headers)

    assert registered.status_code == 200
    assert response.status_code == 200
    assert response.json() == {"message_id": "message-test-1"}
    assert sender.sent == [FCM_TOKEN]


def test_test_notification_410_when_fcm_rejects_token(tmp_path: Path) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(404, json={"error": {"status": "NOT_FOUND"}})

    sender = FcmNotificationSender(
        project_id="test-project",
        token_provider=StaticTokenProvider("test"),
        transport=httpx.MockTransport(handler),
    )
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        headers = auth_headers(token)
        registered = client.put("/api/v1/fcm/token", json={"fcm_token": FCM_TOKEN}, headers=headers)
        cast(FastAPI, client.app).state.test_sender = sender
        response = client.post("/api/v1/diagnostics/test-notification", headers=headers)

    assert registered.status_code == 200
    assert response.status_code == 410


def test_test_notification_503_when_fcm_not_configured(tmp_path: Path) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, content=b"metadata service failure")

    sender = FcmNotificationSender(
        project_id="test-project",
        token_provider=GceMetadataTokenProvider(transport=httpx.MockTransport(handler)),
    )
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        headers = auth_headers(token)
        registered = client.put("/api/v1/fcm/token", json={"fcm_token": FCM_TOKEN}, headers=headers)
        cast(FastAPI, client.app).state.test_sender = sender
        response = client.post("/api/v1/diagnostics/test-notification", headers=headers)

    assert registered.status_code == 200
    assert response.status_code == 503
