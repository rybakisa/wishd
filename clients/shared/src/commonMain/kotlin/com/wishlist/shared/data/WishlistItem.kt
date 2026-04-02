package com.wishlist.shared.data

import kotlinx.serialization.Serializable

@Serializable
data class WishlistItem(
    val id: String,
    val userId: String,
    val title: String,
    val url: String?,
    val imageUrl: String?,
    val price: Double?,
    val notes: String?,
    val isPurchased: Boolean = false,
    val createdAt: Long = 0L,
)
