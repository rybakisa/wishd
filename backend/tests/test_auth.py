"""Unit tests for authentication: Supabase JWT validation, bearer extraction, user upsert, endpoints."""

import time

import jwt
import pytest
from fastapi.testclient import TestClient

import app.db as db_module
import app.config as config_module
import app.auth as auth_module
from app.auth import _decode_supabase_token, _decode_token, _extract_bearer, _upsert_user
from app.main import app

SUPABASE_SECRET = "supabase-test-secret-at-least-32-chars!"


@pytest.fixture(autouse=True)
def _tmp_db(tmp_path, monkeypatch):
    """Point the DB at a temporary file so each test gets a fresh database."""
    monkeypatch.setattr(db_module, "DB_PATH", tmp_path / "test.db")
    db_module.init_db()


@pytest.fixture(autouse=True)
def _set_supabase_secret(monkeypatch):
    """All tests run with a known Supabase JWT secret."""
    monkeypatch.setattr(config_module, "SUPABASE_JWT_SECRET", SUPABASE_SECRET)
    monkeypatch.setattr(auth_module, "SUPABASE_JWT_SECRET", SUPABASE_SECRET)


@pytest.fixture()
def client():
    return TestClient(app, raise_server_exceptions=False)


def _make_supabase_jwt(
    sub="u-1", aud="authenticated", email="test@example.com", **extra
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
# _decode_supabase_token
# ---------------------------------------------------------------------------
class TestDecodeSupabaseToken:
    def test_valid_token(self):
        token = _make_supabase_jwt(sub="su-1")
        payload = _decode_supabase_token(token)
        assert payload["sub"] == "su-1"
        assert payload["aud"] == "authenticated"

    def test_wrong_audience_rejected(self):
        token = _make_supabase_jwt(aud="anon")
        with pytest.raises(jwt.InvalidAudienceError):
            _decode_supabase_token(token)

    def test_expired_token_rejected(self):
        payload = {
            "sub": "u-1", "aud": "authenticated",
            "iat": int(time.time()) - 600, "exp": int(time.time()) - 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        with pytest.raises(jwt.ExpiredSignatureError):
            _decode_supabase_token(token)

    def test_wrong_secret_rejected(self):
        payload = {
            "sub": "u-1", "aud": "authenticated",
            "iat": int(time.time()), "exp": int(time.time()) + 300,
        }
        token = jwt.encode(payload, "wrong-secret-must-be-32-chars-long!", algorithm="HS256")
        with pytest.raises(jwt.InvalidSignatureError):
            _decode_supabase_token(token)

    def test_missing_sub_rejected(self):
        payload = {
            "aud": "authenticated",
            "iat": int(time.time()), "exp": int(time.time()) + 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        with pytest.raises(jwt.InvalidTokenError, match="missing sub"):
            _decode_supabase_token(token)

    def test_non_string_sub_rejected(self):
        payload = {
            "sub": 12345, "aud": "authenticated",
            "iat": int(time.time()), "exp": int(time.time()) + 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        with pytest.raises((jwt.InvalidTokenError, jwt.PyJWTError)):
            _decode_supabase_token(token)


class TestDecodeToken:
    def test_valid_supabase_token(self):
        token = _make_supabase_jwt(sub="su-1")
        assert _decode_token(token) == "su-1"

    def test_invalid_token_raises_401(self):
        with pytest.raises(Exception) as exc:
            _decode_token("garbage-token")
        assert exc.value.status_code == 401

    def test_expired_token_raises_401(self):
        payload = {
            "sub": "u-1", "aud": "authenticated",
            "iat": int(time.time()) - 600, "exp": int(time.time()) - 300,
        }
        token = jwt.encode(payload, SUPABASE_SECRET, algorithm="HS256")
        with pytest.raises(Exception) as exc:
            _decode_token(token)
        assert exc.value.status_code == 401

    def test_401_includes_www_authenticate_header(self):
        with pytest.raises(Exception) as exc:
            _decode_token("garbage")
        assert exc.value.headers.get("WWW-Authenticate") == "Bearer"


# ---------------------------------------------------------------------------
# _extract_bearer
# ---------------------------------------------------------------------------
class TestExtractBearer:
    def test_valid_bearer(self):
        assert _extract_bearer("Bearer abc123") == "abc123"

    def test_none_input(self):
        assert _extract_bearer(None) is None

    def test_basic_scheme(self):
        assert _extract_bearer("Basic abc123") is None

    def test_empty_token(self):
        assert _extract_bearer("Bearer ") is None

    def test_case_insensitive_scheme(self):
        assert _extract_bearer("bearer mytoken") == "mytoken"


# ---------------------------------------------------------------------------
# _upsert_user
# ---------------------------------------------------------------------------
class TestUpsertUser:
    def test_creates_new_user(self):
        user = _upsert_user("google", "Test@Example.COM", None, None)
        assert user.email == "test@example.com"
        assert user.provider == "google"
        assert len(user.id) == 36  # UUID format

    def test_idempotent_same_email(self):
        u1 = _upsert_user("google", "same@test.com", None, None)
        u2 = _upsert_user("google", "same@test.com", "New Name", None)
        assert u1.id == u2.id

    def test_normalizes_email(self):
        user = _upsert_user("apple", "  Alice@Bob.COM  ", None, None)
        assert user.email == "alice@bob.com"

    def test_display_name_fallback(self):
        user = _upsert_user("email", "jane@doe.com", None, None)
        assert user.display_name == "jane"

    def test_display_name_provided(self):
        user = _upsert_user("email", "with-name@test.com", "Jane Doe", None)
        assert user.display_name == "Jane Doe"

    def test_explicit_user_id(self):
        user = _upsert_user("google", "explicit@test.com", None, None, user_id="custom-id-123")
        assert user.id == "custom-id-123"


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
        assert data["id"] == "u-1"

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
            sub="apple-user-1",
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
        token = _make_supabase_jwt(sub="nonexistent-user-id")
        resp = client.get("/api/me", headers={"Authorization": f"Bearer {token}"})
        assert resp.status_code == 401


# ---------------------------------------------------------------------------
# get_current_user_optional
# ---------------------------------------------------------------------------
class TestOptionalAuth:
    def test_no_token_returns_none(self):
        from app.auth import get_current_user_optional
        result = get_current_user_optional(authorization=None)
        assert result is None

    def test_bad_token_returns_none(self):
        from app.auth import get_current_user_optional
        result = get_current_user_optional(authorization="Bearer garbage")
        assert result is None
