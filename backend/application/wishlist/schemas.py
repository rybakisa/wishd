from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, ConfigDict, Field


def _camel(s: str) -> str:
    parts = s.split("_")
    return parts[0] + "".join(p.title() for p in parts[1:])


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=_camel, populate_by_name=True)


# -- Item schemas --

class WishlistItemResponse(CamelModel):
    id: str
    wishlist_id: str
    name: str
    url: Optional[str] = None
    image_url: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    currency: str = "USD"
    size: Optional[str] = None
    comment: Optional[str] = None
    sort_order: int = 0
    created_at: Optional[str] = None


class WishlistItemCreateCommand(CamelModel):
    name: str
    url: Optional[str] = None
    image_url: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    currency: str = "USD"
    size: Optional[str] = None
    comment: Optional[str] = None
    sort_order: int = 0


class WishlistItemUpdateCommand(CamelModel):
    name: Optional[str] = None
    url: Optional[str] = None
    image_url: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    currency: Optional[str] = None
    size: Optional[str] = None
    comment: Optional[str] = None
    sort_order: Optional[int] = None


# -- Wishlist schemas --

class WishlistResponse(CamelModel):
    id: str
    owner_id: str
    name: str
    cover_type: Literal["none", "emoji", "image"] = "none"
    cover_value: Optional[str] = None
    access: Literal["link", "public", "private"] = "link"
    share_token: str
    created_at: Optional[str] = None
    items: list[WishlistItemResponse] = Field(default_factory=list)


class WishlistCreateCommand(CamelModel):
    name: str
    cover_type: Literal["none", "emoji", "image"] = "none"
    cover_value: Optional[str] = None
    access: Literal["link", "public", "private"] = "link"


class WishlistUpdateCommand(CamelModel):
    name: Optional[str] = None
    cover_type: Optional[Literal["none", "emoji", "image"]] = None
    cover_value: Optional[str] = None
    access: Optional[Literal["link", "public", "private"]] = None
