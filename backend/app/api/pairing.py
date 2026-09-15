"""Endpoint de emparejamiento y consulta autenticada de dispositivos."""

from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel, Field

from app.api.deps import DeviceDep, ServiceDep
from app.pairing.service import PairingError

router = APIRouter(prefix="/api/v1/pairing", tags=["pairing"])


class ClaimRequest(BaseModel):
    """Petición de reclamación de un código de emparejamiento."""

    code: str = Field(min_length=1, max_length=32, description="Código temporal RF-XXXXXX")
    device_id: str = Field(
        min_length=1, max_length=128, description="Identificador del dispositivo"
    )
    device_name: str = Field(
        min_length=1, max_length=200, description="Nombre legible del dispositivo"
    )


class ClaimResponse(BaseModel):
    """Token de dispositivo entregado tras un emparejamiento correcto."""

    device_token: str
    device_id: str


class DeviceInfo(BaseModel):
    """Resumen de un dispositivo emparejado (sin valores secretos)."""

    device_id: str
    device_name: str
    created_at: str
    revoked_at: str | None


@router.post(
    "/claim",
    response_model=ClaimResponse,
    summary="Empareja el dispositivo con un código temporal",
    description=(
        "Reclama un código OTP generado por el administrador y entrega el token "
        "de dispositivo. El código caduca y solo puede usarse una vez; las "
        "peticiones excesivas se limitan por dirección de origen."
    ),
)
async def claim_code(payload: ClaimRequest, service: ServiceDep) -> ClaimResponse:
    """Empareja un dispositivo o rechaza el código de forma genérica."""
    try:
        device_token = service.claim_pairing(payload.code, payload.device_id, payload.device_name)
    except PairingError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Emparejamiento no válido",
        ) from None
    return ClaimResponse(device_token=device_token, device_id=payload.device_id)


@router.get(
    "/devices",
    response_model=list[DeviceInfo],
    summary="Lista los dispositivos emparejados",
    description="Requerido un token de dispositivo válido; no expone hashes ni tokens.",
)
async def list_devices(
    _device: DeviceDep,
    service: ServiceDep,
) -> list[DeviceInfo]:
    """Devuelve el resumen de los dispositivos del usuario autenticado."""
    return [
        DeviceInfo(
            device_id=device.device_id,
            device_name=device.device_name,
            created_at=device.created_at.isoformat(),
            revoked_at=device.revoked_at.isoformat() if device.revoked_at else None,
        )
        for device in service.list_devices()
    ]
