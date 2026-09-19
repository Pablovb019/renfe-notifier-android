"""Endpoints autenticados de seguimientos (CRUD y acciones de ciclo de vida).

Contrato para la app Android: crear, listar, consultar, pausar, reanudar,
renovar, confirmar avisos y eliminar seguimientos. Confirmar nunca borra el
seguimiento y pausar/eliminar también cancelan los recordatorios pendientes.
"""

import secrets
from datetime import UTC, date, datetime, time, timedelta
from typing import Annotated

from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel, Field

from app.api.deps import AlertQueueDep, CatalogDep, DeviceDep, FollowUpsDep
from app.followups.database import Episode, FollowUpRepository
from app.followups.domain import (
    MADRID,
    AlertState,
    FollowUp,
    FollowUpMode,
    Lifecycle,
)
from app.renfe.stations import Station, StationCatalog

router = APIRouter(prefix="/api/v1/followups", tags=["followups"])

MAX_TRAVEL_HORIZON_DAYS = 62
ID_PATTERN = r"^[A-Za-z0-9_-]{1,64}$"
TIME_PATTERN = r"^([01][0-9]|2[0-3]):[0-5][0-9]$"
TRAIN_ID_PATTERN = r"^[A-Za-z0-9 _:|.:-]{1,200}$"


class FollowUpCreateRequest(BaseModel):
    origin_code: str = Field(min_length=1, max_length=8, pattern=r"^[A-Za-z0-9]{1,8}$")
    destination_code: str = Field(min_length=1, max_length=8, pattern=r"^[A-Za-z0-9]{1,8}$")
    travel_date: date
    mode: FollowUpMode
    plaza_h: bool = False
    specific_train_id: str | None = Field(
        default=None, min_length=1, max_length=200, pattern=TRAIN_ID_PATTERN
    )
    departure_time: str | None = Field(default=None, pattern=TIME_PATTERN)


class FollowUpOut(BaseModel):
    followup_id: str
    origin_code: str
    origin_name: str | None
    destination_code: str
    destination_name: str | None
    travel_date: date
    mode: str
    plaza_h: bool
    specific_train_id: str | None
    lifecycle: str
    availability: str
    alert_state: str
    episode: int
    expires_at: datetime


class EpisodeOut(BaseModel):
    episode_id: int
    episode: int
    observed_at: datetime
    train_ids: list[str]


class FollowUpDetailOut(FollowUpOut):
    episodes: list[EpisodeOut]


class FollowUpListOut(BaseModel):
    items: list[FollowUpOut]
    total: int


def _station(catalog: StationCatalog, code: str, role: str) -> Station:
    station = catalog.by_code(code)
    if station is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Estación de {role} desconocida",
        )
    return station


def _validate_date(travel_date: date) -> None:
    today = datetime.now(MADRID).date()
    if travel_date < today:
        raise HTTPException(status_code=400, detail="La fecha no puede ser anterior a hoy")
    if travel_date > today + timedelta(days=MAX_TRAVEL_HORIZON_DAYS):
        raise HTTPException(
            status_code=400,
            detail=f"La fecha supera el horizonte de {MAX_TRAVEL_HORIZON_DAYS} días",
        )


def _validate_mode(mode: FollowUpMode, specific_train_id: str | None) -> None:
    if mode is FollowUpMode.SPECIFIC and not specific_train_id:
        raise HTTPException(
            status_code=400, detail="Un seguimiento específico requiere specific_train_id"
        )
    if mode is not FollowUpMode.SPECIFIC and specific_train_id:
        raise HTTPException(
            status_code=400, detail="specific_train_id solo aplica al modo específico"
        )


def _parse_departure(departure_time: str | None) -> time | None:
    if not departure_time:
        return None
    hour, minute = departure_time.split(":")
    return time(int(hour), int(minute))


def _to_out(followup: FollowUp, catalog: StationCatalog) -> FollowUpOut:
    origin = catalog.by_code(followup.origin_code)
    destination = catalog.by_code(followup.destination_code)
    return FollowUpOut(
        followup_id=followup.followup_id,
        origin_code=followup.origin_code,
        origin_name=origin.name if origin else None,
        destination_code=followup.destination_code,
        destination_name=destination.name if destination else None,
        travel_date=followup.travel_date,
        mode=followup.mode.value,
        plaza_h=followup.plaza_h,
        specific_train_id=followup.specific_train_id,
        lifecycle=followup.lifecycle.value,
        availability=followup.availability.value,
        alert_state=followup.alert_state.value,
        episode=followup.episode,
        expires_at=followup.expires_at,
    )


def _to_detail(
    followup: FollowUp, catalog: StationCatalog, episodes: tuple[Episode, ...]
) -> FollowUpDetailOut:
    return FollowUpDetailOut(
        **_to_out(followup, catalog).model_dump(),
        episodes=[
            EpisodeOut(
                episode_id=episode.episode_id,
                episode=episode.episode,
                observed_at=episode.observed_at,
                train_ids=sorted(episode.train_ids),
            )
            for episode in episodes
        ],
    )


def _require(repository: FollowUpRepository, followup_id: str) -> FollowUp:
    followup = repository.get(followup_id)
    if followup is None:
        raise HTTPException(status_code=404, detail="Seguimiento no encontrado")
    return followup


@router.post("", response_model=FollowUpOut, status_code=status.HTTP_201_CREATED)
async def create_followup(
    payload: FollowUpCreateRequest,
    device: DeviceDep,
    catalog: CatalogDep,
    repository: FollowUpsDep,
) -> FollowUpOut:
    """Crea un seguimiento nuevo; el identificador se genera en el servidor."""
    _validate_date(payload.travel_date)
    if payload.origin_code == payload.destination_code:
        raise HTTPException(status_code=400, detail="Origen y destino no pueden coincidir")
    _station(catalog, payload.origin_code, "origen")
    _station(catalog, payload.destination_code, "destino")
    _validate_mode(payload.mode, payload.specific_train_id)

    followup = FollowUp.create(
        followup_id=secrets.token_urlsafe(12),
        origin_code=payload.origin_code,
        destination_code=payload.destination_code,
        travel_date=payload.travel_date,
        mode=payload.mode,
        now=datetime.now(UTC),
        departure_time=_parse_departure(payload.departure_time),
        specific_train_id=payload.specific_train_id,
        plaza_h=payload.plaza_h,
    )
    repository.create(followup)
    return _to_out(followup, catalog)


@router.get("", response_model=FollowUpListOut)
async def list_followups(
    device: DeviceDep,
    catalog: CatalogDep,
    repository: FollowUpsDep,
    lifecycle: Annotated[Lifecycle | None, Query()] = None,
) -> FollowUpListOut:
    """Lista los seguimientos, filtrables por estado del ciclo de vida.

    Sin filtro se muestran todos excepto los eliminados (papelera). Los
    eliminados solo son visibles pidiendo explícitamente ``lifecycle=deleted``.
    """
    if lifecycle is None:
        raw = repository.list()
        items = [
            followup
            for followup in raw
            if followup.lifecycle is not Lifecycle.DELETED
        ]
    else:
        items = list(repository.list(lifecycle))
    out = [_to_out(followup, catalog) for followup in items]
    return FollowUpListOut(items=out, total=len(out))


@router.get("/{followup_id}", response_model=FollowUpDetailOut)
async def get_followup(
    followup_id: Annotated[str, Field(pattern=ID_PATTERN)],
    device: DeviceDep,
    catalog: CatalogDep,
    repository: FollowUpsDep,
) -> FollowUpDetailOut:
    """Detalle de un seguimiento con sus episodios registrados."""
    followup = _require(repository, followup_id)
    episodes = repository.list_episodes(followup_id)
    return _to_detail(followup, catalog, episodes)


@router.post("/{followup_id}/pause", response_model=FollowUpOut)
async def pause_followup(
    followup_id: Annotated[str, Field(pattern=ID_PATTERN)],
    device: DeviceDep,
    catalog: CatalogDep,
    repository: FollowUpsDep,
    queue: AlertQueueDep,
) -> FollowUpOut:
    followup = _require(repository, followup_id)
    if followup.lifecycle is not Lifecycle.ACTIVE:
        raise HTTPException(status_code=409, detail="Solo se puede pausar un seguimiento activo")
    paused = followup.pause()
    repository.save(paused)
    queue.cancel_for_followup(followup_id, "paused")
    return _to_out(paused, catalog)


@router.post("/{followup_id}/resume", response_model=FollowUpOut)
async def resume_followup(
    followup_id: Annotated[str, Field(pattern=ID_PATTERN)],
    device: DeviceDep,
    catalog: CatalogDep,
    repository: FollowUpsDep,
) -> FollowUpOut:
    followup = _require(repository, followup_id)
    if followup.lifecycle is not Lifecycle.PAUSED:
        raise HTTPException(status_code=409, detail="Solo se puede reanudar un seguimiento pausado")
    resumed = followup.resume(datetime.now(UTC))
    repository.save(resumed)
    return _to_out(resumed, catalog)


@router.post("/{followup_id}/renew", response_model=FollowUpOut)
async def renew_followup(
    followup_id: Annotated[str, Field(pattern=ID_PATTERN)],
    device: DeviceDep,
    catalog: CatalogDep,
    repository: FollowUpsDep,
) -> FollowUpOut:
    followup = _require(repository, followup_id)
    if followup.lifecycle is Lifecycle.DELETED:
        raise HTTPException(status_code=409, detail="No se puede renovar un seguimiento eliminado")
    renewed = followup.renew(now=datetime.now(UTC))
    repository.save(renewed)
    return _to_out(renewed, catalog)


@router.post("/{followup_id}/acknowledge", response_model=FollowUpOut)
async def acknowledge_followup(
    followup_id: Annotated[str, Field(pattern=ID_PATTERN)],
    device: DeviceDep,
    catalog: CatalogDep,
    repository: FollowUpsDep,
    queue: AlertQueueDep,
) -> FollowUpOut:
    """Confirma el aviso actual sin borrar ni pausar el seguimiento."""
    followup = _require(repository, followup_id)
    if followup.alert_state is not AlertState.PENDING:
        raise HTTPException(status_code=409, detail="No hay ningún aviso pendiente que confirmar")
    queue.cancel_for_followup(followup_id, "acknowledged")
    acknowledged = followup.acknowledge()
    repository.save(acknowledged)
    return _to_out(acknowledged, catalog)


@router.delete("/{followup_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_followup(
    followup_id: Annotated[str, Field(pattern=ID_PATTERN)],
    device: DeviceDep,
    repository: FollowUpsDep,
    queue: AlertQueueDep,
) -> None:
    """Eliminación lógica; idempotente y cancela los recordatorios."""
    followup = _require(repository, followup_id)
    if followup.lifecycle is Lifecycle.DELETED:
        return
    repository.save(followup.delete())
    queue.cancel_for_followup(followup_id, "deleted")
