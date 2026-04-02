package com.wishlist.shared

import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import com.wishlist.shared.network.WishlistApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WishlistApiClientTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val sampleWishlist = Wishlist(
        id = "w1",
        name = "Birthday Wishes",
        description = "My birthday wishlist",
        ownerId = "user1",
        isPublic = true,
        items = listOf(
            WishlistItem(id = "i1", wishlistId = "w1", name = "Book", price = 19.99)
        )
    )

    private val sampleItem = WishlistItem(
        id = "i2",
        wishlistId = "w1",
        name = "Headphones",
        price = 149.99,
    )

    @Test
    fun getWishlistsReturnsList() = runTest {
        val client = buildMockClient(json.encodeToString(listOf(sampleWishlist))) { method, url ->
            assertEquals(HttpMethod.Get, method)
            assertTrue(url.contains("/wishlists"))
        }
        val result = client.getWishlists("user1")
        assertEquals(1, result.size)
        assertEquals("w1", result[0].id)
        assertEquals("Birthday Wishes", result[0].name)
        assertEquals(1, result[0].items.size)
    }

    @Test
    fun getWishlistReturnsItem() = runTest {
        val client = buildMockClient(json.encodeToString(sampleWishlist)) { method, url ->
            assertEquals(HttpMethod.Get, method)
            assertTrue(url.contains("/wishlists/w1"))
        }
        val result = client.getWishlist("w1")
        assertEquals("w1", result.id)
        assertEquals("user1", result.ownerId)
    }

    @Test
    fun createWishlistPostsAndReturns() = runTest {
        val client = buildMockClient(json.encodeToString(sampleWishlist)) { method, url ->
            assertEquals(HttpMethod.Post, method)
            assertTrue(url.contains("/wishlists"))
        }
        val result = client.createWishlist(sampleWishlist)
        assertEquals("w1", result.id)
        assertEquals(true, result.isPublic)
    }

    @Test
    fun addItemPostsToWishlist() = runTest {
        val client = buildMockClient(json.encodeToString(sampleItem)) { method, url ->
            assertEquals(HttpMethod.Post, method)
            assertTrue(url.contains("/wishlists/w1/items"))
        }
        val result = client.addItem("w1", sampleItem)
        assertEquals("i2", result.id)
        assertEquals("Headphones", result.name)
        assertEquals(149.99, result.price)
    }

    @Test
    fun updateItemPatchesItem() = runTest {
        val updated = sampleItem.copy(isPurchased = true)
        val client = buildMockClient(json.encodeToString(updated)) { method, url ->
            assertEquals(HttpMethod.Patch, method)
            assertTrue(url.contains("/items/i2"))
        }
        val result = client.updateItem(updated)
        assertEquals(true, result.isPurchased)
    }

    private fun buildMockClient(
        responseBody: String,
        verify: (HttpMethod, String) -> Unit = { _, _ -> },
    ): WishlistApiClient {
        val engine = MockEngine { request ->
            verify(request.method, request.url.toString())
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        return WishlistApiClient("http://test.local", httpClient)
    }
}
