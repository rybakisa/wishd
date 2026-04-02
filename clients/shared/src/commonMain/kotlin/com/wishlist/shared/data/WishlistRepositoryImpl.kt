package com.wishlist.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.wishlist.shared.domain.WishlistRepository
import com.wishlist.shared.network.WishlistApiClient
import com.wishlist.shared.storage.WishlistDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WishlistRepositoryImpl(
    private val api: WishlistApiClient,
    private val db: WishlistDatabase,
) : WishlistRepository {

    private val wishlistQueries = db.wishlistQueries

    override fun getWishlists(userId: String): Flow<List<Wishlist>> =
        wishlistQueries.selectWishlistsByOwner(userId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    val items = wishlistQueries.selectItemsByWishlist(row.id)
                        .executeAsList()
                        .map { it.toWishlistItem() }
                    row.toWishlist(items)
                }
            }

    override suspend fun getWishlist(id: String): Wishlist? = withContext(Dispatchers.Default) {
        wishlistQueries.selectWishlistById(id).executeAsOneOrNull()?.let { row ->
            val items = wishlistQueries.selectItemsByWishlist(row.id)
                .executeAsList()
                .map { it.toWishlistItem() }
            row.toWishlist(items)
        }
    }

    override suspend fun createWishlist(wishlist: Wishlist): Wishlist = withContext(Dispatchers.Default) {
        val created = api.createWishlist(wishlist)
        upsertWishlist(created)
        created
    }

    override suspend fun updateWishlist(wishlist: Wishlist): Wishlist = withContext(Dispatchers.Default) {
        val updated = api.updateWishlist(wishlist)
        wishlistQueries.updateWishlist(
            name = updated.name,
            description = updated.description,
            isPublic = if (updated.isPublic) 1L else 0L,
            id = updated.id,
        )
        updated
    }

    override suspend fun deleteWishlist(id: String): Unit = withContext(Dispatchers.Default) {
        api.deleteWishlist(id)
        wishlistQueries.deleteWishlist(id)
    }

    override suspend fun addItem(wishlistId: String, item: WishlistItem): WishlistItem =
        withContext(Dispatchers.Default) {
            val created = api.addItem(wishlistId, item)
            upsertItem(created)
            created
        }

    override suspend fun updateItem(item: WishlistItem): WishlistItem = withContext(Dispatchers.Default) {
        val updated = api.updateItem(item)
        wishlistQueries.updateItem(
            name = updated.name,
            description = updated.description,
            url = updated.url,
            imageUrl = updated.imageUrl,
            price = updated.price,
            currency = updated.currency,
            isPurchased = if (updated.isPurchased) 1L else 0L,
            id = updated.id,
        )
        updated
    }

    override suspend fun deleteItem(id: String): Unit = withContext(Dispatchers.Default) {
        val item = wishlistQueries.selectItemById(id).executeAsOneOrNull()
        if (item != null) {
            api.deleteItem(item.wishlist_id, id)
        }
        wishlistQueries.deleteItem(id)
    }

    override suspend fun markItemPurchased(itemId: String, purchased: Boolean): Unit =
        withContext(Dispatchers.Default) {
            val item = wishlistQueries.selectItemById(itemId).executeAsOneOrNull()
                ?: return@withContext
            val updated = item.toWishlistItem().copy(isPurchased = purchased)
            api.updateItem(updated)
            wishlistQueries.updateItemPurchased(
                isPurchased = if (purchased) 1L else 0L,
                id = itemId,
            )
        }

    suspend fun refreshWishlists(userId: String) = withContext(Dispatchers.Default) {
        val wishlists = api.getWishlists(userId)
        wishlists.forEach { upsertWishlist(it) }
    }

    private fun upsertWishlist(wishlist: Wishlist) {
        wishlistQueries.insertOrReplaceWishlist(
            id = wishlist.id,
            name = wishlist.name,
            description = wishlist.description,
            ownerId = wishlist.ownerId,
            isPublic = if (wishlist.isPublic) 1L else 0L,
        )
        wishlist.items.forEach { upsertItem(it) }
    }

    private fun upsertItem(item: WishlistItem) {
        wishlistQueries.insertOrReplaceItem(
            id = item.id,
            wishlistId = item.wishlistId,
            name = item.name,
            description = item.description,
            url = item.url,
            imageUrl = item.imageUrl,
            price = item.price,
            currency = item.currency,
            isPurchased = if (item.isPurchased) 1L else 0L,
        )
    }
}

// Mapping extensions
private fun com.wishlist.shared.storage.Wishlist.toWishlist(items: List<WishlistItem>) = Wishlist(
    id = id,
    name = name,
    description = description,
    items = items,
    ownerId = owner_id,
    isPublic = is_public != 0L,
)

private fun com.wishlist.shared.storage.Wishlist_item.toWishlistItem() = WishlistItem(
    id = id,
    wishlistId = wishlist_id,
    name = name,
    description = description,
    url = url,
    imageUrl = image_url,
    price = price,
    currency = currency,
    isPurchased = is_purchased != 0L,
)
