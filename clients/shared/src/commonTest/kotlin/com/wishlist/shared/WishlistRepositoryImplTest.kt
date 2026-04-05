package com.wishlist.shared

import com.wishlist.shared.auth.AuthTokenHolder
import com.wishlist.shared.data.Access
import com.wishlist.shared.data.CoverType
import com.wishlist.shared.data.ItemCreateRequest
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import com.wishlist.shared.data.WishlistRepositoryImpl
import com.wishlist.shared.network.WishlistApiClient
import com.wishlist.shared.storage.WishlistDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
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
import kotlin.test.assertNotNull

private val testJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

private fun mockClient(handlers: Map<String, () -> String>): HttpClient = HttpClient(
    MockEngine { req ->
        val key = "${req.method.value} ${req.url.encodedPath}"
        val body = handlers[key]?.invoke() ?: "{}"
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
) {
    install(ContentNegotiation) { json(testJson) }
    install(Logging) { level = LogLevel.NONE }
}

class WishlistRepositoryImplTest {
    private fun makeDb() = WishlistDatabase(createInMemoryDriver())

    @Test
    fun createAndObserveWishlist() = runTest {
        val db = makeDb()
        val w = Wishlist(
            id = "w1", ownerId = "u1", name = "Birthday",
            coverType = CoverType.EMOJI, coverValue = "\uD83C\uDF81", access = Access.LINK,
            shareToken = "tok1", createdAt = "2025-01-01",
        )
        val http = mockClient(mapOf(
            "POST /api/wishlists" to { testJson.encodeToString(w) },
        ))
        val tokenHolder = AuthTokenHolder().also { it.set("jwt") }
        val api = WishlistApiClient("http://x", tokenHolder, http)
        val repo = WishlistRepositoryImpl(api, db)

        val created = repo.createWishlist("Birthday", CoverType.EMOJI, "\uD83C\uDF81", Access.LINK)
        assertEquals("w1", created.id)
        assertEquals("tok1", created.shareToken)

        val observed = repo.observeWishlists("u1").first()
        assertEquals(1, observed.size)
        assertEquals("Birthday", observed.first().name)
        assertEquals(CoverType.EMOJI, observed.first().coverType)
    }

    @Test
    fun addItemPersists() = runTest {
        val db = makeDb()
        val item = WishlistItem(
            id = "i1", wishlistId = "w1", name = "AirPods",
            price = 249.0, currency = "USD", size = "M", sortOrder = 0,
        )
        val http = mockClient(mapOf(
            "POST /api/wishlists/w1/items" to { testJson.encodeToString(item) },
        ))
        val tokenHolder = AuthTokenHolder().also { it.set("jwt") }
        val api = WishlistApiClient("http://x", tokenHolder, http)
        val repo = WishlistRepositoryImpl(api, db)

        // Seed the parent wishlist locally so FK doesn't complain
        db.wishlistQueries.insertOrReplaceWishlist(
            id = "w1", ownerId = "u1", name = "Birthday",
            coverType = "emoji", coverValue = "\uD83C\uDF81",
            access = "link", shareToken = "tok", createdAt = "",
        )

        val created = repo.createItem("w1", ItemCreateRequest(name = "AirPods", price = 249.0, currency = "USD", size = "M"))
        assertEquals("i1", created.id)

        val fetched = repo.getWishlist("w1")
        assertNotNull(fetched)
        assertEquals(1, fetched!!.items.size)
        assertEquals("M", fetched.items[0].size)
    }
}
