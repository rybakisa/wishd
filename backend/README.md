# Wishlist API (FastAPI)

Python backend for the Wishlist monorepo. SQLite persistence, stub auth, self-hosted URL scraper.

## Setup

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Run

```bash
uvicorn app.main:app --reload --port 4000
```

## Endpoints

- `GET /health`
- `POST /auth/stub` — `{provider: "apple"|"google"|"email", email, displayName?}` → `{token, user}`
- `GET /api/me` — current user (Bearer token)
- `GET/POST /api/wishlists[/:id]` — wishlist CRUD (Bearer token)
- `POST /api/wishlists/:wid/items`, `PATCH /api/wishlists/:wid/items/:iid`, `DELETE …` — item CRUD
- `GET /api/share/:token` — anonymous share read (link/public wishlists only)
- `GET /api/parse-url?url=<url>` — heuristic product scraper (JSON-LD + OpenGraph)

## Environment

- `WISHLIST_JWT_SECRET` — HS256 secret (default: `dev-secret-change-me`)
- `WISHLIST_DB_PATH` — override SQLite path (default: `backend/wishlist.db`)

## Auth

Stub: any email + provider yields a 30-day JWT. Replace `auth_stub` with real Apple/Google
token verification for production.
