package com.wishlist.android

import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import com.wishlist.shared.domain.WishlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WishlistViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)

    private val sampleWishlist = Wishlist(
        id = "w1",
        name = "Birthday",
        ownerId = "user1",
        items = listOf(WishlistItem(id = "i1", wishlistId = "w1", name = "Book", price = 19.99)),
    )

    @Test
    fun `initial state is Loading`() {
        val repo = FakeWishlistRepository(emitImmediately = false)
        val vm = WishlistViewModel(repo)
        assertTrue(vm.uiState.value is WishlistUiState.Loading)
    }

    @Test
    fun `uiState becomes Success when wishlists are emitted`() = scope.runTest {
        val repo = FakeWishlistRepository()
        repo.setWishlists(listOf(sampleWishlist))
        val vm = WishlistViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WishlistUiState.Success)
        assertEquals(1, (state as WishlistUiState.Success).wishlists.size)
        assertEquals("Birthday", state.wishlists[0].name)
    }

    @Test
    fun `createWishlist delegates to repository`() = scope.runTest {
        val repo = FakeWishlistRepository()
        val vm = WishlistViewModel(repo)
        vm.createWishlist("New List")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repo.createdWishlists.size)
        assertEquals("New List", repo.createdWishlists[0].name)
    }

    @Test
    fun `deleteWishlist delegates to repository`() = scope.runTest {
        val repo = FakeWishlistRepository()
        val vm = WishlistViewModel(repo)
        vm.deleteWishlist("w1")
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.deletedWishlistIds.contains("w1"))
    }

    @Test
    fun `toggleItemPurchased calls markItemPurchased with flipped value`() = scope.runTest {
        val repo = FakeWishlistRepository()
        val vm = WishlistViewModel(repo)
        val item = WishlistItem(id = "i1", wishlistId = "w1", name = "Book", isPurchased = false)
        vm.toggleItemPurchased(item)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(Pair("i1", true), repo.purchasedUpdates.last())
    }
}

/** Pure-Kotlin fake — no Android, no Koin, no SQLDelight. */
class FakeWishlistRepository(emitImmediately: Boolean = true) : WishlistRepository {
    private val flow = MutableStateFlow<List<Wishlist>>(emptyList())

    val createdWishlists = mutableListOf<Wishlist>()
    val deletedWishlistIds = mutableListOf<String>()
    val purchasedUpdates = mutableListOf<Pair<String, Boolean>>()

    init {
        if (emitImmediately) flow.value = emptyList()
    }

    fun setWishlists(list: List<Wishlist>) { flow.value = list }

    override fun getWishlists(userId: String): Flow<List<Wishlist>> = flow
    override suspend fun getWishlist(id: String): Wishlist? = flow.first().find { it.id == id }
    override suspend fun createWishlist(wishlist: Wishlist): Wishlist { createdWishlists += wishlist; return wishlist }
    override suspend fun updateWishlist(wishlist: Wishlist): Wishlist = wishlist
    override suspend fun deleteWishlist(id: String) { deletedWishlistIds += id }
    override suspend fun addItem(wishlistId: String, item: WishlistItem): WishlistItem = item
    override suspend fun updateItem(item: WishlistItem): WishlistItem = item
    override suspend fun deleteItem(id: String) {}
    override suspend fun markItemPurchased(itemId: String, purchased: Boolean) { purchasedUpdates += Pair(itemId, purchased) }
}
