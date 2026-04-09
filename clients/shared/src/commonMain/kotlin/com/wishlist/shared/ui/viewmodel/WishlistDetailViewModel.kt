package com.wishlist.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.domain.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WishlistDetailViewModel(
    val wishlistId: String,
    private val repo: WishlistRepository,
) : ViewModel() {
    private val _wishlist = MutableStateFlow<Wishlist?>(null)
    val wishlist: StateFlow<Wishlist?> = _wishlist.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { repo.getWishlist(wishlistId) }
                .onSuccess { _wishlist.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            runCatching { repo.deleteItem(wishlistId, itemId) }
                .onSuccess { load() }
                .onFailure { _error.value = it.message }
        }
    }
}
