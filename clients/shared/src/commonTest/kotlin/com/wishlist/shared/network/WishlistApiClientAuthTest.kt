package com.wishlist.shared.network

import com.wishlist.shared.auth.AuthTokenHolder
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val testJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

private val meJson = """{"id":"u1","email":"a@b.com","displayName":"A","provider":"google"}"""

class WishlistApiClientAuthTest {

    @Test
    fun requestIncludesAuthHeaderWhenTokenSet() = runTest {
        var capturedAuth: String? = null
        val holder = AuthTokenHolder().also { it.set("my-jwt") }
        val client = HttpClient(MockEngine { req ->
            capturedAuth = req.headers[HttpHeaders.Authorization]
            respond(meJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(testJson) }
            install(Auth) {
                bearer {
                    loadTokens { BearerTokens(holder.current() ?: "", "") }
                    sendWithoutRequest { true }
                }
            }
        }
        val api = WishlistApiClient("http://test", holder, client)

        api.getMe()
        assertEquals("Bearer my-jwt", capturedAuth)
    }

    @Test
    fun requestOmitsAuthHeaderWhenTokenNull() = runTest {
        var capturedAuth: String? = null
        val holder = AuthTokenHolder() // no token set
        val client = HttpClient(MockEngine { req ->
            capturedAuth = req.headers[HttpHeaders.Authorization]
            respond(meJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(testJson) }
            install(Auth) {
                bearer {
                    loadTokens {
                        val t = holder.current() ?: return@loadTokens null
                        BearerTokens(t, "")
                    }
                }
            }
        }
        val api = WishlistApiClient("http://test", holder, client)

        api.getMe()
        assertTrue(capturedAuth == null)
    }

    @Test
    fun throws401AsApiException() = runTest {
        val holder = AuthTokenHolder().also { it.set("expired") }
        val client = HttpClient(MockEngine {
            respond("Unauthorized", HttpStatusCode.Unauthorized)
        }) {
            install(ContentNegotiation) { json(testJson) }
        }
        val api = WishlistApiClient("http://test", holder, client)

        val ex = assertFailsWith<ApiException> { api.getMe() }
        assertEquals(401, ex.status)
    }

    @Test
    fun authExpiredFlowAvailable() = runTest {
        val holder = AuthTokenHolder()
        val client = HttpClient(MockEngine {
            respond(meJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(testJson) }
        }
        val api = WishlistApiClient("http://test", holder, client)
        assertNotNull(api.authExpired)
    }
}
