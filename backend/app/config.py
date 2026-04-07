"""Centralised configuration loaded from environment variables."""
import os

SUPABASE_URL: str = os.environ.get("SUPABASE_URL", "")
SUPABASE_JWT_SECRET: str = os.environ.get("SUPABASE_JWT_SECRET", "")
SUPABASE_ANON_KEY: str = os.environ.get("SUPABASE_ANON_KEY", "")

# CORS origins (comma-separated). Default allows local dev.
CORS_ORIGINS: list[str] = [
    o.strip()
    for o in os.environ.get("CORS_ORIGINS", "http://localhost:3000,http://10.0.2.2:4000").split(",")
    if o.strip()
]
