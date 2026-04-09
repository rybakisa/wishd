from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import CORS_ORIGINS, DATABASE_URL, SUPABASE_JWT_SECRET
from presentation.api.routes import router
from runtime import RuntimeContainer


@asynccontextmanager
async def lifespan(app: FastAPI):
    container = RuntimeContainer(database_url=DATABASE_URL, jwt_secret=SUPABASE_JWT_SECRET)
    app.state.runtime = container
    yield
    container.close()


app = FastAPI(title="Wishlist API", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)


@app.get("/health")
async def health():
    return {"status": "ok"}
