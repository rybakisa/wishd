"""Unit tests for authentication: Supabase JWT validation, bearer extraction, user upsert, endpoints."""

import time

import jwt
import pytest
from fastapi.testclient import TestClient

from main import app
from runtime import RuntimeContainer

SUPABASE_SECRET = "supabase-test-secret-at-least-32-chars!"


@pytest.fixture(autouse=True)
def _setup_runtime(pg_dsn):
    """Create a fresh RuntimeContainer for each test with a test DB."""
    container = RuntimeContainer(database_url=pg_dsn, jwt_secret=SUPABASE_SECRET)
    app.state.runtime = container
    yield
    container.close()


@pytest.fixture()
def client():
    return TestClient(app, raise_server_exceptions=False)


def _make_supabase_jwt(
    sub="00000000-0000-0000-0000-000000000001", aud="authenticated", email="test@example.com", **extra
) -> str:
    payload = {
        "sub": sub,
        "aud": aud,
        "email": email,
        "iat": int(time.time()),
        "exp": int(time.time()) + 300,
        "user_metadata": {"full_name": "Test User", "avatar_url": "https://img.test/a.png"},
        "app_metadata": {"provider": "google"},
        **extra,
    }
    return jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")


# ---------------------------------------------------------------------------
# Token decoding (via UserService)
# ---------------------------------------------------------------------------
class TestDecodeSupabaseToken:
    def _get_service(self, pg_dsn):
        from application.user.service import UserService
        from domain.user.use_cases import UserUseCases
        from infra.postgresql.client import PostgreSQLClient
        from infra.postgresql.user_repository import UserRepository
        db = PostgreSQLClient(pg_dsn)
        db.init_db()
        repo = UserRepository(db)
        uc = UserUseCases(repo)
        return UserService(uc, jwt_secret=SUPABASE_SECRET)

    def test_valid_token(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        token = _make_supabase_jwt(sub="00000000-0000-0000-0000-000000000011")
        payload = svc.decode_supabase_token(token)
        assert payload["sub"] == "00000000-0000-0000-0000-000000000011"
        assert payload["aud"] == "authenticated"

    def test_wrong_audience_rejected(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        token = _make_supabase_jwt(aud="anon")
        with pytest.raises(jwt.InvalidAudienceError):
            svc.decode_supabase_token(token)

    def test_expired_token_rejected(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        payload = {
            "sub": "u-1", "aud": "authenticated",
            "iat": int(time.time()) - 600, "exp": int(time.time()) - 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        with pytest.raises(jwt.ExpiredSignatureError):
            svc.decode_supabase_token(token)

    def test_wrong_secret_rejected(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        payload = {
            "sub": "u-1", "aud": "authenticated",
            "iat": int(time.time()), "exp": int(time.time()) + 300,
        }
        token = jwt.encode(payload, "wrong-secret-must-be-32-chars-long!", algorithm="HS256")
        with pytest.raises(jwt.InvalidSignatureError):
            svc.decode_supabase_token(token)

    def test_missing_sub_rejected(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        payload = {
            "aud": "authenticated",
            "iat": int(time.time()), "exp": int(time.time()) + 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        with pytest.raises(jwt.InvalidTokenError, match="missing sub"):
            svc.decode_supabase_token(token)

    def test_non_string_sub_rejected(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        payload = {
            "sub": 12345, "aud": "authenticated",
            "iat": int(time.time()), "exp": int(time.time()) + 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        with pytest.raises((jwt.InvalidTokenError, jwt.PyJWTError)):
            svc.decode_supabase_token(token)


class TestDecodeToken:
    def _get_service(self, pg_dsn):
        from application.user.service import UserService
        from domain.user.use_cases import UserUseCases
        from infra.postgresql.client import PostgreSQLClient
        from infra.postgresql.user_repository import UserRepository
        db = PostgreSQLClient(pg_dsn)
        db.init_db()
        repo = UserRepository(db)
        uc = UserUseCases(repo)
        return UserService(uc, jwt_secret=SUPABASE_SECRET)

    def test_valid_supabase_token(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        token = _make_supabase_jwt(sub="00000000-0000-0000-0000-000000000011")
        assert svc.decode_token(token) == "00000000-0000-0000-0000-000000000011"

    def test_invalid_token_raises_401(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        with pytest.raises(Exception) as exc:
            svc.decode_token("garbage-token")
        assert exc.value.status_code == 401

    def test_expired_token_raises_401(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        payload = {
            "sub": "u-1", "aud": "authenticated",
            "iat": int(time.time()) - 600, "exp": int(time.time()) - 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        with pytest.raises(Exception) as exc:
            svc.decode_token(token)
        assert exc.value.status_code == 401

    def test_401_includes_www_authenticate_header(self, pg_dsn):
        svc = self._get_service(pg_dsn)
        with pytest.raises(Exception) as exc:
            svc.decode_token("garbage")
        assert exc.value.headers.get("WWW-Authenticate") == "Bearer"


# ---------------------------------------------------------------------------
# Bearer extraction
# ---------------------------------------------------------------------------
class TestExtractBearer:
    def test_valid_bearer(self):
        from presentation.api.deps import _extract_bearer
        assert _extract_bearer("Bearer abc123") == "abc123"

    def test_none_input(self):
        from presentation.api.deps import _extract_bearer
        assert _extract_bearer(None) is None

    def test_basic_scheme(self):
        from presentation.api.deps import _extract_bearer
        assert _extract_bearer("Basic abc123") is None

    def test_empty_token(self):
        from presentation.api.deps import _extract_bearer
        assert _extract_bearer("Bearer ") is None

    def test_case_insensitive_scheme(self):
        from presentation.api.deps import _extract_bearer
        assert _extract_bearer("bearer mytoken") == "mytoken"


# ---------------------------------------------------------------------------
# User upsert
# ---------------------------------------------------------------------------
class TestUpsertUser:
    def _get_use_cases(self, pg_dsn):
        from domain.user.use_cases import UserUseCases
        from infra.postgresql.client import PostgreSQLClient
        from infra.postgresql.user_repository import UserRepository
        db = PostgreSQLClient(pg_dsn)
        db.init_db()
        repo = UserRepository(db)
        return UserUseCases(repo)

    def test_creates_new_user(self, pg_dsn):
        uc = self._get_use_cases(pg_dsn)
        user = uc.upsert(provider="google", email="Test@Example.COM", display_name=None, avatar_url=None)
        assert user.email == "test@example.com"
        assert user.provider == "google"
        assert len(user.id) == 36  # UUID format

    def test_idempotent_same_email(self, pg_dsn):
        uc = self._get_use_cases(pg_dsn)
        u1 = uc.upsert(provider="google", email="same@test.com", display_name=None, avatar_url=None)
        u2 = uc.upsert(provider="google", email="same@test.com", display_name="New Name", avatar_url=None)
        assert u1.id == u2.id

    def test_normalizes_email(self, pg_dsn):
        uc = self._get_use_cases(pg_dsn)
        user = uc.upsert(provider="apple", email="  Alice@Bob.COM  ", display_name=None, avatar_url=None)
        assert user.email == "alice@bob.com"

    def test_display_name_fallback(self, pg_dsn):
        uc = self._get_use_cases(pg_dsn)
        user = uc.upsert(provider="email", email="jane@doe.com", display_name=None, avatar_url=None)
        assert user.display_name == "jane"

    def test_display_name_provided(self, pg_dsn):
        uc = self._get_use_cases(pg_dsn)
        user = uc.upsert(provider="email", email="with-name@test.com", display_name="Jane Doe", avatar_url=None)
        assert user.display_name == "Jane Doe"

    def test_explicit_user_id(self, pg_dsn):
        uc = self._get_use_cases(pg_dsn)
        user = uc.upsert(provider="google", email="explicit@test.com", display_name=None, avatar_url=None, user_id="00000000-0000-0000-0000-000000000099")
        assert user.id == "00000000-0000-0000-0000-000000000099"


# ---------------------------------------------------------------------------
# POST /auth/session
# ---------------------------------------------------------------------------
class TestAuthSessionEndpoint:
    def test_creates_user_from_supabase_claims(self, client):
        token = _make_supabase_jwt()
        resp = client.post("/auth/session", headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        data = resp.json()
        assert data["email"] == "test@example.com"
        assert data["displayName"] == "Test User"
        assert data["provider"] == "google"
        assert data["id"] == "00000000-0000-0000-0000-000000000001"

    def test_missing_token_returns_401(self, client):
        resp = client.post("/auth/session")
        assert resp.status_code == 401
        assert resp.headers.get("www-authenticate") == "Bearer"

    def test_invalid_token_returns_401(self, client):
        resp = client.post("/auth/session", headers={"Authorization": "Bearer garbage"})
        assert resp.status_code == 401

    def test_idempotent_upsert(self, client):
        token = _make_supabase_jwt()
        r1 = client.post("/auth/session", headers={"Authorization": f"Bearer {token}"})
        r2 = client.post("/auth/session", headers={"Authorization": f"Bearer {token}"})
        assert r1.json()["id"] == r2.json()["id"]

    def test_extracts_avatar_url(self, client):
        token = _make_supabase_jwt()
        resp = client.post("/auth/session", headers={"Authorization": f"Bearer {token}"})
        assert resp.json()["avatarUrl"] == "https://img.test/a.png"

    def test_apple_provider(self, client):
        token = _make_supabase_jwt(
            sub="00000000-0000-0000-0000-000000000022",
            email="apple@test.com",
            **{"app_metadata": {"provider": "apple"}, "user_metadata": {"full_name": "Apple User"}},
        )
        resp = client.post("/auth/session", headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert resp.json()["provider"] == "apple"


# ---------------------------------------------------------------------------
# GET /api/me
# ---------------------------------------------------------------------------
class TestMeEndpoint:
    def _create_user(self, client) -> str:
        """Create a user via /auth/session and return the token."""
        token = _make_supabase_jwt()
        client.post("/auth/session", headers={"Authorization": f"Bearer {token}"})
        return token

    def test_with_valid_token(self, client):
        token = self._create_user(client)
        resp = client.get("/api/me", headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 200
        assert resp.json()["email"] == "test@example.com"

    def test_missing_token(self, client):
        resp = client.get("/api/me")
        assert resp.status_code == 401
        assert resp.headers.get("www-authenticate") == "Bearer"

    def test_invalid_token(self, client):
        resp = client.get("/api/me", headers={"Authorization": "Bearer garbage"})
        assert resp.status_code == 401
        assert resp.headers.get("www-authenticate") == "Bearer"

    def test_expired_token(self, client):
        payload = {
            "sub": "u-1", "aud": "authenticated",
            "iat": int(time.time()) - 600, "exp": int(time.time()) - 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        resp = client.get("/api/me", headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 401

    def test_user_not_found_returns_401(self, client):
        """Token is valid but user doesn't exist in DB yet (never called /auth/session)."""
        token = _make_supabase_jwt(sub="00000000-0000-0000-0000-ffffffffffff")
        resp = client.get("/api/me", headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 401


# ---------------------------------------------------------------------------
# get_current_user_optional
# ---------------------------------------------------------------------------
class TestOptionalAuth:
    def test_no_token_returns_none(self):
        from presentation.api.deps import _extract_bearer
        assert _extract_bearer(None) is None

    def test_bad_token_returns_none(self):
        from presentation.api.deps import _extract_bearer
        assert _extract_bearer("Bearer garbage") == "garbage"
