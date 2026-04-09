from __future__ import annotations

from typing import Optional

from fastapi import APIRouter, Depends, Header, HTTPException, status

from application.user.schemas import UserResponse
from presentation.api.deps import _extract_bearer, get_current_user
from presentation.api.runtime import get_runtime

router = APIRouter()


@router.post("/auth/session", response_model=UserResponse)
def auth_session(
    authorization: Optional[str] = Header(default=None),
    runtime=Depends(get_runtime),
) -> UserResponse:
    token = _extract_bearer(authorization)
    if not token:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing bearer token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return runtime.user_service.sync_session(token)


@router.get("/api/me", response_model=UserResponse)
def me(
    user: UserResponse = Depends(get_current_user),
) -> UserResponse:
    return user
