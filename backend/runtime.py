from __future__ import annotations

from application.product.service import ProductService
from application.user.service import UserService
from application.wishlist.service import WishlistService
from domain.user.use_cases import UserUseCases
from domain.wishlist.use_cases import WishlistUseCases
from infra.postgresql.client import PostgreSQLClient
from infra.postgresql.user_repository import UserRepository
from infra.postgresql.wishlist_repository import WishlistRepository
from services.url_parser import parse_product_url


class RuntimeContainer:
    def __init__(self, *, database_url: str, jwt_secret: str) -> None:
        # Infrastructure
        self.db_client = PostgreSQLClient(database_url)
        self.db_client.init_db()

        # Repositories
        self.user_repository = UserRepository(self.db_client)
        self.wishlist_repository = WishlistRepository(self.db_client)

        # Domain use cases
        self.user_use_cases = UserUseCases(self.user_repository)
        self.wishlist_use_cases = WishlistUseCases(self.wishlist_repository)

        # Application services
        self.user_service = UserService(self.user_use_cases, jwt_secret=jwt_secret)
        self.wishlist_service = WishlistService(self.wishlist_use_cases)
        self.product_service = ProductService(parse_product_url)

    def close(self) -> None:
        self.db_client.close()
