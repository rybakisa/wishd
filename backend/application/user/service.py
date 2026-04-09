from __future__ import annotations

import logging
import ssl
from typing import Optional

import certifi
import jwt
from jwt import PyJWKClient
from fastapi import HTTPException, status

from application.user.schemas import UserResponse
from domain.user.entities import User
from domain.user.use_cases import UserUseCases

logger = logging.getLogger(__name__)


class UserService:
    """Handles Supabase JWT verification and user session management.

    Uses JWKS (JSON Web Key Set) to verify tokens with the public key
    published by Supabase. Falls back to a shared HS256 secret if
    ``jwks_url`` is not provided (legacy / test mode).
    """

    def __init__(
        self,
        user_use_cases: UserUseCases,
        *,
        jwks_url: str = "",
        jwt_secret: str = "",
    ) -> None:
        self.user_use_cases = user_use_cases
        self._jwt_secret = jwt_secret
        self._jwk_client: PyJWKClient | None = None
        if jwks_url:
            ssl_context = ssl.create_default_context(cafile=certifi.where())
            self._jwk_client = PyJWKClient(
                jwks_url, cache_keys=True, lifespan=600, ssl_context=ssl_context,
            )

    def decode_supabase_token(self, token: str) -> dict:
        """Decode and validate a Supabase JWT. Returns full payload."""
        if self._jwk_client:
            # Asymmetric verification via JWKS (RS256 / ES256)
            signing_key = self._jwk_client.get_signing_key_from_jwt(token)
            header = jwt.get_unverified_header(token)
            payload = jwt.decode(
                token,
                signing_key.key,
                algorithms=[header.get("alg", "RS256")],
                audience="authenticated",
            )
        elif self._jwt_secret:
            # Legacy HS256 shared-secret verification (tests / migration)
            payload = jwt.decode(
                token, self._jwt_secret,
                algorithms=["HS256"],
                audience="authenticated",
            )
        else:
            raise jwt.InvalidTokenError("No JWKS URL or JWT secret configured")

        sub = payload.get("sub")
        if not isinstance(sub, str):
            raise jwt.InvalidTokenError("missing sub")
        return payload

    def decode_token(self, token: str) -> str:
        """Validate a Supabase JWT and return user_id (sub claim)."""
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
