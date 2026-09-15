"""Registro y renovación del token FCM del dispositivo autenticado."""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.api.deps import DeviceDep, ServiceDep

router = APIRouter(prefix="/api/v1/fcm", tags=["fcm"])

FCM_TOKEN_PATTERN = r"^[A-Za-z0-9_:-]{20,4096}$"


class FcmTokenRequest(BaseModel):
    fcm_token: str = Field(min_length=20, max_length=4096, pattern=FCM_TOKEN_PATTERN)


class FcmTokenResponse(BaseModel):
    registered: bool


@router.put("/token", response_model=FcmTokenResponse)
async def register_fcm_token(
    payload: FcmTokenRequest,
    device: DeviceDep,
    service: ServiceDep,
) -> FcmTokenResponse:
    """Registra o renueva el token FCM; el dispositivo debe estar activo."""
    registered = service.register_fcm_token(device.device_id, payload.fcm_token)
    if not registered:
        raise HTTPException(status_code=409, detail="Dispositivo no activo")
    return FcmTokenResponse(registered=True)
