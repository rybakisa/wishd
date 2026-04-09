from __future__ import annotations

from typing import List

from fastapi import APIRouter, Depends

from application.user.schemas import UserResponse
from application.wishlist.schemas import (
    WishlistCreateCommand,
    WishlistItemCreateCommand,
    WishlistItemResponse,
    WishlistItemUpdateCommand,
    WishlistResponse,
    WishlistUpdateCommand,
)
from presentation.api.deps import get_current_user
from presentation.api.runtime import get_runtime

router = APIRouter(prefix="/api/wishlists")


@router.get("", response_model=List[WishlistResponse])
def list_wishlists(
    user: UserResponse = Depends(get_current_user),
    runtime=Depends(get_runtime),
) -> List[WishlistResponse]:
    return runtime.wishlist_service.list_for_user(user.id)


@router.post("", response_model=WishlistResponse)
def create_wishlist(
    payload: WishlistCreateCommand,
    user: UserResponse = Depends(get_current_user),
    runtime=Depends(get_runtime),
) -> WishlistResponse:
    return runtime.wishlist_service.create(user_id=user.id, command=payload)


@router.get("/{wid}", response_model=WishlistResponse)
def get_wishlist(
    wid: str,
    user: UserResponse = Depends(get_current_user),
    runtime=Depends(get_runtime),
) -> WishlistResponse:
    return runtime.wishlist_service.get(wishlist_id=wid, user_id=user.id)


@router.patch("/{wid}", response_model=WishlistResponse)
def update_wishlist(
    wid: str,
    payload: WishlistUpdateCommand,
    user: UserResponse = Depends(get_current_user),
    runtime=Depends(get_runtime),
) -> WishlistResponse:
    return runtime.wishlist_service.update(
        wishlist_id=wid, user_id=user.id, command=payload,
    )


@router.delete("/{wid}")
def delete_wishlist(
    wid: str,
    user: UserResponse = Depends(get_current_user),
    runtime=Depends(get_runtime),
):
    return runtime.wishlist_service.delete(wishlist_id=wid, user_id=user.id)


@router.post("/{wid}/items", response_model=WishlistItemResponse)
def create_item(
    wid: str,
    payload: WishlistItemCreateCommand,
    user: UserResponse = Depends(get_current_user),
    runtime=Depends(get_runtime),
) -> WishlistItemResponse:
    return runtime.wishlist_service.create_item(
        wishlist_id=wid, user_id=user.id, command=payload,
    )


@router.patch("/{wid}/items/{iid}", response_model=WishlistItemResponse)
def update_item(
    wid: str,
    iid: str,
    payload: WishlistItemUpdateCommand,
    user: UserResponse = Depends(get_current_user),
    runtime=Depends(get_runtime),
) -> WishlistItemResponse:
    return runtime.wishlist_service.update_item(
        wishlist_id=wid, item_id=iid, user_id=user.id, command=payload,
    )


@router.delete("/{wid}/items/{iid}")
def delete_item(
    wid: str,
    iid: str,
    user: UserResponse = Depends(get_current_user),
    runtime=Depends(get_runtime),
):
    return runtime.wishlist_service.delete_item(
        wishlist_id=wid, item_id=iid, user_id=user.id,
    )
