# Repository Guidelines

## Scope
- This file applies to the entire repository unless a deeper `AGENTS.md` overrides it.

## Project Layout
- `backend/`: FastAPI backend with PostgreSQL persistence and pytest tests.
- `clients/`: Kotlin Multiplatform mobile project with Android, iOS, and shared modules.
- `.github/`: CI and automation workflows.
- `docker-compose.yml`: Local PostgreSQL instance for development.

## Working Style
- Make focused, minimal changes that match the existing structure and naming.
- Fix root causes when practical; avoid unrelated refactors.
- Do not manually edit generated or machine-local files unless the task explicitly requires it.

## Files To Avoid Editing
- `.idea/`, `.gradle/`, `.kotlin/`, `.pytest_cache/`, `.venv/`
- `clients/local.properties`

## Backend Conventions
- Python target is `>=3.11`.
- The backend follows a DDD layered architecture:
  - `domain/`: entities, use cases, and repository protocols (no infrastructure dependencies).
  - `application/`: service layer orchestrating use cases.
  - `infra/`: concrete implementations (database clients, repository implementations).
  - `presentation/`: API routes, controllers, and request/response schemas.
- Repository interfaces live in `domain/` as `typing.Protocol` classes. Concrete SQL implementations live in `infra/postgresql/`.
- Prefer small, composable modules and keep request/response logic near the API layer.
- When changing backend behavior, update or add focused pytest coverage in `backend/tests/` when appropriate.

## Database Conventions
- The backend uses PostgreSQL via `psycopg` with connection pooling (`psycopg_pool`).
- Migrations live in `backend/infra/postgresql/migrations/` (close to the DB client).
- Put schema changes in new numbered migration files (e.g. `002_add_column.sql`).
- Do not rewrite an existing migration unless the user explicitly asks for it.
- The database layer is designed for engine swappability: to switch from PostgreSQL to another SQL database, add a new `infra/<engine>/` package implementing the same repository protocols and update `runtime.py`.

## Mobile Conventions
- Gradle root for clients is `clients/`.
- Keep Android-specific changes in `clients/android/`.
- Keep shared Kotlin Multiplatform code in `clients/shared/`.
- Avoid changing iOS/Xcode project settings unless required by the task.

## Useful Commands
- Start local database: `docker compose up -d`
- Backend setup: `cd backend && python -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt`
- Run backend: `cd backend && uvicorn main:app --reload --port 4000`
- Run backend tests: `cd backend && pytest`
- Build Android app: `cd clients && ./gradlew :android:assembleDebug`

## Validation
- Prefer the smallest relevant validation first:
  - backend changes: targeted `pytest` tests (requires local PostgreSQL via `docker compose up -d`)
  - client changes: relevant Gradle task
  - SQL changes: review migration ordering and naming
- If you cannot run validation, clearly state what remains unverified.

## Notes For Future Agents
- The top-level README is the main orientation document; check it before broad structural changes.
- `backend/README.md` contains the current API and environment contract and should stay in sync with backend changes.
- Supabase is used for OAuth authentication on mobile clients only. The backend validates Supabase JWTs but connects to PostgreSQL directly (via `DATABASE_URL`), not through the Supabase API.
