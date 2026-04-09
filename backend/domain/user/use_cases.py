from __future__ import annotations

import uuid
from typing import TYPE_CHECKING, Optional

if TYPE_CHECKING:
    from domain.user.repositories import UserRepository

from domain.user.entities import User


def normalize_email(email: str) -> str:
    return email.strip().lower()


class UserUseCases:
    def __init__(self, repository: UserRepository) -> None:
        self.repository = repository

    def get(self, *, user_id: str) -> Optional[User]:
        return self.repository.get(user_id=user_id)

    def upsert(
        self,
        *,
        provider: str,
        email: str,
        display_name: Optional[str],
        avatar_url: Optional[str],
        user_id: Optional[str] = None,
    ) -> User:
        email = normalize_email(email)
        existing = self.repository.get_by_email(email=email)
        if existing:
            return existing
        uid = user_id or str(uuid.uuid4())
        display = display_name or email.split("@")[0]
        user = User(
            id=uid,
            email=email,
            display_name=display,
            avatar_url=avatar_url,
            provider=provider,
        )
        return self.repository.create(user)
