package com.wishlist.shared.data

import kotlinx.serialization.Serializable

@Serializable
data class Wishlist(
    val id: String,
    val name: String,
    val description: String = "",
    val items: List<WishlistItem> = emptyList(),
    val ownerId: String,
    val isPublic: Boolean = false,
)

@Serializable
data class WishlistItem(
    val id: String,
    val wishlistId: String,
    val name: String,
    val description: String = "",
    val url: String? = null,
    val imageUrl: String? = null,
    val price: Double? = null,
    val currency: String = "USD",
    val isPurchased: Boolean = false,
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
)
