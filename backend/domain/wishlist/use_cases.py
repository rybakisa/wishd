from __future__ import annotations

from typing import TYPE_CHECKING, Optional

if TYPE_CHECKING:
    from domain.wishlist.repositories import WishlistRepository

from domain.wishlist.entities import Wishlist, WishlistItem


class WishlistUseCases:
    def __init__(self, repository: WishlistRepository) -> None:
        self.repository = repository

    def list_for_user(self, *, owner_id: str) -> list[Wishlist]:
        return self.repository.list_for_user(owner_id=owner_id)

    def get(self, *, wishlist_id: str) -> Optional[Wishlist]:
        return self.repository.get(wishlist_id=wishlist_id)

    def get_by_share_token(self, *, token: str) -> Optional[Wishlist]:
        return self.repository.get_by_share_token(token=token)

    def create(self, wishlist: Wishlist) -> Wishlist:
        return self.repository.create(wishlist)

    def update(self, *, wishlist_id: str, owner_id: str, fields: dict) -> Optional[Wishlist]:
        wishlist = self.repository.get(wishlist_id=wishlist_id)
        if wishlist is None:
            return None
        if wishlist.owner_id != owner_id:
            raise PermissionError("Not the owner")
        return self.repository.update(wishlist_id=wishlist_id, fields=fields)

    def delete(self, *, wishlist_id: str, owner_id: str) -> bool:
        wishlist = self.repository.get(wishlist_id=wishlist_id)
        if wishlist is None:
            return False
        if wishlist.owner_id != owner_id:
            raise PermissionError("Not the owner")
        self.repository.delete(wishlist_id=wishlist_id)
        return True

    def require_owner(self, *, wishlist_id: str, owner_id: str) -> Wishlist:
        wishlist = self.repository.get(wishlist_id=wishlist_id)
        if wishlist is None:
            raise LookupError("Wishlist not found")
        if wishlist.owner_id != owner_id:
            raise PermissionError("Not the owner")
        return wishlist

    # -- Item operations --

    def create_item(self, *, owner_id: str, item: WishlistItem) -> WishlistItem:
        self.require_owner(wishlist_id=item.wishlist_id, owner_id=owner_id)
        return self.repository.create_item(item)

    def update_item(
        self, *, owner_id: str, wishlist_id: str, item_id: str, fields: dict
    ) -> Optional[WishlistItem]:
        self.require_owner(wishlist_id=wishlist_id, owner_id=owner_id)
        return self.repository.update_item(item_id=item_id, wishlist_id=wishlist_id, fields=fields)

    def delete_item(self, *, owner_id: str, wishlist_id: str, item_id: str) -> None:
        self.require_owner(wishlist_id=wishlist_id, owner_id=owner_id)
        self.repository.delete_item(item_id=item_id, wishlist_id=wishlist_id)
