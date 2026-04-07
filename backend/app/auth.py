"""Authentication: validates Supabase-issued JWTs.

Supabase issues JWTs with aud="authenticated" and sub=<user-uuid>.
The backend acts as a resource server — it never issues tokens itself.
"""
import logging
import uuid
from typing import Optional

import jwt
from fastapi import APIRouter, Depends, Header, HTTPException, status

from app.config import SUPABASE_JWT_SECRET
from app.db import connect
from app.models import User

logger = logging.getLogger(__name__)

JWT_ALGO = "HS256"

router = APIRouter()


# ---------------------------------------------------------------------------
# Token helpers
# ---------------------------------------------------------------------------

def _decode_supabase_token(token: str) -> dict:
    """Decode and validate a Supabase-issued JWT. Returns full payload."""
    payload = jwt.decode(token, SUPABASE_JWT_SECRET, algorithms=[JWT_ALGO], audience="authenticated")
    sub = payload.get("sub")
    if not isinstance(sub, str):
        raise jwt.InvalidTokenError("missing sub")
    return payload


def _decode_token(token: str) -> str:
    """Validate a Supabase JWT and return user_id (sub claim)."""
    try:
        payload = _decode_supabase_token(token)
        return payload["sub"]
    except jwt.PyJWTError as err:
        raise HTTPException(
            status_code=401,
            detail=f"Invalid token: {err}",
            headers={"WWW-Authenticate": "Bearer"},
        )


# ---------------------------------------------------------------------------
# User persistence
# ---------------------------------------------------------------------------

def _user_row_to_model(row) -> User:
    return User(
        id=row["id"],
        email=row["email"],
        display_name=row["display_name"] or row["email"].split("@")[0],
        avatar_url=row["avatar_url"],
        provider=row["provider"],
    )


def _upsert_user(
    provider: str,
    email: str,
    display_name: Optional[str],
    avatar_url: Optional[str],
    user_id: Optional[str] = None,
) -> User:
    email = email.strip().lower()
    with connect() as conn:
        existing = conn.execute("SELECT * FROM users WHERE email = ?", (email,)).fetchone()
        if existing:
            return _user_row_to_model(existing)
        uid = user_id or str(uuid.uuid4())
        display = display_name or email.split("@")[0]
        conn.execute(
            "INSERT INTO users(id, email, display_name, avatar_url, provider) VALUES (?,?,?,?,?)",
            (uid, email, display, avatar_url, provider),
        )
        row = conn.execute("SELECT * FROM users WHERE id = ?", (uid,)).fetchone()
        return _user_row_to_model(row)


def _extract_bearer(authorization: Optional[str]) -> Optional[str]:
    if not authorization:
        return None
    parts = authorization.split(" ", 1)
    if len(parts) != 2 or parts[0].lower() != "bearer":
        return None
    return parts[1].strip() or None


# ---------------------------------------------------------------------------
# FastAPI dependencies
# ---------------------------------------------------------------------------

def get_current_user(authorization: Optional[str] = Header(default=None)) -> User:
    token = _extract_bearer(authorization)
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing bearer token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    user_id = _decode_token(token)
    with connect() as conn:
        row = conn.execute("SELECT * FROM users WHERE id = ?", (user_id,)).fetchone()
    if not row:
        raise HTTPException(
            status_code=401,
            detail="User not found",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return _user_row_to_model(row)


def get_current_user_optional(authorization: Optional[str] = Header(default=None)) -> Optional[User]:
    token = _extract_bearer(authorization)
    if not token:
        return None
    try:
        user_id = _decode_token(token)
    except HTTPException:
        return None
    with connect() as conn:
        row = conn.execute("SELECT * FROM users WHERE id = ?", (user_id,)).fetchone()
    return _user_row_to_model(row) if row else None


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@router.post("/auth/session", response_model=User)
def auth_session(authorization: Optional[str] = Header(default=None)) -> User:
    """Sync a Supabase-authenticated user into the local DB.

    The client sends its Supabase access token as a Bearer header.
    We validate it, extract identity claims, and upsert the user.
    """
    token = _extract_bearer(authorization)
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing bearer token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        supabase_payload = _decode_supabase_token(token)
    except jwt.PyJWTError as err:
        raise HTTPException(
            status_code=401,
            detail=f"Invalid token: {err}",
            headers={"WWW-Authenticate": "Bearer"},
        )

    sub = supabase_payload["sub"]
    email = supabase_payload.get("email", "")
    metadata = supabase_payload.get("user_metadata", {})
    display_name = metadata.get("full_name") or metadata.get("name")
    avatar_url = metadata.get("avatar_url") or metadata.get("picture")
    provider = supabase_payload.get("app_metadata", {}).get("provider", "email")
    user = _upsert_user(provider, email, display_name, avatar_url, user_id=sub)

    return user


@router.get("/api/me", response_model=User)
def me(user: User = Depends(get_current_user)) -> User:
    return user
