from __future__ import annotations

from fastapi import APIRouter

from presentation.api.product.controllers import router as product_router
from presentation.api.share.controllers import router as share_router
from presentation.api.user.controllers import router as user_router
from presentation.api.wishlist.controllers import router as wishlist_router

router = APIRouter()
router.include_router(user_router)
router.include_router(wishlist_router)
router.include_router(share_router)
router.include_router(product_router)
