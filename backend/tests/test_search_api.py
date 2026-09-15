from datetime import datetime, timedelta
from pathlib import Path
from typing import cast

import httpx
from fastapi import FastAPI

from app.followups.domain import MADRID
from app.renfe.client import RenfeDwrClient
from app.renfe.search import TrainSearchEngine
from app.renfe.stations import StationCatalog
from tests.helpers import auth_headers, claim_device, make_app

ORIGIN_CODE = "60000"
DESTINATION_CODE = "71801"

TRAIN_LIST = (
    'r.handleCallback("0", "0", {listadoTrenes: ['
    '{numeroTren: "AVE-001", horaSalida: "08:00", horaLlegada: "09:30", precio: "42,50", disponible: true},'
    '{numeroTren: "AVE-002", horaSalida: "10:00", horaLlegada: "11:30", precio: "60,00", disponible: false}'
    "]})"
)


def _future_date(days: int = 5) -> str:
    return (datetime.now(MADRID).date() + timedelta(days=days)).isoformat()


def _install_transport(app: FastAPI) -> None:
    generate_id_calls = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal generate_id_calls
        if request.url.path.endswith("generateId.dwr"):
            generate_id_calls += 1
            token = "ignored" if generate_id_calls == 1 else "synthetictoken123"
            return httpx.Response(200, text=f'r.handleCallback("0", "0", "{token}");')
        if request.url.path.endswith("getTrainsList.dwr"):
            return httpx.Response(200, text=TRAIN_LIST)
        return httpx.Response(200, text="ok")

    client = RenfeDwrClient(transport=httpx.MockTransport(handler), jitter=lambda: 0.0)
    app.state.search_engine = TrainSearchEngine(client, StationCatalog())


def test_stations_search_requires_auth(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        response = client.get("/api/v1/search/stations", params={"q": "atocha"})

    assert response.status_code == 401


def test_stations_search_returns_match(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        response = client.get(
            "/api/v1/search/stations",
            params={"q": "atocha"},
            headers=auth_headers(token),
        )

    assert response.status_code == 200
    names = [item["name"].upper() for item in response.json()]
    assert any("ATOCHA" in name for name in names)
    assert all(item["is_group"] is False for item in response.json())


def test_stations_search_respects_limit_and_requires_query(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        empty = client.get("/api/v1/search/stations", params={"q": ""}, headers=auth_headers(token))
        limited = client.get(
            "/api/v1/search/stations",
            params={"q": "madrid", "limit": 3},
            headers=auth_headers(token),
        )

    assert empty.status_code == 200
    assert empty.json() == []
    assert limited.status_code == 200
    assert len(limited.json()) <= 3


def test_search_trains_returns_typed_trains(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        _install_transport(cast(FastAPI, client.app))
        response = client.post(
            "/api/v1/search/trains",
            json={
                "origin_code": ORIGIN_CODE,
                "destination_code": DESTINATION_CODE,
                "travel_date": _future_date(),
                "plaza_h": False,
            },
            headers=auth_headers(token),
        )

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["plaza_h_requested"] is False
    assert len(body["trains"]) == 2
    first = body["trains"][0]
    assert first["identifier"] == "AVE-001|08:00:00|09:30:00"
    assert first["identity"].startswith("real:")
    assert first["departure"] == "08:00:00"
    assert first["arrival"] == "09:30:00"
    assert first["price"] == "42.50"
    assert first["availability"] == "available"


def test_search_trains_rejects_unknown_station(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        _install_transport(cast(FastAPI, client.app))
        response = client.post(
            "/api/v1/search/trains",
            json={
                "origin_code": "99999",
                "destination_code": DESTINATION_CODE,
                "travel_date": _future_date(),
                "plaza_h": False,
            },
            headers=auth_headers(token),
        )

    assert response.status_code == 400
    assert "origen" in response.json()["detail"]


def test_search_trains_rejects_past_horizon_and_same_station_dates(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        _install_transport(cast(FastAPI, client.app))
        payload = {
            "origin_code": ORIGIN_CODE,
            "destination_code": DESTINATION_CODE,
            "travel_date": "",
        }
        past = client.post(
            "/api/v1/search/trains",
            json={
                **payload,
                "travel_date": (datetime.now(MADRID).date() - timedelta(days=1)).isoformat(),
            },
            headers=auth_headers(token),
        )
        far = client.post(
            "/api/v1/search/trains",
            json={**payload, "travel_date": _future_date(days=63)},
            headers=auth_headers(token),
        )
        same = client.post(
            "/api/v1/search/trains",
            json={**payload, "travel_date": _future_date(), "destination_code": ORIGIN_CODE},
            headers=auth_headers(token),
        )

    assert past.status_code == 400
    assert far.status_code == 400
    assert same.status_code == 400


def test_search_trains_requires_auth(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        response = client.post(
            "/api/v1/search/trains",
            json={
                "origin_code": ORIGIN_CODE,
                "destination_code": DESTINATION_CODE,
                "travel_date": _future_date(),
            },
        )

    assert response.status_code == 401
