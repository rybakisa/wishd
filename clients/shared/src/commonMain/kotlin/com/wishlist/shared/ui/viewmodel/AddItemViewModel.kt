package com.wishlist.shared.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishlist.shared.data.ItemCreateRequest
import com.wishlist.shared.data.ParsedProduct
import com.wishlist.shared.domain.WishlistRepository
import com.wishlist.shared.ui.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddItemViewModel(
    val wishlistId: String,
    private val repo: WishlistRepository,
) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _parsed = MutableStateFlow<ParsedProduct?>(null)
    val parsed: StateFlow<ParsedProduct?> = _parsed.asStateFlow()

    fun parseUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            runCatching { repo.parseProductUrl(url) }
                .onSuccess { _parsed.value = it }
                .onFailure { _error.value = "Could not parse URL: ${it.toUserMessage()}" }
            _busy.value = false
        }
    }

    fun save(req: ItemCreateRequest, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            runCatching { repo.createItem(wishlistId, req) }
                .onSuccess { onDone() }
                .onFailure { _error.value = it.toUserMessage() }
            _busy.value = false
        }
    }
}
