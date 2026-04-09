from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict


def _camel(s: str) -> str:
    parts = s.split("_")
    return parts[0] + "".join(p.title() for p in parts[1:])


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=_camel, populate_by_name=True)


class UserResponse(CamelModel):
    id: str
    email: str
    display_name: str
    avatar_url: Optional[str] = None
    provider: Literal["apple", "google", "email"]
