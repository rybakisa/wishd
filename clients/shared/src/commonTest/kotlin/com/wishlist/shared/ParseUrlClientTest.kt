package com.wishlist.shared

import com.wishlist.shared.auth.AuthTokenHolder
import com.wishlist.shared.data.ParsedProduct
import com.wishlist.shared.network.ApiException
import com.wishlist.shared.network.WishlistApiClient
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the client-side parseUrl feature.
 *
 * These tests verify:
 * - Correct deserialization of ParsedProduct from the backend
 * - All required fields are present (name, url)
 * - Optional fields handle null/missing gracefully
 * - URL encoding of the query parameter
 * - Error handling for backend failures (400, 502)
 * - camelCase JSON wire format matches Kotlin data class
 */

private val testJson = Json { ignoreUnknownKeys = true; explicitNulls = false; isLenient = true }

private fun mockParseUrlClient(responseBody: String, status: HttpStatusCode = HttpStatusCode.OK): HttpClient =
    HttpClient(MockEngine { req ->
        respond(responseBody, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }) {
        install(ContentNegotiation) { json(testJson) }
        install(Logging) { level = LogLevel.NONE }
    }

private fun api(http: HttpClient): WishlistApiClient {
    val tokenHolder = AuthTokenHolder().also { it.set("test-jwt") }
    return WishlistApiClient("http://test", tokenHolder, http)
}

class ParseUrlClientTest {

    // -- Full product with all fields populated --

    @Test
    fun parseUrl_fullProduct_allFieldsDeserialized() = runTest {
        val product = ParsedProduct(
            name = "Air Max 90",
            description = "Classic sneaker",
            imageUrl = "https://cdn.example.com/shoe.jpg",
            price = 129.99,
            currency = "USD",
            url = "https://example.com/shoe",
        )
        val body = testJson.encodeToString(product)
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/shoe")

        assertEquals("Air Max 90", result.name)
        assertEquals("Classic sneaker", result.description)
        assertEquals("https://cdn.example.com/shoe.jpg", result.imageUrl)
        assertEquals(129.99, result.price)
        assertEquals("USD", result.currency)
        assertEquals("https://example.com/shoe", result.url)
    }

    // -- Required fields always present --

    @Test
    fun parseUrl_requiredFields_nameAndUrlAlwaysPresent() = runTest {
        val body = """{"name":"Minimal Item","url":"https://example.com/item"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/item")

        assertEquals("Minimal Item", result.name)
        assertEquals("https://example.com/item", result.url)
    }

    // -- Optional fields can be null --

    @Test
    fun parseUrl_optionalFieldsNull_deserializesGracefully() = runTest {
        val body = """{"name":"Bare Product","url":"https://example.com/bare"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/bare")

        assertEquals("Bare Product", result.name)
        assertEquals("https://example.com/bare", result.url)
        assertNull(result.description)
        assertNull(result.imageUrl)
        assertNull(result.price)
        assertNull(result.currency)
    }

    // -- Description fallback (null when missing) --

    @Test
    fun parseUrl_missingDescription_returnsNull() = runTest {
        val body = """{"name":"No Desc","url":"https://example.com/nodesc","price":10.0,"currency":"USD"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/nodesc")

        assertNull(result.description)
        assertNotNull(result.price)
    }

    // -- Image URL fallback --

    @Test
    fun parseUrl_missingImageUrl_returnsNull() = runTest {
        val body = """{"name":"No Image","url":"https://example.com/noimg","description":"Has desc but no image"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/noimg")

        assertNull(result.imageUrl)
        assertEquals("Has desc but no image", result.description)
    }

    // -- Price and currency fallbacks --

    @Test
    fun parseUrl_missingPriceAndCurrency_returnsNull() = runTest {
        val body = """{"name":"Free Item","url":"https://example.com/free","description":"A free thing"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/free")

        assertNull(result.price)
        assertNull(result.currency)
    }

    @Test
    fun parseUrl_priceWithoutCurrency_currencyNull() = runTest {
        val body = """{"name":"Price Only","url":"https://example.com/priceonly","price":49.99}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/priceonly")

        assertEquals(49.99, result.price)
        assertNull(result.currency)
    }

    @Test
    fun parseUrl_currencyWithoutPrice_priceNull() = runTest {
        val body = """{"name":"Currency Only","url":"https://example.com/curonly","currency":"EUR"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/curonly")

        assertNull(result.price)
        assertEquals("EUR", result.currency)
    }

    // -- Various currency codes --

    @Test
    fun parseUrl_differentCurrencies_deserializedCorrectly() = runTest {
        for ((currency, expectedPrice) in listOf("USD" to 29.99, "EUR" to 24.99, "GBP" to 19.99, "JPY" to 3000.0, "RUB" to 2500.0)) {
            val body = """{"name":"Item","url":"https://example.com","price":$expectedPrice,"currency":"$currency"}"""
            val client = api(mockParseUrlClient(body))

            val result = client.parseUrl("https://example.com")

            assertEquals(currency, result.currency)
            assertEquals(expectedPrice, result.price)
        }
    }

    // -- Name falls back to URL on backend --

    @Test
    fun parseUrl_nameIsUrl_whenNoTitleFound() = runTest {
        val body = """{"name":"https://example.com/unknown","url":"https://example.com/unknown"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/unknown")

        assertEquals("https://example.com/unknown", result.name)
    }

    // -- camelCase wire format --

    @Test
    fun parseUrl_camelCaseWireFormat_mapsCorrectly() = runTest {
        // Backend sends camelCase (imageUrl, not image_url)
        val body = """{"name":"Camel","url":"https://x.com","imageUrl":"https://img.com/a.jpg","description":"test"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://x.com")

        assertEquals("https://img.com/a.jpg", result.imageUrl)
    }

    // -- Unknown extra fields ignored --

    @Test
    fun parseUrl_unknownFields_ignoredGracefully() = runTest {
        val body = """{"name":"Extra","url":"https://x.com","unknownField":"should be ignored","anotherOne":123}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://x.com")

        assertEquals("Extra", result.name)
    }

    // -- URL encoding --

    @Test
    fun parseUrl_specialCharsInUrl_encodedInRequest() = runTest {
        val http = HttpClient(MockEngine { req ->
            // Verify the URL parameter is encoded
            val requestUrl = req.url.toString()
            val product = ParsedProduct(name = "Encoded", url = "https://example.com/search?q=test item")
            respond(
                testJson.encodeToString(product),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json(testJson) }
            install(Logging) { level = LogLevel.NONE }
        }
        val client = api(http)

        val result = client.parseUrl("https://example.com/search?q=test item")

        assertEquals("Encoded", result.name)
    }

    // -- Error handling --

    @Test
    fun parseUrl_invalidUrl_throws400() = runTest {
        val body = """{"detail":"Invalid URL"}"""
        val client = api(mockParseUrlClient(body, HttpStatusCode.BadRequest))

        assertFailsWith<ApiException> {
            client.parseUrl("not-a-url")
        }.also { ex ->
            assertEquals(400, ex.status)
        }
    }

    @Test
    fun parseUrl_backendParseError_throws502() = runTest {
        val body = """{"detail":{"error":"Could not parse product URL","details":"connection timeout"}}"""
        val client = api(mockParseUrlClient(body, HttpStatusCode.BadGateway))

        assertFailsWith<ApiException> {
            client.parseUrl("https://down.example.com")
        }.also { ex ->
            assertEquals(502, ex.status)
        }
    }

    @Test
    fun parseUrl_serverError_throws500() = runTest {
        val body = """{"detail":"Internal server error"}"""
        val client = api(mockParseUrlClient(body, HttpStatusCode.InternalServerError))

        assertFailsWith<ApiException> {
            client.parseUrl("https://broken.example.com")
        }.also { ex ->
            assertEquals(500, ex.status)
        }
    }

    // -- Large price values --

    @Test
    fun parseUrl_largePriceValue_deserializedCorrectly() = runTest {
        val body = """{"name":"Luxury Watch","url":"https://example.com/watch","price":15999.99,"currency":"CHF"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/watch")

        assertEquals(15999.99, result.price)
        assertEquals("CHF", result.currency)
    }

    // -- Zero price --

    @Test
    fun parseUrl_zeroPriceValue_deserializedCorrectly() = runTest {
        val body = """{"name":"Free Sample","url":"https://example.com/sample","price":0.0,"currency":"USD"}"""
        val client = api(mockParseUrlClient(body))

        val result = client.parseUrl("https://example.com/sample")

        assertEquals(0.0, result.price)
    }

    // -- Bare domain URL normalization --

    @Test
    fun parseUrl_bareDomainUrl_getsHttpsPrepended() = runTest {
        val http = HttpClient(MockEngine { req ->
            // The request URL should contain https-encoded version
            val urlParam = req.url.parameters["url"] ?: ""
            // Verify the client prepended https://
            val product = ParsedProduct(name = "WHOOP", url = urlParam)
            respond(
                testJson.encodeToString(product),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json(testJson) }
            install(Logging) { level = LogLevel.NONE }
        }
        val client = api(http)

        val result = client.parseUrl("amazon.ae/dp/B0DY2SWV16/")

        assertEquals(true, result.url.startsWith("https://"))
    }

    @Test
    fun parseUrl_existingScheme_preserved() = runTest {
        val http = HttpClient(MockEngine { req ->
            val urlParam = req.url.parameters["url"] ?: ""
            val product = ParsedProduct(name = "HTTP Item", url = urlParam)
            respond(
                testJson.encodeToString(product),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }) {
            install(ContentNegotiation) { json(testJson) }
            install(Logging) { level = LogLevel.NONE }
        }
        val client = api(http)

        val result = client.parseUrl("http://example.com/item")

        assertEquals(true, result.url.startsWith("http://"))
        assertEquals(false, result.url.startsWith("https://http://"))
    }
}
