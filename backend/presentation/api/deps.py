from __future__ import annotations

from typing import Optional

from fastapi import Depends, Header, HTTPException, status

from application.user.schemas import UserResponse
from presentation.api.runtime import get_runtime


def _extract_bearer(authorization: Optional[str]) -> Optional[str]:
    if not authorization:
        return None
    parts = authorization.split(" ", 1)
    if len(parts) != 2 or parts[0].lower() != "bearer":
        return None
    return parts[1].strip() or None


def get_current_user(
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
    user_id = runtime.user_service.decode_token(token)
    return runtime.user_service.get_current_user(user_id)


def get_current_user_optional(
    authorization: Optional[str] = Header(default=None),
    runtime=Depends(get_runtime),
) -> Optional[UserResponse]:
    token = _extract_bearer(authorization)
    if not token:
        return None
    try:
        user_id = runtime.user_service.decode_token(token)
    except HTTPException:
        return None
    entity = runtime.user_service.get_current_user_entity(user_id)
    if not entity:
        return None
    from application.user.service import UserService
    return UserService._to_response(entity)
