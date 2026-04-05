package com.wishlist.shared.domain

import com.wishlist.shared.data.Access
import com.wishlist.shared.data.CoverType
import com.wishlist.shared.data.ItemCreateRequest
import com.wishlist.shared.data.ItemUpdateRequest
import com.wishlist.shared.data.ParsedProduct
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    fun observeWishlists(ownerId: String): Flow<List<Wishlist>>
    suspend fun getWishlist(id: String): Wishlist?
    suspend fun createWishlist(name: String, coverType: CoverType, coverValue: String?, access: Access): Wishlist
    suspend fun updateWishlist(id: String, name: String?, coverType: CoverType?, coverValue: String?, access: Access?): Wishlist
    suspend fun deleteWishlist(id: String)

    suspend fun createItem(wishlistId: String, req: ItemCreateRequest): WishlistItem
    suspend fun updateItem(wishlistId: String, itemId: String, req: ItemUpdateRequest): WishlistItem
    suspend fun deleteItem(wishlistId: String, itemId: String)

    suspend fun refresh(ownerId: String)
    suspend fun getShared(token: String): Wishlist
    suspend fun parseProductUrl(url: String): ParsedProduct
}
