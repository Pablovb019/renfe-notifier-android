"""Pruebas de los proveedores de token FCM (identidad de VM y ADC, sin red real)."""

import sys

import httpx
import pytest

from app.notifications.fcm import (
    GCE_METADATA_URL,
    METADATA_FLAVOR_HEADER,
    AutoTokenProvider,
    FcmNotConfiguredError,
    FcmTokenProvider,
    GceMetadataTokenProvider,
)


def _metadata_ok() -> httpx.MockTransport:
    def handler(request: httpx.Request) -> httpx.Response:
        assert str(request.url) == GCE_METADATA_URL
        assert request.headers.get(METADATA_FLAVOR_HEADER) == "Google"
        return httpx.Response(200, json={"access_token": "tok-metadata"})

    return httpx.MockTransport(handler)


def _metadata_http_error() -> httpx.MockTransport:
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, content=b"metadata service failure")

    return httpx.MockTransport(handler)


def _metadata_transport_error() -> httpx.MockTransport:
    def handler(request: httpx.Request) -> httpx.Response:
        raise httpx.ConnectError("servidor de metadatos no alcanzable")

    return httpx.MockTransport(handler)


@pytest.mark.asyncio
async def test_gce_metadata_provider_returns_token() -> None:
    provider = GceMetadataTokenProvider(transport=_metadata_ok())
    assert await provider.access_token() == "tok-metadata"


@pytest.mark.asyncio
async def test_gce_metadata_provider_raises_on_http_error() -> None:
    provider = GceMetadataTokenProvider(transport=_metadata_http_error())
    with pytest.raises(FcmNotConfiguredError):
        await provider.access_token()


@pytest.mark.asyncio
async def test_gce_metadata_provider_raises_on_transport_error() -> None:
    provider = GceMetadataTokenProvider(transport=_metadata_transport_error())
    with pytest.raises(FcmNotConfiguredError):
        await provider.access_token()


class _FakeMetadata(FcmTokenProvider):
    async def access_token(self) -> str:
        return "tok-metadata"


@pytest.mark.asyncio
async def test_autoauth_falls_back_to_metadata_without_google_auth(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """Sin google-auth instalado, AutoTokenProvider usa la identidad de la VM."""
    monkeypatch.setitem(sys.modules, "google.auth", None)
    provider = AutoTokenProvider(metadata_provider=_FakeMetadata())
    assert await provider.access_token() == "tok-metadata"


@pytest.mark.asyncio
async def test_autoauth_raises_when_no_credentials_at_all(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setitem(sys.modules, "google.auth", None)
    provider = AutoTokenProvider(
        metadata_provider=GceMetadataTokenProvider(transport=_metadata_transport_error())
    )
    with pytest.raises(FcmNotConfiguredError):
        await provider.access_token()
