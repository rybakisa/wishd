package com.wishlist.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class GreetingTest {
    @Test
    fun greet_containsHelloFromKMP() {
        val greeting = Greeting().greet()
        assertTrue(greeting.contains("Hello from KMP!"), "Unexpected: $greeting")
    }
}
