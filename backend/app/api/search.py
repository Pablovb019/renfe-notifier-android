"""Endpoints autenticados de estaciones y búsqueda de trenes.

Las búsquedas solo aceptan códigos de estación validados contra el catálogo
local (anti-SSRF): ningún campo admite URLs arbitrarias del cliente.
"""

import logging
from collections.abc import Iterable
from datetime import date, datetime, timedelta
from decimal import Decimal
from typing import Annotated

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field

from app.api.deps import CatalogDep, DeviceDep, SearchDep
from app.followups.domain import MADRID, TrainSnapshot
from app.renfe.client import RenfeClientError
from app.renfe.parser import Train
from app.renfe.search import StationNotFoundError
from app.renfe.stations import normalize_station_key, station_match_score

router = APIRouter(prefix="/api/v1/search", tags=["search"])

logger = logging.getLogger(__name__)

MAX_TRAVEL_HORIZON_DAYS = 62
STATION_CODE_PATTERN = r"^[A-Za-z0-9]{1,8}$"


class StationOut(BaseModel):
    name: str
    code: str
    is_group: bool


class TrainSearchRequest(BaseModel):
    origin_code: str = Field(min_length=5, max_length=8, pattern=STATION_CODE_PATTERN)
    destination_code: str = Field(min_length=5, max_length=8, pattern=STATION_CODE_PATTERN)
    travel_date: date
    plaza_h: bool = False


class TrainOut(BaseModel):
    identifier: str
    identity: str
    departure: str | None
    arrival: str | None
    price: str | None
    availability: str


class TrainSearchResponse(BaseModel):
    status: str
    plaza_h_requested: bool
    trains: list[TrainOut]


@router.get("/stations", response_model=list[StationOut])
async def search_stations(
    device: DeviceDep,
    catalog: CatalogDep,
    q: Annotated[str, Query(max_length=80)] = "",
    limit: Annotated[int, Query(ge=1, le=50)] = 20,
) -> list[StationOut]:
    """Sugerencias de estaciones para el buscador de la app."""
    key = normalize_station_key(q.strip())
    if not key:
        return []
    stations = [
        station for station in catalog.stations if key in normalize_station_key(station.name)
    ]
    deduped = {station.code: station for station in stations}.values()
    ranked = sorted(
        deduped,
        key=lambda station: (-station_match_score(station, key), station.name.upper()),
    )
    return [
        StationOut(name=station.name, code=station.code, is_group=station.is_group)
        for station in ranked[:limit]
    ]


@router.post("/trains", response_model=TrainSearchResponse)
async def search_trains(
    payload: TrainSearchRequest,
    device: DeviceDep,
    engine: SearchDep,
) -> TrainSearchResponse:
    """Busca trenes de una ruta; no reserva nada y nunca consulta otra URL."""
    today = datetime.now(MADRID).date()
    if payload.travel_date < today:
        raise HTTPException(status_code=400, detail="La fecha no puede ser anterior a hoy")
    if payload.travel_date > today + timedelta(days=MAX_TRAVEL_HORIZON_DAYS):
        raise HTTPException(
            status_code=400,
            detail=f"La fecha supera el horizonte de {MAX_TRAVEL_HORIZON_DAYS} días",
        )
    if payload.origin_code == payload.destination_code:
        raise HTTPException(status_code=400, detail="Origen y destino no pueden coincidir")

    try:
        result = await engine.search(
            origin_code=payload.origin_code,
            destination_code=payload.destination_code,
            travel_date=payload.travel_date,
            plaza_h=payload.plaza_h,
        )
    except StationNotFoundError as error:
        raise HTTPException(status_code=400, detail=str(error)) from None
    except RenfeClientError as error:
        logger.warning(
            "Búsqueda de %s falló contra Renfe: %s (%s)",
            payload.origin_code,
            error,
            type(error).__name__,
        )
        raise HTTPException(
            status_code=503, detail="Renfe no responde o rechazó la consulta"
        ) from None

    trains = _unique_trains_by_identifier(
        _train_to_out(
            train,
            origin_code=payload.origin_code,
            destination_code=payload.destination_code,
            travel_date=payload.travel_date,
        )
        for train in result.trains.trains
    )
    return TrainSearchResponse(
        status=result.trains.status.value,
        plaza_h_requested=result.trains.plaza_h_requested,
        trains=trains,
    )


def _unique_trains_by_identifier(trains: Iterable[TrainOut]) -> list[TrainOut]:
    """Devuelve trenes únicos por ``identifier`` preservando el orden.

    Renfe puede repetir el mismo servicio (mismo identificador y horarios) en
    una sola respuesta; el dedup evita claves duplicadas del lado del cliente.
    """
    seen: set[str] = set()
    unique: list[TrainOut] = []
    for train in trains:
        if train.identifier in seen:
            continue
        seen.add(train.identifier)
        unique.append(train)
    return unique


def _train_to_out(
    train: Train,
    *,
    origin_code: str,
    destination_code: str,
    travel_date: date,
) -> TrainOut:
    snapshot = TrainSnapshot(
        departure=train.departure,
        arrival=train.arrival,
        availability=train.availability,
        real_id=train.identifier,
    )
    return TrainOut(
        identifier=train.identifier,
        identity=snapshot.identity(
            origin_code=origin_code,
            destination_code=destination_code,
            travel_date=travel_date,
        ),
        departure=train.departure.isoformat() if train.departure else None,
        arrival=train.arrival.isoformat() if train.arrival else None,
        price=_format_price(train.price),
        availability=train.availability.value,
    )


def _format_price(price: Decimal | None) -> str | None:
    if price is None:
        return None
    return str(price.quantize(Decimal("0.01")))
