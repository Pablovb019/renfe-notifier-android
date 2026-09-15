"""Cola persistente de avisos sobre SQLite con transacciones cortas.

Solo un aviso permanece activo por seguimiento: al encolar un episodio nuevo
se invalidan los anteriores. Cada entrega consume un intento; al agotarlos el
evento queda entregado. Las marcas de estado son idempotentes y el estado se
lee siempre desde SQLite, por lo que la cola se recupera tras reinicios.
"""

import sqlite3
from collections.abc import Iterator
from contextlib import contextmanager
from dataclasses import replace
from datetime import UTC, datetime
from pathlib import Path

from app.db import apply_migrations, connect
from app.reminders.domain import (
    REPLACED,
    ReminderEvent,
    ReminderStatus,
    from_epoch,
    next_remind_at,
    to_epoch,
)


class AlertQueue:
    """Repositorio SQLite de eventos de aviso pendientes de entrega.

    Cada operación abre y cierra su propia conexión en una transacción corta
    (mismo patrón que ``FollowUpRepository``). El retraso inicial de cada
    evento y su tope de intentos los fija el productor al encolar; el resto de
    la configuración (retraso inicial por defecto e intervalo entre
    recordatorios) se inyecta aquí.
    """

    _initialized_paths: set[Path] = set()

    def __init__(self, db_path: Path, *, interval_s: float = 300.0) -> None:
        self._db_path = db_path
        self._interval_s = interval_s

    def initialize(self) -> None:
        """Crea el directorio de datos y aplica las migraciones pendientes."""
        if self._db_path in self._initialized_paths:
            return
        self._db_path.parent.mkdir(parents=True, exist_ok=True)
        connection = connect(self._db_path)
        try:
            apply_migrations(connection)
            connection.commit()
        finally:
            connection.close()
        self._initialized_paths.add(self._db_path)

    @contextmanager
    def _session(self) -> Iterator[sqlite3.Connection]:
        connection = connect(self._db_path)
        try:
            yield connection
            connection.commit()
        except BaseException:
            connection.rollback()
            raise
        finally:
            connection.close()

    def enqueue(self, event: ReminderEvent) -> bool:
        """Registra el aviso si aún no existe y reemplaza avisos anteriores.

        Devuelve ``True`` solo si ha insertado un evento nuevo. Si el
        ``event_id`` ya existe la operación es un no-op (idempotencia). Cuando
        se inserta un episodio más reciente, se invalidan los avisos previos
        del mismo seguimiento aún pendientes o suspendidos.
        """
        with self._session() as connection:
            if connection.execute(
                "SELECT 1 FROM alert_events WHERE event_id = ?", (event.event_id,)
            ).fetchone():
                return False
            connection.execute(
                "UPDATE alert_events SET status = ?, cancelled_reason = ? "
                "WHERE followup_id = ? AND status IN (?, ?)",
                (
                    ReminderStatus.CANCELLED.value,
                    REPLACED,
                    event.followup_id,
                    ReminderStatus.PENDING.value,
                    ReminderStatus.SUSPENDED.value,
                ),
            )
            connection.execute(
                "INSERT INTO alert_events ("
                "event_id, followup_id, episode_id, observed_at, expires_at,"
                "created_at, remind_at, status, attempts, max_attempts"
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (
                    event.event_id,
                    event.followup_id,
                    event.episode_id,
                    to_epoch(event.observed_at),
                    to_epoch(event.expires_at),
                    to_epoch(event.created_at or datetime.now(UTC)),
                    to_epoch(event.remind_at),
                    event.status.value,
                    event.attempts,
                    event.max_attempts,
                ),
            )
        return True

    def get(self, event_id: str) -> ReminderEvent | None:
        with self._session() as connection:
            row = connection.execute(
                "SELECT * FROM alert_events WHERE event_id = ?", (event_id,)
            ).fetchone()
        if row is None:
            return None
        return _row_to_event(row)

    def due_events(self, now: datetime, limit: int) -> tuple[ReminderEvent, ...]:
        """Avisos pendientes cuyo próximo recordatorio ya ha vencido."""
        with self._session() as connection:
            rows = connection.execute(
                "SELECT * FROM alert_events "
                "WHERE status = ? AND remind_at <= ? "
                "ORDER BY remind_at LIMIT ?",
                (ReminderStatus.PENDING.value, to_epoch(now), limit),
            ).fetchall()
        return tuple(_row_to_event(row) for row in rows)

    def deliver(self, event_id: str, now: datetime) -> bool:
        """Consume un intento y programa el siguiente recordatorio.

        Tras el último intento el evento pasa a ``delivered``. Si el evento no
        existe o ya no está pendiente, la operación es un no-op (idempotente).
        """
        delivered_at = to_epoch(now)
        with self._session() as connection:
            row = connection.execute(
                "SELECT * FROM alert_events WHERE event_id = ? AND status = ?",
                (event_id, ReminderStatus.PENDING.value),
            ).fetchone()
            if row is None:
                return False
            event = _row_to_event(row)
            e = replace(event, attempts=event.attempts + 1)
            if e.attempts >= event.max_attempts:
                connection.execute(
                    "UPDATE alert_events SET attempts = ?, status = ?, delivered_at = ? "
                    "WHERE event_id = ?",
                    (e.attempts, ReminderStatus.DELIVERED.value, delivered_at, event.event_id),
                )
            else:
                connection.execute(
                    "UPDATE alert_events SET attempts = ?, remind_at = ? WHERE event_id = ?",
                    (
                        e.attempts,
                        to_epoch(next_remind_at(now, self._interval_s)),
                        event.event_id,
                    ),
                )
        return True

    def mark_failed(self, event_id: str, reason: str) -> bool:
        """Falla un aviso pendiente o suspendido; no-op en el resto."""
        with self._session() as connection:
            cursor = connection.execute(
                "UPDATE alert_events SET status = ?, cancelled_reason = ? "
                "WHERE event_id = ? AND status IN (?, ?)",
                (
                    ReminderStatus.FAILED.value,
                    reason,
                    event_id,
                    ReminderStatus.PENDING.value,
                    ReminderStatus.SUSPENDED.value,
                ),
            )
            return cursor.rowcount > 0

    def cancel_for_followup(self, followup_id: str, reason: str) -> int:
        """Cancela los avisos activos de un seguimiento. Devuelve el total."""
        with self._session() as connection:
            cursor = connection.execute(
                "UPDATE alert_events SET status = ?, cancelled_reason = ? "
                "WHERE followup_id = ? AND status IN (?, ?)",
                (
                    ReminderStatus.CANCELLED.value,
                    reason,
                    followup_id,
                    ReminderStatus.PENDING.value,
                    ReminderStatus.SUSPENDED.value,
                ),
            )
        return cursor.rowcount

    def suspend_for_followup(self, followup_id: str) -> int:
        """Suspende los avisos pendientes por disponibilidad obsoleta."""
        with self._session() as connection:
            cursor = connection.execute(
                "UPDATE alert_events SET status = ? WHERE followup_id = ? AND status = ?",
                (ReminderStatus.SUSPENDED.value, followup_id, ReminderStatus.PENDING.value),
            )
        return cursor.rowcount

    def count_pending(self) -> int:
        with self._session() as connection:
            row = connection.execute(
                "SELECT COUNT(*) AS total FROM alert_events WHERE status = ?",
                (ReminderStatus.PENDING.value,),
            ).fetchone()
        return int(row["total"])


def _row_to_event(row: sqlite3.Row) -> ReminderEvent:
    return ReminderEvent(
        event_id=row["event_id"],
        followup_id=row["followup_id"],
        episode_id=int(row["episode_id"]),
        observed_at=from_epoch(int(row["observed_at"])),
        expires_at=from_epoch(int(row["expires_at"])),
        created_at=from_epoch(int(row["created_at"])),
        remind_at=from_epoch(int(row["remind_at"])),
        status=ReminderStatus(row["status"]),
        attempts=int(row["attempts"]),
        max_attempts=int(row["max_attempts"]),
        delivered_at=from_epoch(int(row["delivered_at"]))
        if row["delivered_at"] is not None
        else None,
        cancelled_reason=row["cancelled_reason"],
    )
