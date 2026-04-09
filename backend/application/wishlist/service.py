from __future__ import annotations

from fastapi import HTTPException, status

from application.wishlist.schemas import (
    WishlistCreateCommand,
    WishlistItemCreateCommand,
    WishlistItemResponse,
    WishlistItemUpdateCommand,
    WishlistResponse,
    WishlistUpdateCommand,
)
from domain.wishlist.entities import Wishlist, WishlistItem
from domain.wishlist.use_cases import WishlistUseCases


class WishlistService:
    def __init__(self, wishlist_use_cases: WishlistUseCases) -> None:
        self.wishlist_use_cases = wishlist_use_cases

    def list_for_user(self, user_id: str) -> list[WishlistResponse]:
        wishlists = self.wishlist_use_cases.list_for_user(owner_id=user_id)
        return [self._to_response(w) for w in wishlists]

    def get(self, *, wishlist_id: str, user_id: str) -> WishlistResponse:
        wishlist = self.wishlist_use_cases.get(wishlist_id=wishlist_id)
        if wishlist is None:
            raise HTTPException(status_code=404, detail="Wishlist not found")
        if wishlist.owner_id != user_id and wishlist.access not in ("public",):
            raise HTTPException(status_code=403, detail="Forbidden")
        return self._to_response(wishlist)

    def create(self, *, user_id: str, command: WishlistCreateCommand) -> WishlistResponse:
        entity = Wishlist(
            owner_id=user_id,
            name=command.name,
            cover_type=command.cover_type,
            cover_value=command.cover_value,
            access=command.access,
        )
        created = self.wishlist_use_cases.create(entity)
        return self._to_response(created)

    def update(
        self, *, wishlist_id: str, user_id: str, command: WishlistUpdateCommand,
    ) -> WishlistResponse:
        fields = {}
        for field in ("name", "cover_type", "cover_value", "access"):
            val = getattr(command, field)
            if val is not None:
                fields[field] = val
        try:
            updated = self.wishlist_use_cases.update(
                wishlist_id=wishlist_id, owner_id=user_id, fields=fields,
            )
        except LookupError:
            raise HTTPException(status_code=404, detail="Wishlist not found")
        except PermissionError:
            raise HTTPException(status_code=403, detail="Not the owner")
        if updated is None:
            raise HTTPException(status_code=404, detail="Wishlist not found")
        return self._to_response(updated)

    def delete(self, *, wishlist_id: str, user_id: str) -> dict:
        try:
            found = self.wishlist_use_cases.delete(wishlist_id=wishlist_id, owner_id=user_id)
        except PermissionError:
            raise HTTPException(status_code=403, detail="Not the owner")
        if not found:
            raise HTTPException(status_code=404, detail="Wishlist not found")
        return {"status": "ok"}

    def get_shared(self, *, token: str) -> WishlistResponse:
        wishlist = self.wishlist_use_cases.get_by_share_token(token=token)
        if wishlist is None:
            raise HTTPException(status_code=404, detail="Not found")
        if wishlist.access == "private":
            raise HTTPException(status_code=403, detail="Wishlist is private")
        return self._to_response(wishlist)

    # -- Item operations --

    def create_item(
        self, *, wishlist_id: str, user_id: str, command: WishlistItemCreateCommand,
    ) -> WishlistItemResponse:
        item = WishlistItem(
            wishlist_id=wishlist_id,
            name=command.name,
            url=command.url,
            image_url=command.image_url,
            description=command.description,
            price=command.price,
            currency=command.currency,
            size=command.size,
            comment=command.comment,
            sort_order=command.sort_order,
        )
        try:
            created = self.wishlist_use_cases.create_item(owner_id=user_id, item=item)
        except LookupError:
            raise HTTPException(status_code=404, detail="Wishlist not found")
        except PermissionError:
            raise HTTPException(status_code=403, detail="Not the owner")
        return self._item_to_response(created)

    def update_item(
        self,
        *,
        wishlist_id: str,
        item_id: str,
        user_id: str,
        command: WishlistItemUpdateCommand,
    ) -> WishlistItemResponse:
        fields = {}
        for field in ("name", "url", "image_url", "description", "price",
                      "currency", "size", "comment", "sort_order"):
            val = getattr(command, field)
            if val is not None:
                fields[field] = val
        try:
            updated = self.wishlist_use_cases.update_item(
                owner_id=user_id, wishlist_id=wishlist_id,
                item_id=item_id, fields=fields,
            )
        except LookupError:
            raise HTTPException(status_code=404, detail="Wishlist not found")
        except PermissionError:
            raise HTTPException(status_code=403, detail="Not the owner")
        if updated is None:
            raise HTTPException(status_code=404, detail="Item not found")
        return self._item_to_response(updated)

    def delete_item(self, *, wishlist_id: str, item_id: str, user_id: str) -> dict:
        try:
            self.wishlist_use_cases.delete_item(
                owner_id=user_id, wishlist_id=wishlist_id, item_id=item_id,
            )
        except LookupError:
            raise HTTPException(status_code=404, detail="Wishlist not found")
        except PermissionError:
            raise HTTPException(status_code=403, detail="Not the owner")
        return {"status": "ok"}

    # -- Mapping --

    @staticmethod
    def _to_response(wishlist: Wishlist) -> WishlistResponse:
        return WishlistResponse(
            id=wishlist.id,
            owner_id=wishlist.owner_id,
            name=wishlist.name,
            cover_type=wishlist.cover_type,
            cover_value=wishlist.cover_value,
            access=wishlist.access,
            share_token=wishlist.share_token,
            created_at=wishlist.created_at,
            items=[WishlistService._item_to_response(i) for i in wishlist.items],
        )

    @staticmethod
    def _item_to_response(item: WishlistItem) -> WishlistItemResponse:
        return WishlistItemResponse(
            id=item.id,
            wishlist_id=item.wishlist_id,
            name=item.name,
            url=item.url,
            image_url=item.image_url,
            description=item.description,
            price=item.price,
            currency=item.currency,
            size=item.size,
            comment=item.comment,
            sort_order=item.sort_order,
            created_at=item.created_at,
        )
