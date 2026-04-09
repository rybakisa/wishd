"""Centralised configuration loaded from environment variables."""
import os

DATABASE_URL: str = os.environ.get(
    "DATABASE_URL",
    "postgresql://wishlist:wishlist@localhost:5432/wishlist",
)

SUPABASE_JWT_SECRET: str = os.environ.get("SUPABASE_JWT_SECRET", "")

# CORS origins (comma-separated). Default allows local dev.
CORS_ORIGINS: list[str] = [
    o.strip()
    for o in os.environ.get("CORS_ORIGINS", "http://localhost:3000,http://10.0.2.2:4000").split(",")
    if o.strip()
]
