package com.wishlist.shared.network

import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class WishlistApiClient(
    private val baseUrl: String,
    httpClient: HttpClient? = null,
) {

    private val client = httpClient ?: HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    suspend fun getWishlists(userId: String): List<Wishlist> =
        client.get("$baseUrl/wishlists?userId=$userId").body()

    suspend fun getWishlist(id: String): Wishlist =
        client.get("$baseUrl/wishlists/$id").body()

    suspend fun createWishlist(wishlist: Wishlist): Wishlist =
        client.post("$baseUrl/wishlists") {
            contentType(ContentType.Application.Json)
            setBody(wishlist)
        }.body()

    suspend fun updateWishlist(wishlist: Wishlist): Wishlist =
        client.patch("$baseUrl/wishlists/${wishlist.id}") {
            contentType(ContentType.Application.Json)
            setBody(wishlist)
        }.body()

    suspend fun deleteWishlist(id: String) {
        client.delete("$baseUrl/wishlists/$id")
    }

    suspend fun addItem(wishlistId: String, item: WishlistItem): WishlistItem =
        client.post("$baseUrl/wishlists/$wishlistId/items") {
            contentType(ContentType.Application.Json)
            setBody(item)
        }.body()

    suspend fun updateItem(item: WishlistItem): WishlistItem =
        client.patch("$baseUrl/wishlists/${item.wishlistId}/items/${item.id}") {
            contentType(ContentType.Application.Json)
            setBody(item)
        }.body()

    suspend fun deleteItem(wishlistId: String, itemId: String) {
        client.delete("$baseUrl/wishlists/$wishlistId/items/$itemId")
    }
}
