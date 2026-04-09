"""PostgreSQL client with connection pooling and migration support."""
from __future__ import annotations

from contextlib import contextmanager
from pathlib import Path

import psycopg
from psycopg.rows import dict_row
from psycopg_pool import ConnectionPool

MIGRATIONS_DIR = Path(__file__).parent / "migrations"


class PostgreSQLClient:
    def __init__(self, dsn: str) -> None:
        self._pool = ConnectionPool(dsn, min_size=2, max_size=10)

    def init_db(self) -> None:
        for sql_file in sorted(MIGRATIONS_DIR.glob("*.sql")):
            sql = sql_file.read_text()
            with self.connect() as conn:
                conn.execute(sql)

    @contextmanager
    def connect(self):
        with self._pool.connection() as conn:
            conn.row_factory = dict_row
            yield conn

    def close(self) -> None:
        self._pool.close()
