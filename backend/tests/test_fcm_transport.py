"""Pruebas del transporte FCM con emisor simulado (MockTransport, sin red real)."""

import json
from collections.abc import Callable

import httpx
import pytest

from app.notifications.fcm import (
    CHANNEL_ALERT,
    MESSAGE_TYPE_ALERT,
    MESSAGE_TYPE_TEST,
    AlertMessage,
    FcmMessage,
    FcmNotificationSender,
    FcmPayloadError,
    FcmRejectedError,
    FcmResult,
    FcmTokenProvider,
    StaticTokenProvider,
    build_test_alert,
)


class FakeProvider(FcmTokenProvider):
    async def access_token(self) -> str:
        return "fake-access-token"


def _sender(
    handler: Callable[[httpx.Request], httpx.Response],
    *,
    project_id: str = "test-project",
) -> FcmNotificationSender:
    return FcmNotificationSender(
        project_id=project_id,
        token_provider=FakeProvider(),
        timeout_s=5.0,
        default_ttl_s=120,
        package="com.example.app",
        transport=httpx.MockTransport(handler),
    )


def _alert() -> FcmMessage:
    return FcmMessage(
        token="dev-token",
        data=build_test_alert(token="dev-token").as_data(),
        priority="high",
        ttl_s=300,
        collapse_key="followup:abc123",
        package="com.example.app",
    )


@pytest.mark.asyncio
async def test_send_200_carries_expected_body() -> None:
    captured: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["authorization"] = request.headers.get("authorization", "")
        captured["body"] = json.loads(request.content.decode())
        return httpx.Response(200, json={"name": "projects/test-project/messages/msg-1"})

    sender = _sender(handler)
    result = await sender.send(_alert())

    assert result.accepted is True
    assert result.message_id == "projects/test-project/messages/msg-1"
    assert captured["url"] == ("https://fcm.googleapis.com/v1/projects/test-project/messages:send")
    assert captured["authorization"] == "Bearer fake-access-token"
    body = captured["body"]
    assert isinstance(body, dict)
    message = body["message"]
    assert isinstance(message, dict)
    assert message["token"] == "dev-token"
    android = message["android"]
    assert isinstance(android, dict)
    assert android["priority"] == "high"
    assert android["ttl"] == "300s"
    assert android["collapse_key"] == "followup:abc123"
    assert android["restricted_package_name"] == "com.example.app"
    data = message["data"]
    assert isinstance(data, dict)
    assert data["type"] == MESSAGE_TYPE_TEST


@pytest.mark.asyncio
async def test_send_empty_body_still_accepted() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, content=b"")

    sender = _sender(handler)
    result = await sender.send(_alert())
    assert result.accepted is True
    assert result.message_id is None


@pytest.mark.asyncio
async def test_send_404_reports_invalid_token() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            404,
            json={
                "error": {
                    "status": "NOT_FOUND",
                    "message": "Requested entity was not found.",
                    "code": 404,
                }
            },
        )

    sender = _sender(handler)
    result = await sender.send(_alert())
    assert result.accepted is False
    assert result.token_invalid is True
    assert result.error_code == "NOT_FOUND"


@pytest.mark.asyncio
async def test_send_unregistered_reports_invalid_token() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            400,
            json={"error": {"status": "UNREGISTERED", "message": "app instance removed"}},
        )

    sender = _sender(handler)
    result = await sender.send(_alert())
    assert result.accepted is False
    assert result.token_invalid is True
    assert result.error_code == "UNREGISTERED"


@pytest.mark.asyncio
async def test_send_generic_400_raises_non_retryable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(400, json={"error": {"status": "INVALID_ARGUMENT"}})

    sender = _sender(handler)
    with pytest.raises(FcmRejectedError) as exc_info:
        await sender.send(_alert())
    assert exc_info.value.retryable is False
    assert exc_info.value.token_invalid is False


@pytest.mark.asyncio
async def test_send_authentication_error_is_non_retryable() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(401, json={"error": {"status": "UNAUTHENTICATED"}})

    sender = _sender(handler)
    with pytest.raises(FcmRejectedError) as exc_info:
        await sender.send(_alert())
    assert exc_info.value.retryable is False
    assert exc_info.value.token_invalid is False


@pytest.mark.parametrize("status", [429, 500, 503])
@pytest.mark.asyncio
async def test_send_rate_limit_and_server_errors_are_retryable(status: int) -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(status, json={"error": {"status": "UNAVAILABLE"}})

    sender = _sender(handler)
    with pytest.raises(FcmRejectedError) as exc_info:
        await sender.send(_alert())
    assert exc_info.value.retryable is True
    assert exc_info.value.code == "UNAVAILABLE"


@pytest.mark.asyncio
async def test_send_uses_sender_defaults_for_package() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"name": "projects/test-project/messages/msg-2"})

    sender = FcmNotificationSender(
        project_id="test-project",
        token_provider=FakeProvider(),
        package="com.example.app",
        transport=httpx.MockTransport(handler),
    )
    message = FcmMessage(
        token="dev-token",
        data=build_test_alert(token="dev-token").as_data(),
        ttl_s=60,
    )
    result = await sender.send(message)
    assert result.accepted is True
    assert result.message_id == "projects/test-project/messages/msg-2"


def test_payload_validation_limits_value_and_total_size() -> None:
    with pytest.raises(FcmPayloadError):
        FcmMessage(token="dev-token", data={"big": "x" * 1100}, ttl_s=300).validate()
    with pytest.raises(FcmPayloadError):
        FcmMessage(
            token="dev-token",
            data={"k01": "a" * 1024, "k02": "b" * 1024, "k03": "c" * 1024, "k04": "d" * 1024},
            ttl_s=300,
        ).validate()


def test_payload_validation_rejects_empty_token_bad_keys_bad_ttl() -> None:
    with pytest.raises(FcmPayloadError):
        FcmMessage(token="", data={"type": "alert"}, ttl_s=300).validate()
    with pytest.raises(FcmPayloadError):
        FcmMessage(token="t", data={"bad key": "x"}, ttl_s=300).validate()
    with pytest.raises(FcmPayloadError):
        FcmMessage(token="t", data={}, ttl_s=2_419_201).validate()


def test_test_alert_contract_and_validation() -> None:
    alert = build_test_alert(token="dev-token", ttl_s=90, followup_id="fc-1")
    data = alert.as_data()
    assert data["type"] == MESSAGE_TYPE_TEST
    assert data["channel_id"] == CHANNEL_ALERT
    assert data["priority"] == "high"
    assert data["followup_id"] == "fc-1"
    assert data["event_id"] == "test"
    FcmMessage(token="dev-token", data=data, ttl_s=90).validate()


def test_alert_data_contract() -> None:
    alert = AlertMessage(
        type=MESSAGE_TYPE_ALERT,
        event_id="evt-1",
        followup_id="fc-1",
        episode_id=7,
        title="Aviso",
        body="Plazas disponibles",
        origin_code="60000",
        origin="MADRID (TODAS)",
        destination_code="71801",
        destination="BARCELONA-SANTS",
        travel_date="2026-09-20",
        observed_at="2026-09-13T10:00:00+02:00",
        expires_at="2026-09-13T10:00:00+02:00",
        channel_id=CHANNEL_ALERT,
        departure="08:00:00",
        arrival="09:30:00",
        price="42.50",
    )
    data = alert.as_data()
    assert data["episode_id"] == "7"
    assert data["departure"] == "08:00:00"
    assert data["price"] == "42.50"
    assert data["type"] == MESSAGE_TYPE_ALERT
    FcmMessage(token="dev-token", data=data, ttl_s=120).validate()


@pytest.mark.asyncio
async def test_send_test_returns_message_id_and_raises_on_invalid_token() -> None:
    def ok_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"name": "projects/test-project/messages/msg-test"})

    sender = _sender(ok_handler)
    assert (
        await sender.send_test(fcm_token="dev-token") == "projects/test-project/messages/msg-test"
    )

    def invalid_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(404, json={"error": {"status": "NOT_FOUND"}})

    bad_sender = _sender(invalid_handler)
    with pytest.raises(FcmRejectedError) as exc_info:
        await bad_sender.send_test(fcm_token="dev-token")
    assert exc_info.value.token_invalid is True


def test_build_test_alert_rejects_empty_token() -> None:
    with pytest.raises(FcmPayloadError):
        build_test_alert(token="")


@pytest.mark.asyncio
async def test_send_test_uses_unique_event_id_per_send() -> None:
    event_ids: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        body = json.loads(request.content.decode())
        event_ids.append(body["message"]["data"]["event_id"])
        return httpx.Response(200, json={"name": "projects/test-project/messages/msg"})

    sender = _sender(handler)
    await sender.send_test(fcm_token="dev-token")
    await sender.send_test(fcm_token="dev-token")

    assert len(event_ids) == 2
    assert event_ids[0] != event_ids[1]
    assert all(event_id.startswith("test-") for event_id in event_ids)


@pytest.mark.asyncio
async def test_static_token_provider_returns_token() -> None:
    provider = StaticTokenProvider("static-token")
    assert await provider.access_token() == "static-token"


def test_constructor_validates_project_id_and_ttl() -> None:
    with pytest.raises(ValueError):
        FcmNotificationSender(project_id="  ", token_provider=FakeProvider())
    with pytest.raises(ValueError):
        FcmNotificationSender(
            project_id="p", token_provider=FakeProvider(), default_ttl_s=2_419_201
        )


@pytest.mark.asyncio
async def test_send_alert_builds_collapse_key_per_followup() -> None:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"name": "m"})

    sender = _sender(handler)
    result = await sender.send_alert(
        token="dev-token",
        alert=build_test_alert(token="dev-token", followup_id="fc-9"),
    )
    assert isinstance(result, FcmResult)
    assert result.accepted is True
