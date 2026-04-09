from __future__ import annotations

import logging
from typing import Optional

import jwt
from fastapi import HTTPException, status

from application.user.schemas import UserResponse
from domain.user.entities import User
from domain.user.use_cases import UserUseCases

logger = logging.getLogger(__name__)

JWT_ALGO = "HS256"


class UserService:
    def __init__(self, user_use_cases: UserUseCases, *, jwt_secret: str) -> None:
        self.user_use_cases = user_use_cases
        self.jwt_secret = jwt_secret

    def decode_supabase_token(self, token: str) -> dict:
        payload = jwt.decode(
            token, self.jwt_secret, algorithms=[JWT_ALGO], audience="authenticated",
        )
        sub = payload.get("sub")
        if not isinstance(sub, str):
            raise jwt.InvalidTokenError("missing sub")
        return payload

    def decode_token(self, token: str) -> str:
        try:
            payload = self.decode_supabase_token(token)
            return payload["sub"]
        except jwt.PyJWTError as err:
            raise HTTPException(
                status_code=401,
                detail=f"Invalid token: {err}",
                headers={"WWW-Authenticate": "Bearer"},
            )

    def sync_session(self, token: str) -> UserResponse:
        try:
            supabase_payload = self.decode_supabase_token(token)
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

        user = self.user_use_cases.upsert(
            provider=provider,
            email=email,
            display_name=display_name,
            avatar_url=avatar_url,
            user_id=sub,
        )
        return self._to_response(user)

    def get_current_user(self, user_id: str) -> UserResponse:
        user = self.user_use_cases.get(user_id=user_id)
        if not user:
            raise HTTPException(
                status_code=401,
                detail="User not found",
                headers={"WWW-Authenticate": "Bearer"},
            )
        return self._to_response(user)

    def get_current_user_entity(self, user_id: str) -> Optional[User]:
        return self.user_use_cases.get(user_id=user_id)

    @staticmethod
    def _to_response(user: User) -> UserResponse:
        return UserResponse(
            id=user.id,
            email=user.email,
            display_name=user.display_name,
            avatar_url=user.avatar_url,
            provider=user.provider,
        )
