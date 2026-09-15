"""Dependencias compartidas de la API (emparejamiento y autenticación)."""

from typing import Annotated

from fastapi import Depends, Header, HTTPException, Request, status

from app.followups.database import FollowUpRepository
from app.notifications.base import NotificationSender
from app.pairing.domain import Device
from app.pairing.service import PairingService
from app.reminders.queue import AlertQueue
from app.renfe.search import TrainSearchEngine
from app.renfe.stations import StationCatalog


def get_pairing_service(request: Request) -> PairingService:
    """Devuelve el servicio de emparejamiento construido en el lifespan."""
    service = request.app.state.pairing_service
    if not isinstance(service, PairingService):
        raise RuntimeError("PairingService no inicializado en el lifespan")
    return service


ServiceDep = Annotated[PairingService, Depends(get_pairing_service)]


def get_followups_repository(request: Request) -> FollowUpRepository:
    """Devuelve el repositorio de seguimientos construido en el lifespan."""
    repository = request.app.state.followups_repository
    if not isinstance(repository, FollowUpRepository):
        raise RuntimeError("FollowUpRepository no inicializado en el lifespan")
    return repository


FollowUpsDep = Annotated[FollowUpRepository, Depends(get_followups_repository)]


def get_alert_queue(request: Request) -> AlertQueue:
    """Devuelve la cola de recordatorios construida en el lifespan."""
    queue = request.app.state.alert_queue
    if not isinstance(queue, AlertQueue):
        raise RuntimeError("AlertQueue no inicializada en el lifespan")
    return queue


AlertQueueDep = Annotated[AlertQueue, Depends(get_alert_queue)]


def get_station_catalog(request: Request) -> StationCatalog:
    """Devuelve el catálogo de estaciones cargado en el lifespan."""
    catalog = request.app.state.station_catalog
    if not isinstance(catalog, StationCatalog):
        raise RuntimeError("StationCatalog no inicializado en el lifespan")
    return catalog


CatalogDep = Annotated[StationCatalog, Depends(get_station_catalog)]


def get_search_engine(request: Request) -> TrainSearchEngine:
    """Devuelve el motor de búsqueda de trenes construido en el lifespan."""
    engine = request.app.state.search_engine
    if not isinstance(engine, TrainSearchEngine):
        raise RuntimeError("TrainSearchEngine no inicializado en el lifespan")
    return engine


SearchDep = Annotated[TrainSearchEngine, Depends(get_search_engine)]


def get_test_sender(request: Request) -> NotificationSender | None:
    """Devuelve el emisor FCM inyectado o ``None`` si aún no está implementado."""
    sender = request.app.state.test_sender
    if sender is not None and not isinstance(sender, NotificationSender):
        raise RuntimeError("test_sender no implementa NotificationSender")
    return sender


SenderDep = Annotated[NotificationSender | None, Depends(get_test_sender)]


def require_device(
    service: ServiceDep,
    authorization: Annotated[str | None, Header()] = None,
) -> Device:
    """Autentica ``Authorization: Bearer <token>`` contra la base de datos.

    Devuelve el dispositivo solo si el token es válido y no está revocado; en
    cualquier otro caso responde 401 sin revelar el motivo.
    """
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="No autorizado",
            headers={"WWW-Authenticate": "Bearer"},
        )
    token = authorization[len("Bearer ") :]
    device = service.get_device_by_token(token)
    if device is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="No autorizado",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return device


DeviceDep = Annotated[Device, Depends(require_device)]
