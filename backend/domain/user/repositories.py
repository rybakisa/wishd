from __future__ import annotations

from typing import Optional, Protocol

from domain.user.entities import User


class UserRepository(Protocol):
    def get(self, *, user_id: str) -> Optional[User]: ...
    def get_by_email(self, *, email: str) -> Optional[User]: ...
    def create(self, user: User) -> User: ...
