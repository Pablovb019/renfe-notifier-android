"""Modelo puro de seguimientos y episodios de disponibilidad.

Todos los instantes se almacenan como ``datetime`` consciente en UTC. Las
fechas y horarios de viaje se interpretan exclusivamente en Europe/Madrid.
"""

from dataclasses import dataclass, replace
from datetime import UTC, date, datetime, time, timedelta
from enum import StrEnum
from zoneinfo import ZoneInfo

from app.renfe.parser import Availability

MADRID = ZoneInfo("Europe/Madrid")
INITIAL_TTL = timedelta(days=30)


class Lifecycle(StrEnum):
    ACTIVE = "active"
    PAUSED = "paused"
    EXPIRED = "expired"
    DELETED = "deleted"


class AvailabilityState(StrEnum):
    UNKNOWN = "unknown"
    UNAVAILABLE = "unavailable"
    AVAILABLE = "available"


class AlertState(StrEnum):
    IDLE = "idle"
    PENDING = "pending_alert"
    ACKNOWLEDGED = "acknowledged"


class FollowUpMode(StrEnum):
    SPECIFIC = "specific"
    FIRST = "first"
    LAST = "last"
    ALL = "all"


class ObservationKind(StrEnum):
    VALID = "valid"
    ERROR = "error"
    STALE = "stale"


@dataclass(frozen=True, slots=True)
class TrainSnapshot:
    """Tren observado para aplicar las reglas del seguimiento.

    ``real_id`` solo debe rellenarse cuando el protocolo aporte un identificador
    real. Sin él se usa el fallback documentado: ruta, fecha local y horarios.
    Este fallback puede colisionar si dos trenes comparten exactamente esos
    campos, por lo que se prefiere siempre el ID real cuando exista.
    """

    departure: time | None
    arrival: time | None
    availability: Availability
    real_id: str | None = None

    def identity(self, *, origin_code: str, destination_code: str, travel_date: date) -> str:
        if self.real_id:
            return f"real:{self.real_id}"
        departure = self.departure.isoformat() if self.departure else "unknown"
        arrival = self.arrival.isoformat() if self.arrival else "unknown"
        return f"fallback:{origin_code}:{destination_code}:{travel_date.isoformat()}:{departure}:{arrival}"


@dataclass(frozen=True, slots=True)
class Observation:
    kind: ObservationKind
    observed_at: datetime
    trains: tuple[TrainSnapshot, ...] = ()


@dataclass(frozen=True, slots=True)
class FollowUp:
    followup_id: str
    origin_code: str
    destination_code: str
    travel_date: date
    mode: FollowUpMode
    plaza_h: bool
    expires_at: datetime
    lifecycle: Lifecycle = Lifecycle.ACTIVE
    availability: AvailabilityState = AvailabilityState.UNKNOWN
    alert_state: AlertState = AlertState.IDLE
    episode: int = 0
    specific_train_id: str | None = None
    seen_available_train_ids: frozenset[str] = frozenset()

    @classmethod
    def create(
        cls,
        *,
        followup_id: str,
        origin_code: str,
        destination_code: str,
        travel_date: date,
        mode: FollowUpMode,
        now: datetime,
        departure_time: time | None = None,
        specific_train_id: str | None = None,
        plaza_h: bool = False,
    ) -> "FollowUp":
        if mode is FollowUpMode.SPECIFIC and not specific_train_id:
            raise ValueError("Un seguimiento específico requiere specific_train_id")
        return cls(
            followup_id=followup_id,
            origin_code=origin_code,
            destination_code=destination_code,
            travel_date=travel_date,
            mode=mode,
            plaza_h=plaza_h,
            expires_at=initial_expiry(
                now=now, travel_date=travel_date, mode=mode, departure_time=departure_time
            ),
            specific_train_id=specific_train_id,
        )

    def apply(self, observation: Observation) -> "ObservationResult":
        """Aplica solo observaciones válidas y crea episodios al reaparecer plazas."""
        observed_at = as_utc(observation.observed_at)
        if self.lifecycle is not Lifecycle.ACTIVE:
            return ObservationResult(self, False)
        if observed_at >= self.expires_at:
            return ObservationResult(self.expire(), False)
        if observation.kind is not ObservationKind.VALID:
            return ObservationResult(self, False)

        selected = self._select(observation.trains)
        available_ids = frozenset(
            train.identity(
                origin_code=self.origin_code,
                destination_code=self.destination_code,
                travel_date=self.travel_date,
            )
            for train in selected
            if train.availability is Availability.AVAILABLE
        )
        if not available_ids:
            return ObservationResult(
                replace(
                    self,
                    availability=AvailabilityState.UNAVAILABLE,
                    alert_state=AlertState.IDLE,
                    seen_available_train_ids=frozenset(),
                ),
                False,
            )

        newly_seen = available_ids - self.seen_available_train_ids
        is_new_episode = self.availability is not AvailabilityState.AVAILABLE or (
            self.mode is FollowUpMode.ALL and bool(newly_seen)
        )
        if is_new_episode:
            return ObservationResult(
                replace(
                    self,
                    availability=AvailabilityState.AVAILABLE,
                    alert_state=AlertState.PENDING,
                    episode=self.episode + 1,
                    seen_available_train_ids=self.seen_available_train_ids | available_ids,
                ),
                True,
            )
        return ObservationResult(
            replace(
                self,
                availability=AvailabilityState.AVAILABLE,
                seen_available_train_ids=self.seen_available_train_ids | available_ids,
            ),
            False,
        )

    def acknowledge(self) -> "FollowUp":
        """Confirma el episodio actual sin borrar ni pausar el seguimiento."""
        if self.alert_state is not AlertState.PENDING:
            return self
        return replace(self, alert_state=AlertState.ACKNOWLEDGED)

    def pause(self) -> "FollowUp":
        if self.lifecycle is not Lifecycle.ACTIVE:
            return self
        return replace(
            self,
            lifecycle=Lifecycle.PAUSED,
            availability=AvailabilityState.UNKNOWN,
            alert_state=AlertState.IDLE,
            seen_available_train_ids=frozenset(),
        )

    def resume(self, now: datetime) -> "FollowUp":
        if self.lifecycle is not Lifecycle.PAUSED:
            return self
        if as_utc(now) >= self.expires_at:
            return self.expire()
        return replace(self, lifecycle=Lifecycle.ACTIVE)

    def renew(self, *, now: datetime, departure_time: time | None = None) -> "FollowUp":
        if self.lifecycle is Lifecycle.DELETED:
            return self
        return replace(
            self,
            lifecycle=Lifecycle.ACTIVE,
            availability=AvailabilityState.UNKNOWN,
            alert_state=AlertState.IDLE,
            seen_available_train_ids=frozenset(),
            expires_at=initial_expiry(
                now=now, travel_date=self.travel_date, mode=self.mode, departure_time=departure_time
            ),
        )

    def delete(self) -> "FollowUp":
        return replace(self, lifecycle=Lifecycle.DELETED, alert_state=AlertState.IDLE)

    def expire(self) -> "FollowUp":
        return replace(self, lifecycle=Lifecycle.EXPIRED, alert_state=AlertState.IDLE)

    def _select(self, trains: tuple[TrainSnapshot, ...]) -> tuple[TrainSnapshot, ...]:
        if self.mode is FollowUpMode.ALL:
            return trains
        if self.mode is FollowUpMode.SPECIFIC:
            return tuple(
                train
                for train in trains
                if train.identity(
                    origin_code=self.origin_code,
                    destination_code=self.destination_code,
                    travel_date=self.travel_date,
                )
                == self.specific_train_id
            )

        def known_time(train: TrainSnapshot) -> time:
            return train.departure or time.max

        if not trains:
            return ()
        selected = (
            min(trains, key=known_time)
            if self.mode is FollowUpMode.FIRST
            else max(trains, key=known_time)
        )
        return (selected,)


@dataclass(frozen=True, slots=True)
class ObservationResult:
    followup: FollowUp
    new_episode: bool


def as_utc(instant: datetime) -> datetime:
    if instant.tzinfo is None:
        raise ValueError("Los instantes deben incluir zona horaria")
    return instant.astimezone(UTC)


def departure_at_utc(travel_date: date, departure_time: time) -> datetime:
    """Interpreta una salida local de Madrid y la convierte a UTC."""
    return datetime.combine(travel_date, departure_time, tzinfo=MADRID).astimezone(UTC)


def arrival_at_utc(travel_date: date, departure_time: time, arrival_time: time) -> datetime:
    """Interpreta llegadas tras medianoche como pertenecientes al día siguiente."""
    arrival_date = (
        travel_date + timedelta(days=1) if arrival_time <= departure_time else travel_date
    )
    return datetime.combine(arrival_date, arrival_time, tzinfo=MADRID).astimezone(UTC)


def end_of_travel_day_utc(travel_date: date) -> datetime:
    return datetime.combine(travel_date + timedelta(days=1), time.min, tzinfo=MADRID).astimezone(
        UTC
    )


def initial_expiry(
    *, now: datetime, travel_date: date, mode: FollowUpMode, departure_time: time | None
) -> datetime:
    """Aplica 30 días, limitados por salida o por el fin local del día para ALL."""
    utc_now = as_utc(now)
    ttl_limit = utc_now + INITIAL_TTL
    travel_limit = (
        end_of_travel_day_utc(travel_date)
        if mode is FollowUpMode.ALL or departure_time is None
        else departure_at_utc(travel_date, departure_time)
    )
    return min(ttl_limit, travel_limit)
