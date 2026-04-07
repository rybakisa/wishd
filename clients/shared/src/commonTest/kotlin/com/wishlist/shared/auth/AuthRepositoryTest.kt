package com.wishlist.shared.auth

import com.wishlist.shared.createInMemoryDriver
import com.wishlist.shared.data.AuthProvider
import com.wishlist.shared.data.AuthUser
import com.wishlist.shared.network.WishlistApiClient
import com.wishlist.shared.storage.WishlistDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

class AuthRepositoryTest {

    private fun makeRepo(): Triple<AuthRepository, AuthTokenHolder, WishlistDatabase> {
        val db = WishlistDatabase(createInMemoryDriver())
        val holder = AuthTokenHolder()
        // Mock client not needed for most tests — we test restore/logout/state directly
        val engine = MockEngine {
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(testJson) } }
        val api = WishlistApiClient("http://test", holder, client)
        val repo = AuthRepository(api, db, holder) // no supabaseAuth for unit tests
        return Triple(repo, holder, db)
    }

    @Test
    fun initialAuthStateIsUnknown() {
        val (repo) = makeRepo()
        assertTrue(repo.authState.value is AuthState.Unknown)
    }

    @Test
    fun restoreFromDb() = runTest {
        val (repo, holder, db) = makeRepo()
        db.wishlistQueries.upsertSession(
            userId = "u-2", email = "saved@test.com", displayName = "Saved",
            avatarUrl = null, provider = "google", token = "persisted-jwt",
        )
        val user = repo.restore()
        assertNotNull(user)
        assertEquals("u-2", user.id)
        assertEquals("persisted-jwt", holder.current())
        assertEquals("u-2", repo.currentUser.value?.id)
        assertTrue(repo.authState.value is AuthState.Authenticated)
    }

    @Test
    fun restoreReturnsNullWhenEmpty() = runTest {
        val (repo, holder) = makeRepo()
        val user = repo.restore()
        assertNull(user)
        assertNull(holder.current())
        assertNull(repo.currentUser.value)
        assertTrue(repo.authState.value is AuthState.Unauthenticated)
    }

    @Test
    fun logoutClearsEverything() = runTest {
        val (repo, holder, db) = makeRepo()
        // Seed a session first
        db.wishlistQueries.upsertSession(
            userId = "u-1", email = "test@test.com", displayName = "Test",
            avatarUrl = null, provider = "google", token = "jwt",
        )
        repo.restore()
        assertTrue(repo.isAuthenticated())

        repo.logout()
        assertNull(holder.current())
        assertNull(repo.currentUser.value)
        assertNull(db.wishlistQueries.selectSession().executeAsOneOrNull())
        assertTrue(repo.authState.value is AuthState.Unauthenticated)
    }

    @Test
    fun isAuthenticatedReflectsState() = runTest {
        val (repo, _, db) = makeRepo()
        assertFalse(repo.isAuthenticated())

        db.wishlistQueries.upsertSession(
            userId = "u-1", email = "test@test.com", displayName = "Test",
            avatarUrl = null, provider = "google", token = "jwt",
        )
        repo.restore()
        assertTrue(repo.isAuthenticated())

        repo.logout()
        assertFalse(repo.isAuthenticated())
    }

    @Test
    fun userIdOrAnonReturnsIdOrDefault() = runTest {
        val (repo, _, db) = makeRepo()
        assertEquals("anon-local", repo.userIdOrAnon())

        db.wishlistQueries.upsertSession(
            userId = "u-1", email = "test@test.com", displayName = "Test",
            avatarUrl = null, provider = "google", token = "jwt",
        )
        repo.restore()
        assertEquals("u-1", repo.userIdOrAnon())
    }

    @Test
    fun signInWithGoogleThrowsWithoutSupabase() = runTest {
        val (repo) = makeRepo()
        var threw = false
        try {
            repo.signInWithGoogle()
        } catch (e: IllegalStateException) {
            threw = true
            assertTrue(e.message?.contains("not configured") == true)
        }
        assertTrue(threw)
    }

    @Test
    fun signInWithAppleThrowsWithoutSupabase() = runTest {
        val (repo) = makeRepo()
        var threw = false
        try {
            repo.signInWithApple()
        } catch (e: IllegalStateException) {
            threw = true
            assertTrue(e.message?.contains("not configured") == true)
        }
        assertTrue(threw)
    }

    @Test
    fun restorePreservesProvider() = runTest {
        val (repo, _, db) = makeRepo()
        db.wishlistQueries.upsertSession(
            userId = "u-apple", email = "apple@test.com", displayName = "Apple User",
            avatarUrl = null, provider = "apple", token = "jwt-apple",
        )
        val user = repo.restore()
        assertNotNull(user)
        assertEquals(AuthProvider.APPLE, user.provider)
    }
}
