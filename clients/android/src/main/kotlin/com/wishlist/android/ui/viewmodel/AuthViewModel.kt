package com.wishlist.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishlist.shared.auth.AuthRepository
import com.wishlist.shared.auth.AuthState
import com.wishlist.shared.data.AuthUser
import com.wishlist.shared.network.WishlistApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val auth: AuthRepository,
    private val apiClient: WishlistApiClient? = null,
) : ViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    val currentUser: StateFlow<AuthUser?> = auth.currentUser
    val authState: StateFlow<AuthState> = auth.authState

    init {
        // Auto-logout when token refresh fails irrecoverably
        apiClient?.let { client ->
            viewModelScope.launch {
                client.authExpired.collect {
                    auth.logout()
                }
            }
        }
    }

    /** Sign in with Google via Supabase OAuth. */
    fun signInWithGoogle(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            runCatching { auth.signInWithGoogle() }
                .onSuccess { onDone() }
                .onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    /** Sign in with Apple via Supabase OAuth. */
    fun signInWithApple(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            runCatching { auth.signInWithApple() }
                .onSuccess { onDone() }
                .onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}
