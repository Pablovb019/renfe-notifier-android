"""Modelo puro de eventos de aviso y su programación.

Un evento representa un único episodio de disponibilidad (episodio concreto
de un seguimiento) que requiere llamar la atención del usuario. Cada evento
se entrega mediante uno o varios recordatorios acotados y configurables.

La entrega es *al menos una vez*: los reintentos son acotados, las marcas de
estado idempotentes y nunca se promete entrega exactamente una vez.
"""

from dataclasses import dataclass
from datetime import UTC, datetime, timedelta
from enum import StrEnum

from app.followups.domain import as_utc


class ReminderStatus(StrEnum):
    PENDING = "pending"
    SUSPENDED = "suspended"
    CANCELLED = "cancelled"
    DELIVERED = "delivered"
    FAILED = "failed"


REPLACED = "replaced"
STALE = "stale"


@dataclass(frozen=True, slots=True)
class ReminderEvent:
    """Aviso persistente ligado a un episodio concreto.

    ``episode_id`` es la clave interna de la fila en la tabla ``episodes``,
    ``observed_at`` el instante de la detección y ``expires_at`` la caducidad
    de su seguimiento (momento límite en que pierde sentido recordar).
    """

    event_id: str
    followup_id: str
    episode_id: int
    observed_at: datetime
    expires_at: datetime
    remind_at: datetime
    status: ReminderStatus
    attempts: int
    max_attempts: int
    created_at: datetime | None = None
    delivered_at: datetime | None = None
    cancelled_reason: str | None = None

    @classmethod
    def create(
        cls,
        *,
        event_id: str,
        followup_id: str,
        episode_id: int,
        observed_at: datetime,
        expires_at: datetime,
        initial_delay_s: float,
        max_attempts: int,
        now: datetime,
    ) -> "ReminderEvent":
        """Cola el primer recordatorio en ``observed_at + initial_delay_s``."""
        return cls(
            event_id=event_id,
            followup_id=followup_id,
            episode_id=episode_id,
            observed_at=as_utc(observed_at),
            expires_at=as_utc(expires_at),
            remind_at=first_remind_at(observed_at, initial_delay_s),
            status=ReminderStatus.PENDING,
            attempts=0,
            max_attempts=max_attempts,
            created_at=as_utc(now),
        )


def first_remind_at(observed_at: datetime, initial_delay_s: float) -> datetime:
    """Instante del primer recordatorio tras detectar el episodio."""
    return as_utc(observed_at) + timedelta(seconds=initial_delay_s)


def next_remind_at(now: datetime, interval_s: float) -> datetime:
    """Instante del siguiente recordatorio tras uno ya entregado."""
    return as_utc(now) + timedelta(seconds=interval_s)


def to_epoch(instant: datetime) -> int:
    return int(as_utc(instant).timestamp())


def from_epoch(value: int) -> datetime:
    return datetime.fromtimestamp(value, tz=UTC)
