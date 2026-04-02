package com.wishlist.shared.domain

import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    fun getWishlists(userId: String): Flow<List<Wishlist>>
    suspend fun getWishlist(id: String): Wishlist?
    suspend fun createWishlist(wishlist: Wishlist): Wishlist
    suspend fun updateWishlist(wishlist: Wishlist): Wishlist
    suspend fun deleteWishlist(id: String)
    suspend fun addItem(wishlistId: String, item: WishlistItem): WishlistItem
    suspend fun updateItem(item: WishlistItem): WishlistItem
    suspend fun deleteItem(id: String)
    suspend fun markItemPurchased(itemId: String, purchased: Boolean)
}
