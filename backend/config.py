"""Centralised configuration via Pydantic Settings.

Reads from (in priority order):
  1. Environment variables
  2. ``backend/.env`` file (gitignored — for local dev)

For deployments, set env vars directly (Docker, systemd, etc.).
For local dev, copy ``.env.template`` to ``.env`` and fill in values.
"""
from functools import lru_cache

from pydantic import computed_field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    database_url: str = "postgresql://wishlist:wishlist@localhost:5432/wishlist"

    # Supabase project URL — used to derive the JWKS endpoint.
    supabase_url: str = ""

    # Explicit JWKS URL override (auto-derived from supabase_url when empty).
    supabase_jwks_url: str = ""

    # Legacy HS256 shared secret — still accepted for migration / local dev.
    supabase_jwt_secret: str = ""

    # CORS origins (comma-separated).
    cors_origins: str = "http://localhost:3000,http://10.0.2.2:4000"

    @computed_field  # type: ignore[prop-decorator]
    @property
    def jwks_url(self) -> str:
        if self.supabase_jwks_url:
            return self.supabase_jwks_url
        if self.supabase_url:
            return f"{self.supabase_url.rstrip('/')}/auth/v1/.well-known/jwks.json"
        return ""

    @computed_field  # type: ignore[prop-decorator]
    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
