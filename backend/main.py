from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import get_settings
from presentation.api.routes import router
from runtime import RuntimeContainer


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    container = RuntimeContainer(
        database_url=settings.database_url,
        jwks_url=settings.jwks_url,
        jwt_secret=settings.supabase_jwt_secret,
    )
    app.state.runtime = container
    yield
    container.close()


app = FastAPI(title="Wishlist API", version="1.0.0", lifespan=lifespan)

settings = get_settings()
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origin_list,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)


@app.get("/health")
async def health():
    return {"status": "ok"}
