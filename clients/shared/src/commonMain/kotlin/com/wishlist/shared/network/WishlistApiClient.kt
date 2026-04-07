package com.wishlist.shared.network

import com.wishlist.shared.auth.AuthTokenHolder
import com.wishlist.shared.auth.SupabaseAuthManager
import com.wishlist.shared.data.AuthUser
import com.wishlist.shared.data.ItemCreateRequest
import com.wishlist.shared.data.ItemUpdateRequest
import com.wishlist.shared.data.ParsedProduct
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistCreateRequest
import com.wishlist.shared.data.WishlistItem
import com.wishlist.shared.data.WishlistUpdateRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json

class WishlistApiClient(
    private val baseUrl: String,
    private val tokenHolder: AuthTokenHolder,
    httpClient: HttpClient? = null,
    private val supabaseAuth: SupabaseAuthManager? = null,
) {
    /** Emitted when auth is irrecoverably expired (refresh failed). */
    private val _authExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val authExpired: SharedFlow<Unit> = _authExpired.asSharedFlow()

    private val client = httpClient ?: HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            })
        }
        install(Logging) { level = LogLevel.INFO }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenHolder.current() ?: return@loadTokens null
                    BearerTokens(token, "")
                }
                refreshTokens {
                    val newToken = supabaseAuth?.refreshSession()
                    if (newToken != null) {
                        tokenHolder.set(newToken)
                        BearerTokens(newToken, "")
                    } else {
                        _authExpired.tryEmit(Unit)
                        null
                    }
                }
            }
        }
    }

    private suspend fun ensureOk(response: HttpResponse): HttpResponse {
        if (!response.status.isSuccess()) {
            throw ApiException(response.status.value, response.bodyOrEmpty())
        }
        return response
    }

    // ---- Auth ----

    /** Sync Supabase session with backend. */
    suspend fun syncSession(): AuthUser =
        ensureOk(client.post("$baseUrl/auth/session")).body()

    suspend fun getMe(): AuthUser =
        ensureOk(client.get("$baseUrl/api/me")).body()

    // ---- Wishlists ----
    suspend fun getWishlists(): List<Wishlist> =
        ensureOk(client.get("$baseUrl/api/wishlists")).body()

    suspend fun getWishlist(id: String): Wishlist =
        ensureOk(client.get("$baseUrl/api/wishlists/$id")).body()

    suspend fun createWishlist(body: WishlistCreateRequest): Wishlist =
        ensureOk(client.post("$baseUrl/api/wishlists") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }).body()

    suspend fun updateWishlist(id: String, body: WishlistUpdateRequest): Wishlist =
        ensureOk(client.patch("$baseUrl/api/wishlists/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }).body()

    suspend fun deleteWishlist(id: String) {
        ensureOk(client.delete("$baseUrl/api/wishlists/$id"))
    }

    // ---- Items ----
    suspend fun createItem(wishlistId: String, body: ItemCreateRequest): WishlistItem =
        ensureOk(client.post("$baseUrl/api/wishlists/$wishlistId/items") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }).body()

    suspend fun updateItem(wishlistId: String, itemId: String, body: ItemUpdateRequest): WishlistItem =
        ensureOk(client.patch("$baseUrl/api/wishlists/$wishlistId/items/$itemId") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }).body()

    suspend fun deleteItem(wishlistId: String, itemId: String) {
        ensureOk(client.delete("$baseUrl/api/wishlists/$wishlistId/items/$itemId"))
    }

    // ---- Share + parse ----
    suspend fun getShared(token: String): Wishlist =
        ensureOk(client.get("$baseUrl/api/share/$token")).body()

    suspend fun parseUrl(url: String): ParsedProduct {
        val normalised = url.trim().let { if ("://" !in it) "https://$it" else it }
        return ensureOk(client.get("$baseUrl/api/parse-url?url=${normalised.encodeURLParameter()}")).body()
    }
}

class ApiException(val status: Int, message: String) : RuntimeException("HTTP $status: $message")

private suspend fun HttpResponse.bodyOrEmpty(): String =
    try { this.bodyAsText() } catch (_: Throwable) { "" }
