package com.wishlist.shared.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper around the Supabase Auth SDK.
 * Handles OAuth sign-in, session observation, and token refresh.
 *
 * @param callbackScheme URL scheme for OAuth redirect (e.g. "com.wishlist.android")
 * @param callbackHost   URL host for OAuth redirect (e.g. "auth")
 */
class SupabaseAuthManager(
    supabaseUrl: String,
    supabasePublishableKey: String,
    callbackScheme: String = "com.wishlist.app",
    callbackHost: String = "auth",
) {
    val client: SupabaseClient = createSupabaseClient(supabaseUrl, supabasePublishableKey) {
        install(Auth) {
            alwaysAutoRefresh = true
            scheme = callbackScheme
            host = callbackHost
        }
    }

    /** Observable session status changes (Initializing -> Authenticated / NotAuthenticated). */
    val sessionStatus: Flow<SessionStatus> get() = client.auth.sessionStatus

    /** Current access token or null. */
    fun currentAccessToken(): String? =
        client.auth.currentSessionOrNull()?.accessToken

    /** Current refresh token or null. */
    fun currentRefreshToken(): String? =
        client.auth.currentSessionOrNull()?.refreshToken

    /** Sign in with Google OAuth. Opens browser for consent. */
    suspend fun signInWithGoogle() {
        client.auth.signInWith(Google)
    }

    /** Sign in with Apple OAuth. Opens browser/native sheet for consent. */
    suspend fun signInWithApple() {
        client.auth.signInWith(Apple)
    }

    /** Attempt to refresh the current session. Returns new access token or null. */
    suspend fun refreshSession(): String? {
        return try {
            client.auth.refreshCurrentSession()
            client.auth.currentSessionOrNull()?.accessToken
        } catch (_: Exception) {
            null
        }
    }

    /** Sign out and clear session. */
    suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Best-effort sign out
        }
    }
}
