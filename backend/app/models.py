"""Request/response DTOs. Snake_case in DB, camelCase on the wire for KMP clients."""
from typing import List, Optional, Literal

from pydantic import BaseModel, ConfigDict, Field

CoverType = Literal["none", "emoji", "image"]
Access = Literal["link", "public", "private"]
Provider = Literal["apple", "google", "email"]


def _camel(s: str) -> str:
    parts = s.split("_")
    return parts[0] + "".join(p.title() for p in parts[1:])


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=_camel, populate_by_name=True)


class User(CamelModel):
    id: str
    email: str
    display_name: str
    avatar_url: Optional[str] = None
    provider: Provider


class WishlistItem(CamelModel):
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


class WishlistItemCreate(CamelModel):
    name: str
    url: Optional[str] = None
    image_url: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    currency: str = "USD"
    size: Optional[str] = None
    comment: Optional[str] = None
    sort_order: int = 0


class WishlistItemUpdate(CamelModel):
    name: Optional[str] = None
    url: Optional[str] = None
    image_url: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    currency: Optional[str] = None
    size: Optional[str] = None
    comment: Optional[str] = None
    sort_order: Optional[int] = None


class Wishlist(CamelModel):
    id: str
    owner_id: str
    name: str
    cover_type: CoverType = "none"
    cover_value: Optional[str] = None
    access: Access = "link"
    share_token: str
    created_at: Optional[str] = None
    items: List[WishlistItem] = Field(default_factory=list)


class WishlistCreate(CamelModel):
    name: str
    cover_type: CoverType = "none"
    cover_value: Optional[str] = None
    access: Access = "link"


class WishlistUpdate(CamelModel):
    name: Optional[str] = None
    cover_type: Optional[CoverType] = None
    cover_value: Optional[str] = None
    access: Optional[Access] = None


class ParsedProduct(CamelModel):
    name: str
    description: Optional[str] = None
    image_url: Optional[str] = None
    price: Optional[float] = None
    currency: Optional[str] = None
    url: str
