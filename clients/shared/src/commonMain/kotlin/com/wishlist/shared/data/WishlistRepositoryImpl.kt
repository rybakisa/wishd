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

    private val q = db.wishlistQueries

    override fun observeWishlists(ownerId: String): Flow<List<Wishlist>> =
        q.selectWishlistsByOwner(ownerId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    val items = q.selectItemsByWishlist(row.id).executeAsList().map { it.toDomain() }
                    row.toDomain(items)
                }
            }

    override suspend fun getWishlist(id: String): Wishlist? = withContext(Dispatchers.Default) {
        q.selectWishlistById(id).executeAsOneOrNull()?.let { row ->
            val items = q.selectItemsByWishlist(id).executeAsList().map { it.toDomain() }
            row.toDomain(items)
        }
    }

    override suspend fun createWishlist(
        name: String, coverType: CoverType, coverValue: String?, access: Access,
    ): Wishlist = withContext(Dispatchers.Default) {
        val created = api.createWishlist(WishlistCreateRequest(name, coverType, coverValue, access))
        upsertWishlist(created)
        created
    }

    override suspend fun updateWishlist(
        id: String, name: String?, coverType: CoverType?, coverValue: String?, access: Access?,
    ): Wishlist = withContext(Dispatchers.Default) {
        val updated = api.updateWishlist(id, WishlistUpdateRequest(name, coverType, coverValue, access))
        upsertWishlist(updated)
        updated
    }

    override suspend fun deleteWishlist(id: String): Unit = withContext(Dispatchers.Default) {
        api.deleteWishlist(id)
        q.deleteWishlist(id)
    }

    override suspend fun createItem(wishlistId: String, req: ItemCreateRequest): WishlistItem =
        withContext(Dispatchers.Default) {
            val created = api.createItem(wishlistId, req)
            upsertItem(created)
            created
        }

    override suspend fun updateItem(wishlistId: String, itemId: String, req: ItemUpdateRequest): WishlistItem =
        withContext(Dispatchers.Default) {
            val updated = api.updateItem(wishlistId, itemId, req)
            upsertItem(updated)
            updated
        }

    override suspend fun deleteItem(wishlistId: String, itemId: String): Unit =
        withContext(Dispatchers.Default) {
            api.deleteItem(wishlistId, itemId)
            q.deleteItem(itemId)
        }

    override suspend fun refresh(ownerId: String): Unit = withContext(Dispatchers.Default) {
        val remote = api.getWishlists()
        q.transaction {
            remote.forEach { w ->
                q.insertOrReplaceWishlist(
                    id = w.id, ownerId = w.ownerId, name = w.name,
                    coverType = w.coverType.wire(), coverValue = w.coverValue,
                    access = w.access.wire(), shareToken = w.shareToken,
                    createdAt = w.createdAt ?: "",
                )
                q.deleteItemsForWishlist(w.id)
                w.items.forEach { upsertItemNoTx(it) }
            }
        }
    }

    override suspend fun getShared(token: String): Wishlist = withContext(Dispatchers.Default) {
        api.getShared(token)
    }

    override suspend fun parseProductUrl(url: String): ParsedProduct = withContext(Dispatchers.Default) {
        api.parseUrl(url)
    }

    private fun upsertWishlist(w: Wishlist) {
        q.transaction {
            q.insertOrReplaceWishlist(
                id = w.id, ownerId = w.ownerId, name = w.name,
                coverType = w.coverType.wire(), coverValue = w.coverValue,
                access = w.access.wire(), shareToken = w.shareToken,
                createdAt = w.createdAt ?: "",
            )
            w.items.forEach { upsertItemNoTx(it) }
        }
    }

    private fun upsertItem(item: WishlistItem) {
        q.transaction { upsertItemNoTx(item) }
    }

    private fun upsertItemNoTx(item: WishlistItem) {
        q.insertOrReplaceItem(
            id = item.id, wishlistId = item.wishlistId, name = item.name,
            url = item.url, imageUrl = item.imageUrl, description = item.description,
            price = item.price, currency = item.currency,
            size = item.size, comment = item.comment,
            sortOrder = item.sortOrder.toLong(),
            createdAt = item.createdAt ?: "",
        )
    }
}

private fun com.wishlist.shared.storage.Wishlist.toDomain(items: List<WishlistItem>) = Wishlist(
    id = id,
    ownerId = owner_id,
    name = name,
    coverType = CoverType.fromWire(cover_type),
    coverValue = cover_value,
    access = Access.fromWire(access),
    shareToken = share_token,
    createdAt = created_at,
    items = items,
)

private fun com.wishlist.shared.storage.Wishlist_item.toDomain() = WishlistItem(
    id = id,
    wishlistId = wishlist_id,
    name = name,
    url = url,
    imageUrl = image_url,
    description = description,
    price = price,
    currency = currency,
    size = size,
    comment = comment,
    sortOrder = sort_order.toInt(),
    createdAt = created_at,
)
