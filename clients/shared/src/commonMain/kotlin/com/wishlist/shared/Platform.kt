package com.wishlist.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
