from __future__ import annotations

from fastapi import APIRouter, Depends

from application.wishlist.schemas import WishlistResponse
from presentation.api.runtime import get_runtime

router = APIRouter(prefix="/api/share")


@router.get("/{token}", response_model=WishlistResponse)
def get_shared(
    token: str,
    runtime=Depends(get_runtime),
) -> WishlistResponse:
    return runtime.wishlist_service.get_shared(token=token)
