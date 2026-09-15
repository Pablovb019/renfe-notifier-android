"""Capa de persistencia SQLite: conexión segura y migraciones versionadas."""

from app.db.connection import backup_database, connect
from app.db.migrations import MIGRATIONS, apply_migrations

__all__ = ["MIGRATIONS", "apply_migrations", "backup_database", "connect"]
