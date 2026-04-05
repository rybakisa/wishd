# Wishlist

Monorepo for the Wishlist app: Python backend + mobile clients.

## Layout

```
.
├── backend/     # Python FastAPI service
├── clients/     # Mobile clients (Kotlin Multiplatform)
│   ├── android/ # Android app
│   ├── ios/     # iOS app
│   └── shared/  # KMP shared module
├── supabase/    # Database schema & migrations
└── .github/     # CI/CD workflows
```

## Backend (Python / FastAPI)

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 4000
```

Endpoints:
- `GET /health`
- `GET /api/parse-url?url=<product-url>`

See [backend/README.md](backend/README.md) for details.

## Clients (Kotlin Multiplatform)

Gradle root lives in `clients/`.

```bash
cd clients
./gradlew :android:assembleDebug   # Android
```

For iOS, open `clients/ios/iosApp` in Xcode. See [clients/ios/XCODE_SETUP.md](clients/ios/XCODE_SETUP.md).

## Supabase

Schema and migrations under `supabase/`.
