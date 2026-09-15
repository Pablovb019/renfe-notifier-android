"""Persistencia SQLite de seguimientos, episodios y configuración del dominio.

Cada operación abre y cierra su propia conexión en una transacción corta.
Nunca se mantienen transacciones abiertas durante consultas de red.
"""

import json
import sqlite3
from collections.abc import Iterator
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import UTC, date, datetime
from pathlib import Path
from typing import Any

from app.db import apply_migrations, connect
from app.followups.domain import (
    AlertState,
    AvailabilityState,
    FollowUp,
    FollowUpMode,
    Lifecycle,
)


@dataclass(frozen=True, slots=True)
class Episode:
    episode_id: int
    followup_id: str
    episode: int
    observed_at: datetime
    train_ids: frozenset[str]


class FollowUpRepository:
    """Repositorio SQLite con transacciones cortas y cierre garantizado.

    Los enums del dominio se persisten por su valor textual y los instantes
    como epoch UTC (entero). Las fechas de viaje viajan como ``YYYY-MM-DD``.
    """

    _initialized_paths: set[Path] = set()

    def __init__(self, db_path: Path) -> None:
        self._db_path = db_path

    def initialize(self) -> None:
        """Crea el directorio de datos y aplica las migraciones pendientes."""
        if self._db_path in self._initialized_paths:
            # Verificar si las tablas realmente existen (para casos de recuperación)
            try:
                connection = connect(self._db_path)
                cursor = connection.execute(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='followups'"
                )
                if cursor.fetchone() is not None:
                    connection.close()
                    return
                connection.close()
            except sqlite3.Error:
                pass  # Si hay error, continuar con inicialización
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

    def create(self, followup: FollowUp, *, now: datetime | None = None) -> None:
        created_at = _epoch(now or datetime.now(UTC))
        values = (
            *_followup_values(followup, updated_at=created_at),
            created_at,
            followup.followup_id,
        )
        with self._session() as connection:
            connection.execute(
                "INSERT INTO followups ("
                "origin_code, destination_code, travel_date, mode, plaza_h, expires_at,"
                "lifecycle, availability, alert_state, episode, specific_train_id,"
                "seen_available_train_ids, updated_at, created_at, followup_id"
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                values,
            )

    def save(self, followup: FollowUp, *, now: datetime | None = None) -> None:
        updated_at = _epoch(now or datetime.now(UTC))
        values = (*_followup_values(followup, updated_at=updated_at), followup.followup_id)
        with self._session() as connection:
            connection.execute(
                "UPDATE followups SET origin_code = ?, destination_code = ?, "
                "travel_date = ?, mode = ?, plaza_h = ?, expires_at = ?, lifecycle = ?, "
                "availability = ?, alert_state = ?, episode = ?, "
                "specific_train_id = ?, seen_available_train_ids = ?, updated_at = ? "
                "WHERE followup_id = ?",
                values,
            )

    def get(self, followup_id: str) -> FollowUp | None:
        with self._session() as connection:
            row = connection.execute(
                "SELECT * FROM followups WHERE followup_id = ?", (followup_id,)
            ).fetchone()
        if row is None:
            return None
        return _row_to_followup(row)

    def list(self, lifecycle: Lifecycle | None = None) -> tuple[FollowUp, ...]:
        sql = "SELECT * FROM followups"
        params: tuple[Any, ...] = ()
        if lifecycle is not None:
            sql += " WHERE lifecycle = ?"
            params = (lifecycle.value,)
        sql += " ORDER BY created_at"
        with self._session() as connection:
            rows = connection.execute(sql, params).fetchall()
        return tuple(_row_to_followup(row) for row in rows)

    def purge_deleted(self) -> int:
        """Purgado físico controlado de los seguimientos en estado borrado."""
        with self._session() as connection:
            cursor = connection.execute(
                "DELETE FROM followups WHERE lifecycle = ?", (Lifecycle.DELETED.value,)
            )
        return cursor.rowcount

    def count(self, lifecycle: Lifecycle) -> int:
        """Número de seguimientos en un estado del ciclo de vida."""
        with self._session() as connection:
            row = connection.execute(
                "SELECT COUNT(*) AS total FROM followups WHERE lifecycle = ?", (lifecycle.value,)
            ).fetchone()
        return int(row["total"])

    def count_episodes(self) -> int:
        """Número total de episodios registrados en la base de datos."""
        with self._session() as connection:
            row = connection.execute("SELECT COUNT(*) AS total FROM episodes").fetchone()
        return int(row["total"])

    def add_episode(
        self, followup_id: str, episode: int, observed_at: datetime, train_ids: frozenset[str]
    ) -> Episode:
        """Registra el episodio (idempotente) y devuelve su fila persistida."""
        created_at = _epoch(observed_at)
        with self._session() as connection:
            connection.execute(
                "INSERT OR IGNORE INTO episodes "
                "(followup_id, episode, observed_at, train_ids, created_at) "
                "VALUES (?, ?, ?, ?, ?)",
                (followup_id, episode, created_at, _ids_to_json(train_ids), created_at),
            )
            row = connection.execute(
                "SELECT id, followup_id, episode, observed_at, train_ids "
                "FROM episodes WHERE followup_id = ? AND episode = ?",
                (followup_id, episode),
            ).fetchone()
        assert row is not None
        return _row_to_episode(row)

    def list_episodes(self, followup_id: str) -> tuple[Episode, ...]:
        with self._session() as connection:
            rows = connection.execute(
                "SELECT id, followup_id, episode, observed_at, train_ids FROM episodes "
                "WHERE followup_id = ? ORDER BY episode",
                (followup_id,),
            ).fetchall()
        return tuple(_row_to_episode(row) for row in rows)

    def set_config(self, key: str, value: str) -> None:
        updated_at = _epoch(datetime.now(UTC))
        with self._session() as connection:
            connection.execute(
                "INSERT INTO app_config (key, value, updated_at) VALUES (?, ?, ?) "
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value, "
                "updated_at = excluded.updated_at",
                (key, value, updated_at),
            )

    def get_config(self, key: str) -> str | None:
        with self._session() as connection:
            row = connection.execute(
                "SELECT value FROM app_config WHERE key = ?", (key,)
            ).fetchone()
        if row is None:
            return None
        return str(row["value"])


def _followup_values(followup: FollowUp, *, updated_at: int) -> tuple[Any, ...]:
    return (
        followup.origin_code,
        followup.destination_code,
        followup.travel_date.isoformat(),
        followup.mode.value,
        1 if followup.plaza_h else 0,
        _epoch(followup.expires_at),
        followup.lifecycle.value,
        followup.availability.value,
        followup.alert_state.value,
        followup.episode,
        followup.specific_train_id,
        _ids_to_json(followup.seen_available_train_ids),
        updated_at,
    )


def _epoch(instant: datetime) -> int:
    return int(instant.astimezone(UTC).timestamp())


def _epoch_to_datetime(value: int) -> datetime:
    return datetime.fromtimestamp(value, tz=UTC)


def _ids_to_json(ids: frozenset[str]) -> str:
    return json.dumps(sorted(ids))


def _json_to_ids(value: str) -> frozenset[str]:
    return frozenset(json.loads(value))


def _row_to_followup(row: sqlite3.Row) -> FollowUp:
    return FollowUp(
        followup_id=row["followup_id"],
        origin_code=row["origin_code"],
        destination_code=row["destination_code"],
        travel_date=date.fromisoformat(row["travel_date"]),
        mode=FollowUpMode(row["mode"]),
        plaza_h=bool(row["plaza_h"]),
        expires_at=_epoch_to_datetime(int(row["expires_at"])),
        lifecycle=Lifecycle(row["lifecycle"]),
        availability=AvailabilityState(row["availability"]),
        alert_state=AlertState(row["alert_state"]),
        episode=int(row["episode"]),
        specific_train_id=row["specific_train_id"],
        seen_available_train_ids=_json_to_ids(row["seen_available_train_ids"]),
    )


def _row_to_episode(row: sqlite3.Row) -> Episode:
    return Episode(
        episode_id=int(row["id"]),
        followup_id=row["followup_id"],
        episode=int(row["episode"]),
        observed_at=_epoch_to_datetime(int(row["observed_at"])),
        train_ids=_json_to_ids(row["train_ids"]),
    )
