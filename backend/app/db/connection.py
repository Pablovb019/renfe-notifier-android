"""Conexiones SQLite con los pragmas aprobados en la arquitectura.

El fichero vive fuera de ``app/`` (directorio de datos) para no perder la
base de datos al sustituir el paquete durante un despliegue.
"""

import sqlite3
from pathlib import Path

BUSY_TIMEOUT_MS = 5000


def connect(db_path: Path) -> sqlite3.Connection:
    connection = sqlite3.connect(db_path)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA journal_mode = WAL")
    connection.execute(f"PRAGMA busy_timeout = {BUSY_TIMEOUT_MS}")
    connection.execute("PRAGMA foreign_keys = ON")
    return connection


def backup_database(source_path: Path, target_path: Path) -> None:
    """Copia consistente mediante la API ``online backup`` de SQLite.

    No es una copia ciega del fichero ``.db``: SQLite garantiza que el destino
    refleja un estado coherente aunque haya cambios pendientes en el WAL.
    """
    if not source_path.exists():
        raise FileNotFoundError(f"No existe la base de datos: {source_path}")
    target_path.parent.mkdir(parents=True, exist_ok=True)
    source = sqlite3.connect(source_path)
    target = sqlite3.connect(target_path)
    try:
        source.backup(target)
    finally:
        target.close()
        source.close()


def verify_database_integrity(db_path: Path) -> tuple[bool, str]:
    """Verifica la integridad de la base de datos con PRAGMA integrity_check.

    Returns:
        Tuple (ok, message). ok=True si pasa; message contiene detalles.
    """
    if not db_path.exists():
        return False, f"No existe la base de datos: {db_path}"
    conn = sqlite3.connect(db_path)
    try:
        cursor = conn.execute("PRAGMA integrity_check")
        result = cursor.fetchone()
        if result and result[0] == "ok":
            return True, "integrity_check: ok"
        return False, f"integrity_check falló: {result}"
    except sqlite3.DatabaseError as e:
        return False, f"Error de base de datos: {e}"
    finally:
        conn.close()


def get_database_version(db_path: Path) -> int | None:
    """Obtiene la versión de esquema aplicada (máxima en schema_migrations).

    Returns:
        Versión (entero) o None si no existe la tabla.
    """
    if not db_path.exists():
        return None
    conn = sqlite3.connect(db_path)
    try:
        cursor = conn.execute("SELECT MAX(version) FROM schema_migrations")
        row = cursor.fetchone()
        return row[0] if row and row[0] is not None else None
    except sqlite3.DatabaseError:
        return None
    finally:
        conn.close()
