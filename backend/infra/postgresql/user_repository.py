from __future__ import annotations

from typing import Optional

from domain.user.entities import User
from infra.postgresql.client import PostgreSQLClient


class UserRepository:
    def __init__(self, db_client: PostgreSQLClient) -> None:
        self.db_client = db_client

    def get(self, *, user_id: str) -> Optional[User]:
        with self.db_client.connect() as conn:
            row = conn.execute("SELECT * FROM users WHERE id = %s", (user_id,)).fetchone()
        return self._to_entity(row) if row else None

    def get_by_email(self, *, email: str) -> Optional[User]:
        with self.db_client.connect() as conn:
            row = conn.execute("SELECT * FROM users WHERE email = %s", (email,)).fetchone()
        return self._to_entity(row) if row else None

    def create(self, user: User) -> User:
        with self.db_client.connect() as conn:
            conn.execute(
                "INSERT INTO users(id, email, display_name, avatar_url, provider) VALUES (%s,%s,%s,%s,%s)",
                (user.id, user.email, user.display_name, user.avatar_url, user.provider),
            )
        return user

    @staticmethod
    def _to_entity(row: dict) -> User:
        return User(
            id=str(row["id"]),
            email=row["email"],
            display_name=row["display_name"] or row["email"].split("@")[0],
            avatar_url=row["avatar_url"],
            provider=row["provider"],
        )
