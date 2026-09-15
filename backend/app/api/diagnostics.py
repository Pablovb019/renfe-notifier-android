"""Estadísticas, salud y notificación de prueba para diagnóstico de la app."""

import logging
import sqlite3
import time

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app import APP_VERSION
from app.api.deps import AlertQueueDep, DeviceDep, FollowUpsDep, SenderDep, ServiceDep
from app.followups.domain import Lifecycle
from app.monitoring import monitoring
from app.notifications.base import NotificationSendError
from app.notifications.fcm import FcmNotConfiguredError, FcmRejectedError

router = APIRouter(prefix="/api/v1/diagnostics", tags=["diagnostics"])

logger = logging.getLogger(__name__)

_started_at: float = time.time()


class HealthResponse(BaseModel):
    status: str
    uptime_s: float


class FollowUpCounts(BaseModel):
    active: int
    paused: int
    expired: int
    deleted: int
    total: int


class SearchStats(BaseModel):
    """Consultas lógicas (grupos de seguimientos) y peticiones HTTP desde el reinicio."""

    logical_queries: int
    http_requests: int
    bytes_received: int


class DiagnosticsResponse(BaseModel):
    status: str
    app_version: str
    db_ok: bool
    devices: int
    followups: FollowUpCounts
    episodes: int
    alerts_pending: int
    search_stats: SearchStats


class TestNotificationResponse(BaseModel):
    message_id: str


@router.get("/health", response_model=HealthResponse)
async def diagnostics_health() -> HealthResponse:
    """Versión pública del health check usada por CD y por la app."""
    return HealthResponse(status="ok", uptime_s=round(time.time() - _started_at, 1))


@router.get("", response_model=DiagnosticsResponse)
async def diagnostics(
    device: DeviceDep,
    repository: FollowUpsDep,
    service: ServiceDep,
    queue: AlertQueueDep,
) -> DiagnosticsResponse:
    """Cifras operativas no sensibles para la pantalla de diagnóstico."""
    try:
        devices = len(service.list_devices())
        counts = {
            lifecycle: repository.count(lifecycle)
            for lifecycle in (
                Lifecycle.ACTIVE,
                Lifecycle.PAUSED,
                Lifecycle.EXPIRED,
                Lifecycle.DELETED,
            )
        }
    except sqlite3.DatabaseError as error:
        logger.error("Diagnóstico falló al leer la base de datos: %s", error)
        raise HTTPException(status_code=503, detail="Base de datos no disponible") from error
    return DiagnosticsResponse(
        status="ok",
        app_version=APP_VERSION,
        db_ok=True,
        devices=devices,
        followups=FollowUpCounts(
            active=counts[Lifecycle.ACTIVE],
            paused=counts[Lifecycle.PAUSED],
            expired=counts[Lifecycle.EXPIRED],
            deleted=counts[Lifecycle.DELETED],
            total=sum(counts.values()),
        ),
        episodes=repository.count_episodes(),
        alerts_pending=queue.count_pending(),
        search_stats=SearchStats(
            logical_queries=monitoring.snapshot().logical_queries,
            http_requests=monitoring.snapshot().http_requests,
            bytes_received=monitoring.snapshot().bytes_received,
        ),
    )


@router.post("/test-notification", response_model=TestNotificationResponse)
async def send_test_notification(
    device: DeviceDep,
    sender: SenderDep,
) -> TestNotificationResponse:
    """Envía una notificación push de prueba identificada como tal al dispositivo."""
    if sender is None:
        raise HTTPException(
            status_code=503, detail="El emisor de notificaciones no está disponible"
        )
    if device.fcm_token is None:
        raise HTTPException(
            status_code=409, detail="El dispositivo no tiene un token FCM registrado"
        )
    try:
        message_id = await sender.send_test(fcm_token=device.fcm_token)
    except FcmRejectedError as error:
        if error.token_invalid:
            raise HTTPException(
                status_code=410, detail="El token FCM del dispositivo ya no es válido"
            ) from None
        raise HTTPException(status_code=502, detail=str(error)) from None
    except FcmNotConfiguredError:
        raise HTTPException(
            status_code=503, detail="El emisor de notificaciones no está configurado"
        ) from None
    except NotificationSendError as error:
        raise HTTPException(status_code=502, detail=str(error)) from None
    return TestNotificationResponse(message_id=message_id)
