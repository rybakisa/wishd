package com.wishlist.shared

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello from KMP! Platform: ${platform.name}"
}
