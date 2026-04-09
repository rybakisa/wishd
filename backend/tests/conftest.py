import os
import uuid

import psycopg
import pytest

# Base DSN for test database management (connects to default 'wishlist' db to create/drop test dbs)
_BASE_DSN = os.environ.get(
    "DATABASE_URL",
    "postgresql://wishlist:wishlist@localhost:5432/wishlist",
)


@pytest.fixture
def pg_dsn():
    """Create an isolated test database, yield its DSN, then drop it."""
    test_db = f"wishlist_test_{uuid.uuid4().hex[:8]}"

    with psycopg.connect(_BASE_DSN, autocommit=True) as conn:
        conn.execute(f"CREATE DATABASE {test_db}")

    # Build DSN pointing to the test database
    parts = _BASE_DSN.rsplit("/", 1)
    dsn = f"{parts[0]}/{test_db}"

    yield dsn

    with psycopg.connect(_BASE_DSN, autocommit=True) as conn:
        conn.execute(f"DROP DATABASE {test_db} WITH (FORCE)")


@pytest.fixture
def html_page():
    """Build an HTML page with configurable sections."""

    def _build(
        *,
        title: str | None = None,
        meta_tags: list[tuple[str, str, str]] | None = None,
        jsonld: str | None = None,
        body: str = "",
    ) -> str:
        parts = ["<html><head>"]
        if title:
            parts.append(f"<title>{title}</title>")
        for attr, name, content in meta_tags or []:
            parts.append(f'<meta {attr}="{name}" content="{content}">')
        if jsonld:
            parts.append(
                f'<script type="application/ld+json">{jsonld}</script>'
            )
        parts.append(f"</head><body>{body}</body></html>")
        return "".join(parts)

    return _build
