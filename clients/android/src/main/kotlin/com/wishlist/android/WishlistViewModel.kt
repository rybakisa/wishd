package com.wishlist.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import com.wishlist.shared.data.WishlistRepositoryImpl
import com.wishlist.shared.domain.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

sealed class WishlistUiState {
    object Loading : WishlistUiState()
    data class Success(val wishlists: List<Wishlist>) : WishlistUiState()
    data class Error(val message: String) : WishlistUiState()
}

class WishlistViewModel(
    private val repository: WishlistRepository,
    private val userId: String = "current-user",
) : ViewModel() {

    private val _uiState = MutableStateFlow<WishlistUiState>(WishlistUiState.Loading)
    val uiState: StateFlow<WishlistUiState> = _uiState

    init {
        observeWishlists()
        refreshFromNetwork()
    }

    private fun observeWishlists() {
        viewModelScope.launch {
            repository.getWishlists(userId)
                .catch { e -> _uiState.value = WishlistUiState.Error(e.message ?: "Unknown error") }
                .collect { wishlists -> _uiState.value = WishlistUiState.Success(wishlists) }
        }
    }

    private fun refreshFromNetwork() {
        viewModelScope.launch {
            try {
                (repository as? WishlistRepositoryImpl)?.refreshWishlists(userId)
            } catch (_: Exception) {
                // Offline-first: local DB data is still shown via Flow above
            }
        }
    }

    fun createWishlist(name: String) {
        viewModelScope.launch {
            try {
                repository.createWishlist(
                    Wishlist(id = UUID.randomUUID().toString(), name = name, ownerId = userId)
                )
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error("Failed to create wishlist: ${e.message}")
            }
        }
    }

    fun deleteWishlist(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteWishlist(id)
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error("Failed to delete: ${e.message}")
            }
        }
    }

    fun addItem(wishlistId: String, name: String, url: String? = null, price: Double? = null) {
        viewModelScope.launch {
            try {
                repository.addItem(
                    wishlistId,
                    WishlistItem(id = UUID.randomUUID().toString(), wishlistId = wishlistId, name = name, url = url, price = price)
                )
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error("Failed to add item: ${e.message}")
            }
        }
    }

    fun toggleItemPurchased(item: WishlistItem) {
        viewModelScope.launch {
            try {
                repository.markItemPurchased(item.id, !item.isPurchased)
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error("Failed to update item: ${e.message}")
            }
        }
    }
}
