"""Tests for WishlistService using in-memory doubles."""
from __future__ import annotations

from typing import Optional

import pytest
from fastapi import HTTPException

from application.wishlist.schemas import WishlistCreateCommand, WishlistItemCreateCommand
from application.wishlist.service import WishlistService
from domain.wishlist.entities import Wishlist, WishlistItem
from domain.wishlist.use_cases import WishlistUseCases


class InMemoryWishlistRepository:
    def __init__(self) -> None:
        self.wishlists: dict[str, Wishlist] = {}
        self.items: dict[str, WishlistItem] = {}

    def list_for_user(self, *, owner_id: str) -> list[Wishlist]:
        return [w for w in self.wishlists.values() if w.owner_id == owner_id]

    def get(self, *, wishlist_id: str) -> Optional[Wishlist]:
        w = self.wishlists.get(wishlist_id)
        if w:
            w = w.model_copy()
            w.items = [i for i in self.items.values() if i.wishlist_id == wishlist_id]
        return w

    def get_by_share_token(self, *, token: str) -> Optional[Wishlist]:
        for w in self.wishlists.values():
            if w.share_token == token:
                copy = w.model_copy()
                copy.items = [i for i in self.items.values() if i.wishlist_id == w.id]
                return copy
        return None

    def create(self, wishlist: Wishlist) -> Wishlist:
        self.wishlists[wishlist.id] = wishlist
        return wishlist

    def update(self, *, wishlist_id: str, fields: dict) -> Optional[Wishlist]:
        w = self.wishlists.get(wishlist_id)
        if not w:
            return None
        for k, v in fields.items():
            setattr(w, k, v)
        return self.get(wishlist_id=wishlist_id)

    def delete(self, *, wishlist_id: str) -> None:
        self.wishlists.pop(wishlist_id, None)
        to_remove = [k for k, v in self.items.items() if v.wishlist_id == wishlist_id]
        for k in to_remove:
            del self.items[k]

    def create_item(self, item: WishlistItem) -> WishlistItem:
        self.items[item.id] = item
        return item

    def update_item(self, *, item_id: str, wishlist_id: str, fields: dict) -> Optional[WishlistItem]:
        item = self.items.get(item_id)
        if not item or item.wishlist_id != wishlist_id:
            return None
        for k, v in fields.items():
            setattr(item, k, v)
        return item

    def delete_item(self, *, item_id: str, wishlist_id: str) -> None:
        self.items.pop(item_id, None)


def build_service() -> tuple[WishlistService, InMemoryWishlistRepository]:
    repo = InMemoryWishlistRepository()
    use_cases = WishlistUseCases(repo)
    service = WishlistService(use_cases)
    return service, repo


def test_create_wishlist():
    service, repo = build_service()
    result = service.create(
        user_id="user-1",
        command=WishlistCreateCommand(name="Birthday"),
    )
    assert result.name == "Birthday"
    assert result.owner_id == "user-1"
    assert len(repo.wishlists) == 1


def test_list_filters_by_user():
    service, _ = build_service()
    service.create(user_id="user-1", command=WishlistCreateCommand(name="A"))
    service.create(user_id="user-2", command=WishlistCreateCommand(name="B"))
    results = service.list_for_user("user-1")
    assert len(results) == 1
    assert results[0].name == "A"


def test_get_nonexistent_raises_404():
    service, _ = build_service()
    with pytest.raises(HTTPException) as exc:
        service.get(wishlist_id="nope", user_id="user-1")
    assert exc.value.status_code == 404


def test_delete_not_owner_raises_403():
    service, _ = build_service()
    created = service.create(user_id="user-1", command=WishlistCreateCommand(name="Mine"))
    with pytest.raises(HTTPException) as exc:
        service.delete(wishlist_id=created.id, user_id="user-2")
    assert exc.value.status_code == 403


def test_create_and_list_items():
    service, _ = build_service()
    wl = service.create(user_id="user-1", command=WishlistCreateCommand(name="Gifts"))
    item = service.create_item(
        wishlist_id=wl.id,
        user_id="user-1",
        command=WishlistItemCreateCommand(name="Book"),
    )
    assert item.name == "Book"
    fetched = service.get(wishlist_id=wl.id, user_id="user-1")
    assert len(fetched.items) == 1


def test_shared_wishlist_private_raises_403():
    service, _ = build_service()
    wl = service.create(
        user_id="user-1",
        command=WishlistCreateCommand(name="Private", access="private"),
    )
    with pytest.raises(HTTPException) as exc:
        service.get_shared(token=wl.share_token)
    assert exc.value.status_code == 403
