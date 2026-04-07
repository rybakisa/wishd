package com.wishlist.shared.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class CoverType {
    @SerialName("none") NONE,
    @SerialName("emoji") EMOJI,
    @SerialName("image") IMAGE;

    fun wire(): String = when (this) {
        NONE -> "none"
        EMOJI -> "emoji"
        IMAGE -> "image"
    }

    companion object {
        fun fromWire(s: String?): CoverType = when (s) {
            "emoji" -> EMOJI
            "image" -> IMAGE
            else -> NONE
        }
    }
}

enum class Access {
    @SerialName("link") LINK,
    @SerialName("public") PUBLIC,
    @SerialName("private") PRIVATE;

    fun wire(): String = when (this) {
        LINK -> "link"
        PUBLIC -> "public"
        PRIVATE -> "private"
    }

    companion object {
        fun fromWire(s: String?): Access = when (s) {
            "public" -> PUBLIC
            "private" -> PRIVATE
            else -> LINK
        }
    }
}

enum class AuthProvider {
    @SerialName("apple") APPLE,
    @SerialName("google") GOOGLE,
    @SerialName("email") EMAIL;

    fun wire(): String = when (this) {
        APPLE -> "apple"
        GOOGLE -> "google"
        EMAIL -> "email"
    }
}

@Serializable
data class Wishlist(
    val id: String,
    val ownerId: String,
    val name: String,
    val coverType: CoverType = CoverType.NONE,
    val coverValue: String? = null,
    val access: Access = Access.LINK,
    val shareToken: String = "",
    val createdAt: String? = null,
    val items: List<WishlistItem> = emptyList(),
)

@Serializable
data class WishlistItem(
    val id: String,
    val wishlistId: String,
    val name: String,
    val url: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val currency: String = "USD",
    val size: String? = null,
    val comment: String? = null,
    val sortOrder: Int = 0,
    val createdAt: String? = null,
)

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val provider: AuthProvider,
)

@Serializable
data class WishlistCreateRequest(
    val name: String,
    val coverType: CoverType = CoverType.NONE,
    val coverValue: String? = null,
    val access: Access = Access.LINK,
)

@Serializable
data class WishlistUpdateRequest(
    val name: String? = null,
    val coverType: CoverType? = null,
    val coverValue: String? = null,
    val access: Access? = null,
)

@Serializable
data class ItemCreateRequest(
    val name: String,
    val url: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val currency: String = "USD",
    val size: String? = null,
    val comment: String? = null,
    val sortOrder: Int = 0,
)

@Serializable
data class ItemUpdateRequest(
    val name: String? = null,
    val url: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val size: String? = null,
    val comment: String? = null,
    val sortOrder: Int? = null,
)

@Serializable
data class ParsedProduct(
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val url: String,
)
