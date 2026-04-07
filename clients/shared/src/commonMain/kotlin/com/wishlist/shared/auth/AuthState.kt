package com.wishlist.shared.auth

import com.wishlist.shared.data.AuthUser

/**
 * Explicit auth state machine so the UI can distinguish between
 * "we haven't checked yet" and "user is definitely not logged in".
 */
sealed class AuthState {
    /** App launched, session check in progress. */
    data object Unknown : AuthState()

    /** No valid session found. */
    data object Unauthenticated : AuthState()

    /** Token refresh in progress (user was authenticated, token expired). */
    data object Refreshing : AuthState()

    /** Successfully authenticated. */
    data class Authenticated(val user: AuthUser) : AuthState()
}
