from __future__ import annotations

from typing import Optional

from domain.wishlist.entities import Wishlist, WishlistItem
from infra.postgresql.client import PostgreSQLClient


class WishlistRepository:
    def __init__(self, db_client: PostgreSQLClient) -> None:
        self.db_client = db_client

    # -- Wishlist queries --

    def list_for_user(self, *, owner_id: str) -> list[Wishlist]:
        with self.db_client.connect() as conn:
            rows = conn.execute(
                "SELECT * FROM wishlists WHERE owner_id = %s ORDER BY created_at DESC",
                (owner_id,),
            ).fetchall()
            return [self._to_entity(r, self._load_items(conn, str(r["id"]))) for r in rows]

    def get(self, *, wishlist_id: str) -> Optional[Wishlist]:
        with self.db_client.connect() as conn:
            row = conn.execute("SELECT * FROM wishlists WHERE id = %s", (wishlist_id,)).fetchone()
            if not row:
                return None
            return self._to_entity(row, self._load_items(conn, wishlist_id))

    def get_by_share_token(self, *, token: str) -> Optional[Wishlist]:
        with self.db_client.connect() as conn:
            row = conn.execute("SELECT * FROM wishlists WHERE share_token = %s", (token,)).fetchone()
            if not row:
                return None
            return self._to_entity(row, self._load_items(conn, str(row["id"])))

    def create(self, wishlist: Wishlist) -> Wishlist:
        with self.db_client.connect() as conn:
            conn.execute(
                """INSERT INTO wishlists(id, owner_id, name, cover_type, cover_value, access, share_token)
                   VALUES (%s,%s,%s,%s,%s,%s,%s)""",
                (
                    wishlist.id, wishlist.owner_id, wishlist.name,
                    wishlist.cover_type, wishlist.cover_value,
                    wishlist.access, wishlist.share_token,
                ),
            )
            row = conn.execute("SELECT * FROM wishlists WHERE id = %s", (wishlist.id,)).fetchone()
            return self._to_entity(row, [])

    def update(self, *, wishlist_id: str, fields: dict) -> Optional[Wishlist]:
        if not fields:
            return self.get(wishlist_id=wishlist_id)
        with self.db_client.connect() as conn:
            sets = [f"{k} = %s" for k in fields]
            params = list(fields.values()) + [wishlist_id]
            conn.execute(f"UPDATE wishlists SET {', '.join(sets)} WHERE id = %s", params)
            row = conn.execute("SELECT * FROM wishlists WHERE id = %s", (wishlist_id,)).fetchone()
            if not row:
                return None
            return self._to_entity(row, self._load_items(conn, wishlist_id))

    def delete(self, *, wishlist_id: str) -> None:
        with self.db_client.connect() as conn:
            conn.execute("DELETE FROM wishlists WHERE id = %s", (wishlist_id,))

    # -- Item queries --

    def create_item(self, item: WishlistItem) -> WishlistItem:
        with self.db_client.connect() as conn:
            conn.execute(
                """INSERT INTO wishlist_items
                   (id, wishlist_id, name, url, image_url, description, price, currency, size, comment, sort_order)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
                (
                    item.id, item.wishlist_id, item.name, item.url, item.image_url,
                    item.description, item.price, item.currency, item.size,
                    item.comment, item.sort_order,
                ),
            )
            row = conn.execute("SELECT * FROM wishlist_items WHERE id = %s", (item.id,)).fetchone()
            return self._item_to_entity(row)

    def update_item(self, *, item_id: str, wishlist_id: str, fields: dict) -> Optional[WishlistItem]:
        if not fields:
            return self._get_item(item_id=item_id)
        with self.db_client.connect() as conn:
            sets = [f"{k} = %s" for k in fields]
            params = list(fields.values()) + [item_id, wishlist_id]
            conn.execute(
                f"UPDATE wishlist_items SET {', '.join(sets)} WHERE id = %s AND wishlist_id = %s",
                params,
            )
            row = conn.execute("SELECT * FROM wishlist_items WHERE id = %s", (item_id,)).fetchone()
            return self._item_to_entity(row) if row else None

    def delete_item(self, *, item_id: str, wishlist_id: str) -> None:
        with self.db_client.connect() as conn:
            conn.execute(
                "DELETE FROM wishlist_items WHERE id = %s AND wishlist_id = %s",
                (item_id, wishlist_id),
            )

    # -- Mapping --

    def _load_items(self, conn, wishlist_id: str) -> list[WishlistItem]:
        rows = conn.execute(
            "SELECT * FROM wishlist_items WHERE wishlist_id = %s ORDER BY sort_order, created_at",
            (wishlist_id,),
        ).fetchall()
        return [self._item_to_entity(r) for r in rows]

    def _get_item(self, *, item_id: str) -> Optional[WishlistItem]:
        with self.db_client.connect() as conn:
            row = conn.execute("SELECT * FROM wishlist_items WHERE id = %s", (item_id,)).fetchone()
            return self._item_to_entity(row) if row else None

    @staticmethod
    def _to_entity(row: dict, items: list[WishlistItem]) -> Wishlist:
        return Wishlist(
            id=str(row["id"]),
            owner_id=str(row["owner_id"]),
            name=row["name"],
            cover_type=row["cover_type"],
            cover_value=row["cover_value"],
            access=row["access"],
            share_token=str(row["share_token"]),
            created_at=str(row["created_at"]) if row["created_at"] else None,
            items=items,
        )

    @staticmethod
    def _item_to_entity(row: dict) -> WishlistItem:
        return WishlistItem(
            id=str(row["id"]),
            wishlist_id=str(row["wishlist_id"]),
            name=row["name"],
            url=row["url"],
            image_url=row["image_url"],
            description=row["description"],
            price=float(row["price"]) if row["price"] is not None else None,
            currency=row["currency"].strip() if row["currency"] else "USD",
            size=row["size"],
            comment=row["comment"],
            sort_order=row["sort_order"],
            created_at=str(row["created_at"]) if row["created_at"] else None,
        )
