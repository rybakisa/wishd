package com.wishlist.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishlist.shared.auth.AuthRepository
import com.wishlist.shared.data.AuthProvider
import com.wishlist.shared.data.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    val currentUser: StateFlow<AuthUser?> = auth.currentUser

    fun login(provider: AuthProvider, email: String, displayName: String? = null, onDone: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            runCatching { auth.login(provider, email, displayName) }
                .onSuccess { onDone() }
                .onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun logout() { viewModelScope.launch { auth.logout() } }
}
