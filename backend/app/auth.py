"""Stub auth: JWT issued for any email+provider.
Real Apple/Google token verification is a TODO — hook it in at `_verify_provider_token`.
"""
import os
import time
import uuid
from typing import Optional

import jwt
from fastapi import APIRouter, Depends, Header, HTTPException, status

from app.db import connect
from app.models import AuthResponse, AuthStubRequest, User

JWT_SECRET = os.environ.get("WISHLIST_JWT_SECRET", "dev-secret-change-me")
JWT_ALGO = "HS256"
JWT_TTL_SECONDS = 60 * 60 * 24 * 30  # 30 days

router = APIRouter()


def _issue_token(user_id: str) -> str:
    now = int(time.time())
    return jwt.encode(
        {"sub": user_id, "iat": now, "exp": now + JWT_TTL_SECONDS},
        JWT_SECRET,
        algorithm=JWT_ALGO,
    )


def _decode_token(token: str) -> str:
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGO])
    except jwt.PyJWTError as err:
        raise HTTPException(status_code=401, detail=f"Invalid token: {err}")
    sub = payload.get("sub")
    if not isinstance(sub, str):
        raise HTTPException(status_code=401, detail="Token missing sub")
    return sub


def _user_row_to_model(row) -> User:
    return User(
        id=row["id"],
        email=row["email"],
        display_name=row["display_name"] or row["email"].split("@")[0],
        avatar_url=row["avatar_url"],
        provider=row["provider"],
    )


def _upsert_user(provider: str, email: str, display_name: Optional[str], avatar_url: Optional[str]) -> User:
    email = email.strip().lower()
    with connect() as conn:
        existing = conn.execute("SELECT * FROM users WHERE email = ?", (email,)).fetchone()
        if existing:
            return _user_row_to_model(existing)
        uid = str(uuid.uuid4())
        display = display_name or email.split("@")[0]
        conn.execute(
            "INSERT INTO users(id, email, display_name, avatar_url, provider) VALUES (?,?,?,?,?)",
            (uid, email, display, avatar_url, provider),
        )
        row = conn.execute("SELECT * FROM users WHERE id = ?", (uid,)).fetchone()
        return _user_row_to_model(row)


@router.post("/auth/stub", response_model=AuthResponse)
def auth_stub(req: AuthStubRequest) -> AuthResponse:
    """Stub login — accepts any provider+email and issues a JWT.
    Replace with real Apple/Google verification in production.
    """
    if "@" not in req.email:
        raise HTTPException(status_code=400, detail="Invalid email")
    user = _upsert_user(req.provider, req.email, req.display_name, req.avatar_url)
    return AuthResponse(token=_issue_token(user.id), user=user)


def _extract_bearer(authorization: Optional[str]) -> Optional[str]:
    if not authorization:
        return None
    parts = authorization.split(" ", 1)
    if len(parts) != 2 or parts[0].lower() != "bearer":
        return None
    return parts[1].strip() or None


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
        raise HTTPException(status_code=401, detail="User not found")
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


@router.get("/api/me", response_model=User)
def me(user: User = Depends(get_current_user)) -> User:
    return user
