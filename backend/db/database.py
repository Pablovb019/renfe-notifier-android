import sqlite3
import os
import logging
from datetime import datetime, timedelta

logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                    level=logging.DEBUG)
logger = logging.getLogger(__name__)

# Base directory of the repository (two levels up from this file: backend/db/database.py -> backend -> repo root)
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATABASE_PATH = os.getenv('RENFE_DB_PATH', os.path.join(BASE_DIR, 'data', 'renfe_notifier.db'))

def get_connection():
    """Return a SQLite connection with WAL mode and busy timeout."""
    conn = sqlite3.connect(DATABASE_PATH, timeout=10)
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA busy_timeout=5000;")
    conn.execute("PRAGMA foreign_keys=ON;")
    return conn

def init_db():
    """Initialize database schema if not exists."""
    with get_connection() as conn:
        cursor = conn.cursor()
        # Followups table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS followups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userid TEXT NOT NULL,
                origin TEXT NOT NULL,
                destination TEXT NOT NULL,
                travel_date TEXT NOT NULL,
                plaza_h INTEGER NOT NULL DEFAULT 0,
                watch_all INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'active',  -- active, paused, completed, expired
                departure_time TEXT,  -- HH:MM
                arrival_time TEXT,    -- HH:MM
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL,
                last_checked INTEGER,
                available INTEGER NOT NULL DEFAULT 0,
                consult_count INTEGER NOT NULL DEFAULT 0,
                consult_count_24h INTEGER NOT NULL DEFAULT 0
            )
        ''')
        # FCM tokens table
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS fcm_tokens (
                userid TEXT PRIMARY KEY,
                token TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
        ''')
        conn.commit()
        logger.info("Database schema initialized.")

# CRUD operations for followups
def add_followup(userid, origin, destination, travel_date, plaza_h=False, watch_all=False, departure_time=None, arrival_time=None):
    """Insert a new followup and return its ID."""
    now = int(datetime.now().timestamp())
    # Expires after 30 days or at departure time, whichever is sooner
    departure_ts = None
    if departure_time and travel_date:
        try:
            dt = datetime.strptime(f"{travel_date} {departure_time}", "%Y-%m-%d %H:%M")
            departure_ts = int(dt.timestamp())
        except Exception:
            departure_ts = None
    expires_ts = now + 30*24*3600  # 30 days
    if departure_ts:
        expires_ts = min(expires_ts, departure_ts)
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO followups
            (userid, origin, destination, travel_date, plaza_h, watch_all, departure_time, arrival_time,
             created_at, updated_at, expires_at, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', (userid, origin, destination, travel_date, int(plaza_h), int(watch_all),
              departure_time, arrival_time, now, now, expires_ts, 'active'))
        conn.commit()
        return cursor.lastrowid

def get_active_followups():
    """Return list of followups with status='active'."""
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            SELECT * FROM followups WHERE status='active' ORDER BY id
        ''')
        rows = cursor.fetchall()
        # Convert to list of dicts
        columns = [description[0] for description in cursor.description]
        return [dict(zip(columns, row)) for row in rows]

def get_followup_by_id(followup_id):
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT * FROM followups WHERE id=?', (followup_id,))
        row = cursor.fetchone()
        if row:
            columns = [description[0] for description in cursor.description]
            return dict(zip(columns, row))
        return None

def update_followup(followup_id, **kwargs):
    """Update fields of a followup."""
    if not kwargs:
        return
    fields = ', '.join([f"{key}=?" for key in kwargs.keys()])
    values = list(kwargs.values())
    values.append(int(datetime.now().timestamp()))
    values.append(followup_id)
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute(f'UPDATE followups SET {fields}, updated_at=? WHERE id=?', values)
        conn.commit()

def set_followup_available(followup_id, available):
    update_followup(followup_id, available=int(available), last_checked=int(datetime.now().timestamp()))

def increment_consult_count(followup_id):
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('UPDATE followups SET consult_count = consult_count + 1, consult_count_24h = consult_count_24h + 1, updated_at=? WHERE id=?',
                       (int(datetime.now().timestamp()), followup_id))
        conn.commit()

def reset_consult_count_24h():
    """Called daily to reset 24h counter."""
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('UPDATE followups SET consult_count_24h = 0')
        conn.commit()

def set_followup_status(followup_id, status):
    update_followup(followup_id, status=status)

def delete_followup(followup_id):
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('DELETE FROM followups WHERE id=?', (followup_id,))
        conn.commit()

# FCM token operations
def save_fcm_token(userid, token):
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('''
            INSERT OR REPLACE INTO fcm_tokens (userid, token, updated_at)
            VALUES (?, ?, ?)
        ''', (userid, token, int(datetime.now().timestamp())))
        conn.commit()

def get_fcm_token(userid):
    with get_connection() as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT token FROM fcm_tokens WHERE userid=?', (userid,))
        row = cursor.fetchone()
        return row[0] if row else None