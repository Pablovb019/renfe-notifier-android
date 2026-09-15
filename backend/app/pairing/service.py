"""Servicio de emparejamiento y verificación de tokens de dispositivo.

Conecta el dominio puro con la persistencia aplicando las reglas de caducidad
del código, límite de intentos, reclamación única y revocación. El reloj es
inyectable para poder probar las transiciones temporales de forma determinista.
"""

import secrets
from collections.abc import Callable
from datetime import UTC, datetime, timedelta

from app.pairing.database import PairingRepository
from app.pairing.domain import Device, PairingCode, as_utc, generate_pairing_code, hash_secret

Clock = Callable[[], datetime]


class PairingError(Exception):
    """Código de emparejamiento no válido, caducado, agotado o ya usado.

    Unifica todos los motivos para no revelar cuál aplica a un atacante.
    """


class PairingService:
    """Operaciones de emparejamiento sobre ``PairingRepository``."""

    def __init__(
        self,
        repository: PairingRepository,
        *,
        code_ttl_s: float = 600.0,
        max_attempts: int = 3,
        clock: Clock = lambda: datetime.now(UTC),
    ) -> None:
        self._repo = repository
        self._code_ttl_s = code_ttl_s
        self._max_attempts = max_attempts
        self._clock = clock

    def initialize(self) -> None:
        """Crea el esquema de la base de datos si aún no existe."""
        self._repo.initialize()

    def create_pairing_code(self) -> str:
        """Genera y guarda un código temporal; devuelve el valor en claro.

        El valor solo se imprime por la CLI del administrador; la BD conserva
        únicamente su hash.
        """
        now = as_utc(self._clock())
        code = generate_pairing_code()
        self._repo.create_code(
            PairingCode(
                code_hash=hash_secret(code),
                expires_at=now + timedelta(seconds=self._code_ttl_s),
                max_attempts=self._max_attempts,
                created_at=now,
            )
        )
        return code

    def claim_pairing(self, code: str, device_id: str, device_name: str) -> str:
        """Reclama un código y entrega el token del dispositivo.

        Levanta ``PairingError`` para cualquier código inválido, caducado,
        agotado o ya usado. Un intento erróneo consume uno de los intentos del
        código; reclamar de nuevo el mismo dispositivo revoca su token previo.
        """
        now = as_utc(self._clock())
        stored = self._repo.get_by_code_hash(hash_secret(code))
        if stored is None:
            raise PairingError("Código de emparejamiento no válido")
        if stored.is_claimed() or stored.is_expired(now) or not stored.can_try():
            raise PairingError("Código de emparejamiento no válido")

        existing = self._repo.get_device_by_id(device_id)
        if existing is not None and existing.is_active:
            self._repo.save_device(existing.revoke(now))

        device_token = secrets.token_urlsafe(48)
        device = Device(
            device_id=device_id,
            device_name=device_name,
            token_hash=hash_secret(device_token),
            created_at=now,
        )
        self._repo.add_device(device)
        self._repo.save_code(stored.claim(now))
        return device_token

    def get_device_by_token(self, token: str) -> Device | None:
        """Devuelve el dispositivo si el token es válido y no está revocado."""
        device = self._repo.get_device_by_token_hash(hash_secret(token))
        if device is None or not device.is_active:
            return None
        return device

    def list_devices(self) -> tuple[Device, ...]:
        return self._repo.list_devices()

    def register_fcm_token(self, device_id: str, fcm_token: str) -> bool:
        """Registra o renueva el token FCM de un dispositivo activo."""
        return self._repo.update_fcm_token(device_id, fcm_token)

    def clear_fcm_token(self, device_id: str) -> bool:
        """Invalida el token FCM de un dispositivo sin revocar el emparejamiento."""
        return self._repo.clear_fcm_token(device_id)

    def invalidate_fcm_token(self, fcm_token: str) -> bool:
        """Invalida un token FCM concreto (FCM lo ha rechazado como UNREGISTERED)."""
        device = self._repo.get_device_by_fcm_token(fcm_token)
        if device is None:
            return False
        return self._repo.clear_fcm_token(device.device_id)

    def revoke(self, device_id: str) -> bool:
        device = self._repo.get_device_by_id(device_id)
        if device is None or not device.is_active:
            return False
        self._repo.save_device(device.revoke(self._clock()))
        return True

    def revoke_all(self) -> int:
        """Revoca todos los dispositivos activos; devuelve el número revocado."""
        now = as_utc(self._clock())
        revoked = 0
        for device in self._repo.list_devices():
            if device.is_active:
                self._repo.save_device(device.revoke(now))
                revoked += 1
        return revoked
