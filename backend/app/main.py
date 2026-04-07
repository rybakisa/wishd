from urllib.parse import urlparse

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware

from app.auth import router as auth_router
from app.config import CORS_ORIGINS
from app.db import init_db
from app.parse_url import parse_product_url
from app.routes.share import router as share_router
from app.routes.wishlists import router as wishlists_router

app = FastAPI(title="Wishlist API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def _startup() -> None:
    init_db()


app.include_router(auth_router)
app.include_router(wishlists_router)
app.include_router(share_router)


def _normalise_url(url: str) -> str:
    """Add https:// scheme when the user pastes a bare domain link."""
    url = url.strip()
    if not url:
        return url
    parsed = urlparse(url)
    if not parsed.scheme:
        url = "https://" + url
    return url


def _is_valid_url(url: str) -> bool:
    try:
        parsed = urlparse(url)
        return bool(parsed.scheme and parsed.netloc and "." in parsed.netloc)
    except Exception:
        return False


@app.get("/api/parse-url")
async def parse_url(url: str = Query(..., description="Product URL to parse")):
    url = _normalise_url(url)
    if not _is_valid_url(url):
        raise HTTPException(status_code=400, detail="Invalid URL")
    try:
        product = await parse_product_url(url)
    except Exception as err:
        raise HTTPException(
            status_code=502,
            detail={"error": "Could not parse product URL", "details": str(err)},
        )
    return product.model_dump(by_alias=True)


@app.get("/health")
async def health():
    return {"status": "ok"}
