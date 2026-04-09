from __future__ import annotations

from typing import Literal, Optional
from uuid import uuid4

from pydantic import BaseModel, Field

CoverType = Literal["none", "emoji", "image"]
Access = Literal["link", "public", "private"]


class WishlistItem(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid4()))
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


class Wishlist(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid4()))
    owner_id: str
    name: str
    cover_type: CoverType = "none"
    cover_value: Optional[str] = None
    access: Access = "link"
    share_token: str = Field(default_factory=lambda: str(uuid4()))
    created_at: Optional[str] = None
    items: list[WishlistItem] = Field(default_factory=list)
