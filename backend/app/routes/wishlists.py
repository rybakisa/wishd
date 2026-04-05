"""Wishlist + item CRUD (authenticated)."""
import uuid
from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException

from app.auth import get_current_user
from app.db import connect
from app.models import (
    User,
    Wishlist,
    WishlistCreate,
    WishlistItem,
    WishlistItemCreate,
    WishlistItemUpdate,
    WishlistUpdate,
)

router = APIRouter(prefix="/api/wishlists")


def _row_to_wishlist(row, items: List[WishlistItem]) -> Wishlist:
    return Wishlist(
        id=row["id"],
        owner_id=row["owner_id"],
        name=row["name"],
        cover_type=row["cover_type"],
        cover_value=row["cover_value"],
        access=row["access"],
        share_token=row["share_token"],
        created_at=row["created_at"],
        items=items,
    )


def _row_to_item(row) -> WishlistItem:
    return WishlistItem(
        id=row["id"],
        wishlist_id=row["wishlist_id"],
        name=row["name"],
        url=row["url"],
        image_url=row["image_url"],
        description=row["description"],
        price=row["price"],
        currency=row["currency"],
        size=row["size"],
        comment=row["comment"],
        sort_order=row["sort_order"],
        created_at=row["created_at"],
    )


def _load_items(conn, wid: str) -> List[WishlistItem]:
    rows = conn.execute(
        "SELECT * FROM wishlist_items WHERE wishlist_id = ? ORDER BY sort_order, created_at",
        (wid,),
    ).fetchall()
    return [_row_to_item(r) for r in rows]


def _require_owner(conn, wid: str, user: User) -> dict:
    row = conn.execute("SELECT * FROM wishlists WHERE id = ?", (wid,)).fetchone()
    if not row:
        raise HTTPException(status_code=404, detail="Wishlist not found")
    if row["owner_id"] != user.id:
        raise HTTPException(status_code=403, detail="Not the owner")
    return row


@router.get("", response_model=List[Wishlist])
def list_wishlists(user: User = Depends(get_current_user)) -> List[Wishlist]:
    with connect() as conn:
        rows = conn.execute(
            "SELECT * FROM wishlists WHERE owner_id = ? ORDER BY created_at DESC",
            (user.id,),
        ).fetchall()
        return [_row_to_wishlist(r, _load_items(conn, r["id"])) for r in rows]


@router.post("", response_model=Wishlist)
def create_wishlist(payload: WishlistCreate, user: User = Depends(get_current_user)) -> Wishlist:
    wid = str(uuid.uuid4())
    token = str(uuid.uuid4())
    with connect() as conn:
        conn.execute(
            """INSERT INTO wishlists(id, owner_id, name, cover_type, cover_value, access, share_token)
               VALUES (?,?,?,?,?,?,?)""",
            (wid, user.id, payload.name, payload.cover_type, payload.cover_value, payload.access, token),
        )
        row = conn.execute("SELECT * FROM wishlists WHERE id = ?", (wid,)).fetchone()
        return _row_to_wishlist(row, [])


@router.get("/{wid}", response_model=Wishlist)
def get_wishlist(wid: str, user: User = Depends(get_current_user)) -> Wishlist:
    with connect() as conn:
        row = conn.execute("SELECT * FROM wishlists WHERE id = ?", (wid,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Wishlist not found")
        if row["owner_id"] != user.id and row["access"] not in ("public",):
            raise HTTPException(status_code=403, detail="Forbidden")
        return _row_to_wishlist(row, _load_items(conn, wid))


@router.patch("/{wid}", response_model=Wishlist)
def update_wishlist(
    wid: str, payload: WishlistUpdate, user: User = Depends(get_current_user)
) -> Wishlist:
    with connect() as conn:
        _require_owner(conn, wid, user)
        updates = []
        params: List[Optional[str]] = []
        for field in ("name", "cover_type", "cover_value", "access"):
            val = getattr(payload, field)
            if val is not None:
                updates.append(f"{field} = ?")
                params.append(val)
        if updates:
            params.append(wid)
            conn.execute(f"UPDATE wishlists SET {', '.join(updates)} WHERE id = ?", params)
        row = conn.execute("SELECT * FROM wishlists WHERE id = ?", (wid,)).fetchone()
        return _row_to_wishlist(row, _load_items(conn, wid))


@router.delete("/{wid}")
def delete_wishlist(wid: str, user: User = Depends(get_current_user)):
    with connect() as conn:
        _require_owner(conn, wid, user)
        conn.execute("DELETE FROM wishlists WHERE id = ?", (wid,))
    return {"status": "ok"}


@router.post("/{wid}/items", response_model=WishlistItem)
def create_item(
    wid: str, payload: WishlistItemCreate, user: User = Depends(get_current_user)
) -> WishlistItem:
    with connect() as conn:
        _require_owner(conn, wid, user)
        iid = str(uuid.uuid4())
        conn.execute(
            """INSERT INTO wishlist_items
               (id, wishlist_id, name, url, image_url, description, price, currency, size, comment, sort_order)
               VALUES (?,?,?,?,?,?,?,?,?,?,?)""",
            (
                iid, wid, payload.name, payload.url, payload.image_url, payload.description,
                payload.price, payload.currency, payload.size, payload.comment, payload.sort_order,
            ),
        )
        row = conn.execute("SELECT * FROM wishlist_items WHERE id = ?", (iid,)).fetchone()
        return _row_to_item(row)


@router.patch("/{wid}/items/{iid}", response_model=WishlistItem)
def update_item(
    wid: str, iid: str, payload: WishlistItemUpdate, user: User = Depends(get_current_user)
) -> WishlistItem:
    with connect() as conn:
        _require_owner(conn, wid, user)
        updates = []
        params: List[object] = []
        for field in ("name", "url", "image_url", "description", "price",
                      "currency", "size", "comment", "sort_order"):
            val = getattr(payload, field)
            if val is not None:
                updates.append(f"{field} = ?")
                params.append(val)
        if updates:
            params.extend([iid, wid])
            conn.execute(
                f"UPDATE wishlist_items SET {', '.join(updates)} WHERE id = ? AND wishlist_id = ?",
                params,
            )
        row = conn.execute("SELECT * FROM wishlist_items WHERE id = ?", (iid,)).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Item not found")
        return _row_to_item(row)


@router.delete("/{wid}/items/{iid}")
def delete_item(wid: str, iid: str, user: User = Depends(get_current_user)):
    with connect() as conn:
        _require_owner(conn, wid, user)
        conn.execute("DELETE FROM wishlist_items WHERE id = ? AND wishlist_id = ?", (iid, wid))
    return {"status": "ok"}
