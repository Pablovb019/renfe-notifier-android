"""CLI para aplicar migraciones de base de datos (usado por deploy_backend.sh).

Se ejecuta en la VM durante el despliegue. No expone secretos.
"""

import sys

from app.config import Settings
from app.db.connection import connect
from app.db.migrations import apply_migrations


def main() -> int:
    settings = Settings()
    db_path = settings.database_path
    if not db_path.exists():
        print(f"Base de datos no encontrada: {db_path}", file=sys.stderr)
        return 1
    connection = connect(db_path)
    try:
        apply_migrations(connection)
        print(f"Migraciones aplicadas correctamente en {db_path}")
        return 0
    except Exception as e:
        print(f"Error aplicando migraciones: {e}", file=sys.stderr)
        return 1
    finally:
        connection.close()


if __name__ == "__main__":
    raise SystemExit(main())
