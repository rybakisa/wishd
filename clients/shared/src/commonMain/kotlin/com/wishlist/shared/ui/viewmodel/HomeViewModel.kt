package com.wishlist.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishlist.shared.auth.AuthRepository
import com.wishlist.shared.data.AuthUser
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.domain.WishlistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repo: WishlistRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _wishlists = MutableStateFlow<List<Wishlist>>(emptyList())
    val wishlists: StateFlow<List<Wishlist>> = _wishlists.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentUser: StateFlow<AuthUser?> = auth.currentUser

    init {
        auth.currentUser
            .flatMapLatest { user ->
                val userId = user?.id ?: AuthRepository.ANON_USER_ID
                repo.observeWishlists(userId)
            }
            .catch { _error.value = it.message }
            .onEach { _wishlists.value = it }
            .launchIn(viewModelScope)

        viewModelScope.launch { refresh() }
    }

    fun refresh() {
        viewModelScope.launch {
            val user = auth.currentUser.value ?: return@launch
            runCatching { repo.refresh(user.id) }
                .onFailure { _error.value = it.message }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { repo.deleteWishlist(id) }.onFailure { _error.value = it.message }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
