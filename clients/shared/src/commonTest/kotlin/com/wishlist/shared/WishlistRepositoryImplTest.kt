package com.wishlist.shared

import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import com.wishlist.shared.data.WishlistRepositoryImpl
import com.wishlist.shared.network.WishlistApiClient
import com.wishlist.shared.storage.WishlistDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit-level regression tests for [WishlistRepositoryImpl] using in-memory SQLDelight and mock Ktor engine.
 * These run on all KMP targets (JVM + native).
 */
class WishlistRepositoryImplTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val baseWishlist = Wishlist(
        id = "w1",
        name = "Regression Wishlist",
        description = "desc",
        ownerId = "u1",
        isPublic = false,
        items = emptyList(),
    )

    private val baseItem = WishlistItem(
        id = "item1",
        wishlistId = "w1",
        name = "Widget",
        price = 9.99,
    )

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun makeRepo(
        responseForPost: String = json.encodeToString(baseWishlist),
        responseForGet: String = json.encodeToString(listOf(baseWishlist)),
    ): WishlistRepositoryImpl {
        val engine = MockEngine { request ->
            val body = when {
                request.url.encodedPath.endsWith("/items") -> json.encodeToString(baseItem)
                request.url.encodedPath.contains("/items/") -> json.encodeToString(baseItem.copy(isPurchased = true))
                request.url.encodedPath.endsWith("/wishlists") && request.method.value == "POST" -> responseForPost
                request.url.encodedPath.endsWith("/wishlists") -> responseForGet
                else -> responseForPost
            }
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = WishlistApiClient(
            baseUrl = "http://test.local",
            httpClient = HttpClient(engine) {
                install(ContentNegotiation) { json(json) }
            },
        )
        val db = WishlistDatabase(createInMemoryDriver())
        return WishlistRepositoryImpl(client, db)
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `getWishlists returns empty flow before any data`() = runTest {
        val repo = makeRepo()
        val result = repo.getWishlists("u1").first()
        assertTrue(result.isEmpty(), "Expected empty list from clean DB")
    }

    @Test
    fun `createWishlist persists to local DB`() = runTest {
        val repo = makeRepo()
        repo.createWishlist(baseWishlist)
        val local = repo.getWishlist("w1")
        assertNotNull(local, "Wishlist should be in DB after createWishlist")
        assertEquals("Regression Wishlist", local?.name)
    }

    @Test
    fun `addItem persists item under wishlist`() = runTest {
        val repo = makeRepo()
        repo.createWishlist(baseWishlist)
        repo.addItem("w1", baseItem)

        val wishlist = repo.getWishlist("w1")
        assertNotNull(wishlist)
        assertEquals(1, wishlist!!.items.size)
        assertEquals("Widget", wishlist.items[0].name)
    }

    @Test
    fun `markItemPurchased flips purchased flag`() = runTest {
        val repo = makeRepo()
        repo.createWishlist(baseWishlist)
        repo.addItem("w1", baseItem)

        repo.markItemPurchased("item1", true)

        val wishlist = repo.getWishlist("w1")
        val item = wishlist?.items?.find { it.id == "item1" }
        assertNotNull(item)
        assertTrue(item!!.isPurchased, "Item should be marked purchased")
    }

    @Test
    fun `deleteItem removes item from DB`() = runTest {
        val repo = makeRepo()
        repo.createWishlist(baseWishlist)
        repo.addItem("w1", baseItem)
        repo.deleteItem("item1")

        val wishlist = repo.getWishlist("w1")
        assertTrue(wishlist?.items.isNullOrEmpty(), "Item should be removed after deleteItem")
    }

    @Test
    fun `deleteWishlist removes wishlist from DB`() = runTest {
        val repo = makeRepo()
        repo.createWishlist(baseWishlist)
        repo.deleteWishlist("w1")

        assertNull(repo.getWishlist("w1"), "Wishlist should be gone after delete")
    }

    @Test
    fun `refreshWishlists syncs remote data to local DB`() = runTest {
        val wishlistWithItem = baseWishlist.copy(items = listOf(baseItem))
        val repo = makeRepo(
            responseForGet = json.encodeToString(listOf(wishlistWithItem)),
        )
        repo.refreshWishlists("u1")

        val local = repo.getWishlist("w1")
        assertNotNull(local)
        assertEquals(1, local?.items?.size)
    }
}
