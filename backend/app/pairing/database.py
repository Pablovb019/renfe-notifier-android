"""Persistencia SQLite de códigos de emparejamiento y dispositivos.

Mismo patrón que el resto del backend: transacciones cortas con conexión
efímera, instantes como epoch UTC y enums/valores por su texto.
"""

import sqlite3
from collections.abc import Iterator
from contextlib import contextmanager
from datetime import UTC, datetime
from pathlib import Path

from app.db import apply_migrations, connect
from app.pairing.domain import Device, PairingCode, as_utc


class PairingRepository:
    """Repositorio SQLite de emparejamientos con transacciones cortas.

    Solo se persisten hashes de códigos y tokens; los valores en claro solo
    existen durante el flujo (CLI → response) y nunca en el fichero SQLite.
    """

    _initialized_paths: set[Path] = set()

    def __init__(self, db_path: Path) -> None:
        self._db_path = db_path

    def initialize(self) -> None:
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

    def create_code(self, code: PairingCode) -> None:
        """Persiste un código por su hash; el ``code_id`` se rellena al leerlo."""
        with self._session() as connection:
            connection.execute(
                "INSERT INTO pairing_codes (code_hash, expires_at, max_attempts, attempts, created_at) "
                "VALUES (?, ?, ?, ?, ?)",
                (
                    code.code_hash,
                    _epoch(code.expires_at),
                    code.max_attempts,
                    code.attempts,
                    _epoch(code.created_at or datetime.now(UTC)),
                ),
            )

    def get_by_code_hash(self, code_hash: str) -> PairingCode | None:
        with self._session() as connection:
            row = connection.execute(
                "SELECT * FROM pairing_codes WHERE code_hash = ?", (code_hash,)
            ).fetchone()
        if row is None:
            return None
        return _row_to_code(row)

    def save_code(self, code: PairingCode) -> None:
        with self._session() as connection:
            connection.execute(
                "UPDATE pairing_codes SET attempts = ?, claimed_at = ? WHERE code_hash = ?",
                (
                    code.attempts,
                    _epoch(code.claimed_at) if code.claimed_at else None,
                    code.code_hash,
                ),
            )

    def purge_expired_codes(self, now: datetime) -> int:
        with self._session() as connection:
            cursor = connection.execute(
                "DELETE FROM pairing_codes WHERE claimed_at IS NULL AND expires_at <= ?",
                (_epoch(now),),
            )
        return cursor.rowcount

    def add_device(self, device: Device) -> None:
        """Inserta o reemplaza el dispositivo por su identificador.

        Reclamar de nuevo el mismo ``device_id`` revoca el token previo y
        sustituye la fila, manteniendo un único dispositivo por identificador.
        """
        with self._session() as connection:
            connection.execute(
                "INSERT OR REPLACE INTO devices "
                "(device_id, device_name, token_hash, created_at, revoked_at, fcm_token) "
                "VALUES (?, ?, ?, ?, ?, ?)",
                (
                    device.device_id,
                    device.device_name,
                    device.token_hash,
                    _epoch(device.created_at),
                    _epoch(device.revoked_at) if device.revoked_at else None,
                    device.fcm_token,
                ),
            )

    def get_device_by_token_hash(self, token_hash: str) -> Device | None:
        with self._session() as connection:
            row = connection.execute(
                "SELECT * FROM devices WHERE token_hash = ?", (token_hash,)
            ).fetchone()
        if row is None:
            return None
        return _row_to_device(row)

    def get_device_by_id(self, device_id: str) -> Device | None:
        with self._session() as connection:
            row = connection.execute(
                "SELECT * FROM devices WHERE device_id = ?", (device_id,)
            ).fetchone()
        if row is None:
            return None
        return _row_to_device(row)

    def list_devices(self) -> tuple[Device, ...]:
        with self._session() as connection:
            rows = connection.execute("SELECT * FROM devices ORDER BY created_at").fetchall()
        return tuple(_row_to_device(row) for row in rows)

    def save_device(self, device: Device) -> None:
        with self._session() as connection:
            connection.execute(
                "UPDATE devices SET revoked_at = ? WHERE device_id = ?",
                (_epoch(device.revoked_at) if device.revoked_at else None, device.device_id),
            )

    def update_fcm_token(self, device_id: str, fcm_token: str) -> bool:
        """Registra o renueva el token FCM de un dispositivo activo."""
        with self._session() as connection:
            cursor = connection.execute(
                "UPDATE devices SET fcm_token = ? WHERE device_id = ? AND revoked_at IS NULL",
                (fcm_token, device_id),
            )
            return cursor.rowcount > 0

    def clear_fcm_token(self, device_id: str) -> bool:
        """Invalida el token FCM tras un rechazo UNREGISTERED/404 de FCM."""
        with self._session() as connection:
            cursor = connection.execute(
                "UPDATE devices SET fcm_token = NULL WHERE device_id = ? AND revoked_at IS NULL",
                (device_id,),
            )
            return cursor.rowcount > 0

    def get_device_by_fcm_token(self, fcm_token: str) -> Device | None:
        """Localiza el dispositivo propietario de un token FCM concreto."""
        with self._session() as connection:
            row = connection.execute(
                "SELECT * FROM devices WHERE fcm_token = ?", (fcm_token,)
            ).fetchone()
        return _row_to_device(row) if row is not None else None

    def count_devices(self) -> int:
        """Número total de dispositivos emparejados (activos y revocados)."""
        with self._session() as connection:
            row = connection.execute("SELECT COUNT(*) AS total FROM devices").fetchone()
        return int(row["total"])


def _epoch(instant: datetime) -> int:
    return int(as_utc(instant).timestamp())


def _to_datetime(value: int) -> datetime:
    return datetime.fromtimestamp(value, tz=UTC)


def _to_optional_datetime(value: int | None) -> datetime | None:
    if value is None:
        return None
    return datetime.fromtimestamp(value, tz=UTC)


def _row_to_code(row: sqlite3.Row) -> PairingCode:
    return PairingCode(
        code_id=int(row["id"]),
        code_hash=row["code_hash"],
        expires_at=_to_datetime(int(row["expires_at"])),
        max_attempts=int(row["max_attempts"]),
        attempts=int(row["attempts"]),
        claimed_at=_to_optional_datetime(row["claimed_at"]),
        created_at=_to_datetime(int(row["created_at"])),
    )


def _row_to_device(row: sqlite3.Row) -> Device:
    return Device(
        device_id=row["device_id"],
        device_name=row["device_name"],
        token_hash=row["token_hash"],
        created_at=_to_datetime(int(row["created_at"])),
        revoked_at=_to_optional_datetime(row["revoked_at"]),
        fcm_token=row["fcm_token"],
    )
