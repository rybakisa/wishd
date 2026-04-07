package com.wishlist.shared.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthTokenHolderTest {

    @Test
    fun initialValueIsNull() {
        val holder = AuthTokenHolder()
        assertNull(holder.current())
        assertNull(holder.token.value)
    }

    @Test
    fun setUpdatesCurrentAndFlow() {
        val holder = AuthTokenHolder()
        holder.set("abc-123")
        assertEquals("abc-123", holder.current())
        assertEquals("abc-123", holder.token.value)
    }

    @Test
    fun setNullClearsToken() {
        val holder = AuthTokenHolder()
        holder.set("tok")
        holder.set(null)
        assertNull(holder.current())
    }

    @Test
    fun latestSetWins() {
        val holder = AuthTokenHolder()
        holder.set("first")
        holder.set("second")
        holder.set("third")
        assertEquals("third", holder.current())
    }
}
