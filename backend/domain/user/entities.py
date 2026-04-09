from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel

Provider = Literal["apple", "google", "email"]


class User(BaseModel):
    id: str
    email: str
    display_name: str
    avatar_url: Optional[str] = None
    provider: Provider
