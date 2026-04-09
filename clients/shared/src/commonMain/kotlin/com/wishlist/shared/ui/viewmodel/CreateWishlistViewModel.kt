package com.wishlist.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishlist.shared.auth.AuthRepository
import com.wishlist.shared.data.Access
import com.wishlist.shared.data.CoverType
import com.wishlist.shared.domain.WishlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateWishlistViewModel(
    private val repo: WishlistRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun requiresLogin(): Boolean = !auth.isAuthenticated()

    fun create(
        name: String, coverType: CoverType, coverValue: String?, access: Access,
        onSuccess: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            runCatching { repo.createWishlist(name, coverType, coverValue?.ifBlank { null }, access) }
                .onSuccess { onSuccess(it.id) }
                .onFailure { _error.value = it.message }
            _busy.value = false
        }
    }
}
