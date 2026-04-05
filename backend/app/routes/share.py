"""Anonymous share endpoint — fetch wishlist by share token."""
from fastapi import APIRouter, HTTPException

from app.db import connect
from app.models import Wishlist
from app.routes.wishlists import _load_items, _row_to_wishlist

router = APIRouter(prefix="/api/share")


@router.get("/{token}", response_model=Wishlist)
def get_shared(token: str) -> Wishlist:
    with connect() as conn:
        row = conn.execute("SELECT * FROM wishlists WHERE share_token = ?", (token,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Not found")
        if row["access"] == "private":
            raise HTTPException(status_code=403, detail="Wishlist is private")
        return _row_to_wishlist(row, _load_items(conn, row["id"]))
