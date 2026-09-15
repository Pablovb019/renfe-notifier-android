"""
Endpoint de salud (/health).

Diseño:
- Responde con HTTP 200 y estado mínimo sin exponer información sensible.
- No incluye versiones de dependencias, rutas de ficheros ni estado interno.
- Utilizado por el health check del CD y para el diagnóstico de la app Android.
"""

import time

from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter(tags=["health"])

# Marca de tiempo de arranque del proceso.
_started_at: float = time.time()


class HealthResponse(BaseModel):
    """Respuesta del endpoint de salud."""

    status: str
    uptime_s: float


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Comprobación de salud del backend",
    description=(
        "Devuelve el estado operativo básico. "
        "No contiene información sensible ni versiones de componentes internos."
    ),
)
async def health_check() -> HealthResponse:
    """Devuelve 200 OK cuando el proceso está vivo y aceptando peticiones."""
    return HealthResponse(
        status="ok",
        uptime_s=round(time.time() - _started_at, 1),
    )
