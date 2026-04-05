"""SQLite persistence. Stdlib sqlite3 is fine for the MVP."""
import os
import sqlite3
from contextlib import contextmanager
from pathlib import Path

DB_PATH = Path(os.environ.get("WISHLIST_DB_PATH", Path(__file__).parent.parent / "wishlist.db"))

SCHEMA = """
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    display_name TEXT,
    avatar_url TEXT,
    provider TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS wishlists (
    id TEXT PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    cover_type TEXT NOT NULL DEFAULT 'none',
    cover_value TEXT,
    access TEXT NOT NULL DEFAULT 'link',
    share_token TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS wishlist_items (
    id TEXT PRIMARY KEY,
    wishlist_id TEXT NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    url TEXT,
    image_url TEXT,
    description TEXT,
    price REAL,
    currency TEXT NOT NULL DEFAULT 'USD',
    size TEXT,
    comment TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_wishlists_owner ON wishlists(owner_id);
CREATE INDEX IF NOT EXISTS idx_wishlists_share ON wishlists(share_token);
CREATE INDEX IF NOT EXISTS idx_items_wishlist ON wishlist_items(wishlist_id);
"""


def init_db() -> None:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    with connect() as conn:
        conn.executescript(SCHEMA)


@contextmanager
def connect():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
