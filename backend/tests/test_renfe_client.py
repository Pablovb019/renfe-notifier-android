import asyncio
from datetime import date

import httpx
import pytest

from app.renfe.client import (
    RenfeAccessBlockedError,
    RenfeBudgetExceededError,
    RenfeDwrClient,
)
from app.renfe.parser import ParseStatus
from app.renfe.stations import Station

ORIGIN = Station("ORIGEN SINTÉTICO", "00001", 1, None, None)
DESTINATION = Station("DESTINO SINTÉTICO", "00002", 1, None, None)
TRAIN_LIST = (
    'r.handleCallback("0", "0", {listadoTrenes: [{numeroTren: "SYN-12", disponible: true}]});'
)


def _success_transport(calls: list[str]) -> httpx.MockTransport:
    generate_id_calls = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal generate_id_calls
        calls.append(request.url.path)
        if request.url.path.endswith("generateId.dwr"):
            generate_id_calls += 1
            token = "ignored" if generate_id_calls == 1 else "synthetictoken123"
            return httpx.Response(200, text=f'r.handleCallback("0", "0", "{token}");')
        if request.url.path.endswith("getTrainsList.dwr"):
            return httpx.Response(200, text=TRAIN_LIST)
        return httpx.Response(200, text="ok")

    return httpx.MockTransport(handler)


@pytest.mark.asyncio
async def test_client_executes_the_five_documented_posts_with_metrics() -> None:
    calls: list[str] = []
    client = RenfeDwrClient(transport=_success_transport(calls), jitter=lambda: 0.0)

    result = await client.search(
        origin=ORIGIN,
        destination=DESTINATION,
        travel_date=date(2026, 9, 12),
        plaza_h=True,
    )

    assert calls == [
        "/vol/buscarTren.do",
        "/vol/dwr/call/plaincall/__System.generateId.dwr",
        "/vol/dwr/call/plaincall/__System.generateId.dwr",
        "/vol/dwr/call/plaincall/buyEnlacesManager.actualizaObjetosSesion.dwr",
        "/vol/dwr/call/plaincall/trainEnlacesManager.getTrainsList.dwr",
    ]
    assert result.metrics.request_count == 5
    assert result.metrics.bytes_received > 0
    assert result.trains.status is ParseStatus.OK
    assert result.trains.plaza_h_requested is True


@pytest.mark.asyncio
async def test_client_honours_retry_after_for_rate_limiting() -> None:
    calls: list[str] = []
    waits: list[float] = []
    first_call = True

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal first_call
        calls.append(request.url.path)
        if first_call:
            first_call = False
            return httpx.Response(429, headers={"Retry-After": "3"})
        if request.url.path.endswith("generateId.dwr"):
            return httpx.Response(200, text='r.handleCallback("0", "0", "synthetictoken123");')
        if request.url.path.endswith("getTrainsList.dwr"):
            return httpx.Response(200, text=TRAIN_LIST)
        return httpx.Response(200)

    async def record_sleep(delay: float) -> None:
        waits.append(delay)

    client = RenfeDwrClient(
        transport=httpx.MockTransport(handler), sleep=record_sleep, jitter=lambda: 0.0
    )
    result = await client.search(
        origin=ORIGIN, destination=DESTINATION, travel_date=date(2026, 9, 12), plaza_h=False
    )

    assert waits == [3.0]
    assert result.metrics.request_count == 6
    assert len(calls) == 6


@pytest.mark.asyncio
async def test_client_stops_on_403_and_closes_the_transport() -> None:
    class ClosingTransport(httpx.AsyncBaseTransport):
        closed = False

        async def handle_async_request(self, request: httpx.Request) -> httpx.Response:
            return httpx.Response(403, request=request)

        async def aclose(self) -> None:
            self.closed = True

    transport = ClosingTransport()
    client = RenfeDwrClient(transport=transport)

    with pytest.raises(RenfeAccessBlockedError):
        await client.search(
            origin=ORIGIN, destination=DESTINATION, travel_date=date(2026, 9, 12), plaza_h=False
        )

    assert transport.closed is True


@pytest.mark.asyncio
async def test_client_enforces_the_total_search_budget() -> None:
    async def delayed_handler(request: httpx.Request) -> httpx.Response:
        await asyncio.sleep(0.02)
        return httpx.Response(200, request=request)

    client = RenfeDwrClient(transport=httpx.MockTransport(delayed_handler), total_budget_s=0.001)

    with pytest.raises(RenfeBudgetExceededError):
        await client.search(
            origin=ORIGIN, destination=DESTINATION, travel_date=date(2026, 9, 12), plaza_h=False
        )
