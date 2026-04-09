from __future__ import annotations

from urllib.parse import urlparse

from fastapi import HTTPException

from application.product.schemas import ParsedProductResponse


def normalise_url(url: str) -> str:
    url = url.strip()
    if not url:
        return url
    parsed = urlparse(url)
    if not parsed.scheme:
        url = "https://" + url
    return url


def is_valid_url(url: str) -> bool:
    try:
        parsed = urlparse(url)
        return bool(parsed.scheme and parsed.netloc and "." in parsed.netloc)
    except Exception:
        return False


class ProductService:
    def __init__(self, parse_product_url_fn) -> None:
        self._parse_product_url = parse_product_url_fn

    async def parse_url(self, url: str) -> ParsedProductResponse:
        url = normalise_url(url)
        if not is_valid_url(url):
            raise HTTPException(status_code=400, detail="Invalid URL")
        try:
            product = await self._parse_product_url(url)
        except Exception as err:
            raise HTTPException(
                status_code=502,
                detail={"error": "Could not parse product URL", "details": str(err)},
            )
        return ParsedProductResponse(
            name=product.name,
            description=product.description,
            image_url=product.image_url,
            price=product.price,
            currency=product.currency,
            url=product.url,
        )
