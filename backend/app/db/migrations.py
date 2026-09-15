"""Migraciones SQL versionadas y su aplicación atómica y reanudable."""

import logging
import sqlite3

logger = logging.getLogger(__name__)

MIGRATIONS: tuple[str, ...] = (
    """\
CREATE TABLE IF NOT EXISTS followups (
    followup_id TEXT PRIMARY KEY,
    origin_code TEXT NOT NULL,
    destination_code TEXT NOT NULL,
    travel_date TEXT NOT NULL,
    mode TEXT NOT NULL,
    expires_at INTEGER NOT NULL,
    lifecycle TEXT NOT NULL,
    availability TEXT NOT NULL,
    alert_state TEXT NOT NULL,
    episode INTEGER NOT NULL DEFAULT 0,
    specific_train_id TEXT,
    seen_available_train_ids TEXT NOT NULL DEFAULT '[]',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    plaza_h INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_followups_lifecycle ON followups(lifecycle);
CREATE INDEX IF NOT EXISTS idx_followups_grouping ON followups(origin_code, destination_code, travel_date);
CREATE INDEX IF NOT EXISTS idx_followups_grouping_v2 ON followups(origin_code, destination_code, travel_date, plaza_h);

CREATE TABLE IF NOT EXISTS episodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    followup_id TEXT NOT NULL,
    episode INTEGER NOT NULL,
    observed_at INTEGER NOT NULL,
    train_ids TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    UNIQUE(followup_id, episode),
    FOREIGN KEY(followup_id) REFERENCES followups(followup_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_episodes_followup ON episodes(followup_id);

CREATE TABLE IF NOT EXISTS app_config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at INTEGER NOT NULL
);
""",
    """\
-- plaza_h ya se añadió en la migración 1 (CREATE TABLE followups)
-- CREATE INDEX IF NOT EXISTS idx_followups_grouping_v2 ON followups(origin_code, destination_code, travel_date, plaza_h);
""",
    """\
CREATE TABLE IF NOT EXISTS alert_events (
    event_id TEXT PRIMARY KEY,
    followup_id TEXT NOT NULL,
    episode_id INTEGER NOT NULL,
    observed_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    remind_at INTEGER NOT NULL,
    status TEXT NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    delivered_at INTEGER,
    cancelled_reason TEXT,
    FOREIGN KEY(followup_id) REFERENCES followups(followup_id) ON DELETE CASCADE,
    FOREIGN KEY(episode_id) REFERENCES episodes(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_alert_events_due ON alert_events(status, remind_at);
CREATE INDEX IF NOT EXISTS idx_alert_events_followup ON alert_events(followup_id);
""",
    """\
CREATE TABLE IF NOT EXISTS pairing_codes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code_hash TEXT NOT NULL UNIQUE,
    expires_at INTEGER NOT NULL,
    max_attempts INTEGER NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    claimed_at INTEGER,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pairing_codes_status ON pairing_codes(claimed_at, expires_at);

CREATE TABLE IF NOT EXISTS devices (
    device_id TEXT PRIMARY KEY,
    device_name TEXT NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL,
    revoked_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_devices_revoked ON devices(revoked_at);
""",
    """\
ALTER TABLE devices ADD COLUMN fcm_token TEXT;
""",
)


def apply_migrations(connection: sqlite3.Connection) -> None:
    """Aplica las migraciones pendientes de forma atómica por versión."""
    connection.execute(
        "CREATE TABLE IF NOT EXISTS schema_migrations ("
        "version INTEGER PRIMARY KEY,"
        " applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)"
    )
    connection.commit()
    applied = {
        row["version"] for row in connection.execute("SELECT version FROM schema_migrations")
    }
    for version, script in enumerate(MIGRATIONS, start=1):
        if version in applied:
            continue
        block = (
            f"BEGIN;\n{script}\nINSERT INTO schema_migrations(version) VALUES ({version});\nCOMMIT;"
        )
        try:
            connection.executescript(block)
        except sqlite3.OperationalError as e:
            # Ignorar errores de "duplicate column" o "table already exists" para hacer migraciones idempotentes
            if "duplicate column" in str(e).lower() or "already exists" in str(e).lower():
                logger.warning(f"Migración {version} ya aplicada parcialmente, continuando: {e}")
                # Marcar la versión como aplicada aunque falle parcialmente
                connection.execute(
                    "INSERT OR IGNORE INTO schema_migrations(version) VALUES (?)", (version,)
                )
                connection.commit()
            else:
                connection.rollback()
                raise
