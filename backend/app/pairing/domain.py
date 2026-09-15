"""Modelo puro de emparejamiento de dispositivos para un único usuario.

El emparejamiento usa un código temporal de baja entropía (caducidad corta y
límite de intentos) que el administrador genera vía CLI y la app consume una
única vez. A cambio se entrega un token criptográfico por dispositivo; la base
de datos solo guarda su hash SHA-256, nunca el valor en claro.
"""

import hashlib
import secrets
from dataclasses import dataclass
from datetime import UTC, datetime

CODE_PREFIX = "RF"
CODE_DIGITS = 6


def generate_pairing_code() -> str:
    """Código temporal legible del estilo ``RF-123456`` para introducir a mano."""
    return f"{CODE_PREFIX}-{secrets.randbelow(10**CODE_DIGITS):0{CODE_DIGITS}d}"


def hash_secret(secret: str) -> str:
    """Hash SHA-256 en hexadecimal; los valores secretos viajan siempre hash."""
    return hashlib.sha256(secret.encode("utf-8")).hexdigest()


def as_utc(instant: datetime) -> datetime:
    """Exige instantes conscientes y los normaliza a UTC."""
    if instant.tzinfo is None:
        raise ValueError("Los instantes deben incluir zona horaria")
    return instant.astimezone(UTC)


@dataclass(frozen=True, slots=True)
class PairingCode:
    """Código temporal de emparejamiento persistido por su hash.

    ``attempts`` cuenta reintentos fallidos y ``can_try`` se agota al alcanzar
    ``max_attempts``; ``claimed_at`` deja el código inutilizable.
    """

    code_hash: str
    expires_at: datetime
    max_attempts: int
    attempts: int = 0
    claimed_at: datetime | None = None
    code_id: int | None = None
    created_at: datetime | None = None

    def is_expired(self, now: datetime) -> bool:
        return as_utc(now) >= self.expires_at

    def is_claimed(self) -> bool:
        return self.claimed_at is not None

    def can_try(self) -> bool:
        return self.attempts < self.max_attempts

    def fail(self, now: datetime) -> "PairingCode":
        """Registra un intento fallido sin sobrepasar el límite."""
        attempts = min(self.attempts + 1, self.max_attempts)
        return PairingCode(
            code_hash=self.code_hash,
            expires_at=self.expires_at,
            max_attempts=self.max_attempts,
            attempts=attempts,
            claimed_at=self.claimed_at,
            code_id=self.code_id,
            created_at=self.created_at,
        )

    def claim(self, now: datetime) -> "PairingCode":
        """Marca el código como usado en el instante indicado."""
        return PairingCode(
            code_hash=self.code_hash,
            expires_at=self.expires_at,
            max_attempts=self.max_attempts,
            attempts=self.attempts,
            claimed_at=as_utc(now),
            code_id=self.code_id,
            created_at=self.created_at,
        )


@dataclass(frozen=True, slots=True)
class Device:
    """Dispositivo emparejado; recuerda solo el hash del token."""

    device_id: str
    device_name: str
    token_hash: str
    created_at: datetime
    revoked_at: datetime | None = None
    fcm_token: str | None = None

    @property
    def is_active(self) -> bool:
        return self.revoked_at is None

    def revoke(self, now: datetime) -> "Device":
        return Device(
            device_id=self.device_id,
            device_name=self.device_name,
            token_hash=self.token_hash,
            created_at=self.created_at,
            revoked_at=as_utc(now),
            fcm_token=self.fcm_token,
        )
