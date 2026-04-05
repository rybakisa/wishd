package com.wishlist.shared.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the current auth token in memory so that Ktor can attach it to every request.
 * The persisted copy lives in the SQLDelight `session` table; see AuthRepository.
 */
class AuthTokenHolder {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    fun set(value: String?) {
        _token.value = value
    }

    fun current(): String? = _token.value
}
