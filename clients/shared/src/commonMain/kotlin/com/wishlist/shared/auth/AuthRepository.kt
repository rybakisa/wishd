package com.wishlist.shared.auth

import com.wishlist.shared.data.AuthProvider
import com.wishlist.shared.data.AuthStubRequest
import com.wishlist.shared.data.AuthUser
import com.wishlist.shared.network.WishlistApiClient
import com.wishlist.shared.storage.WishlistDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AuthRepository(
    private val api: WishlistApiClient,
    private val db: WishlistDatabase,
    private val tokenHolder: AuthTokenHolder,
) {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    /** Load persisted session on app start. */
    suspend fun restore(): AuthUser? = withContext(Dispatchers.Default) {
        val row = db.wishlistQueries.selectSession().executeAsOneOrNull() ?: return@withContext null
        tokenHolder.set(row.token)
        val user = AuthUser(
            id = row.user_id, email = row.email, displayName = row.display_name,
            avatarUrl = row.avatar_url,
            provider = when (row.provider) {
                "apple" -> AuthProvider.APPLE
                "google" -> AuthProvider.GOOGLE
                else -> AuthProvider.EMAIL
            },
        )
        _currentUser.value = user
        user
    }

    /**
     * Stub login — in real app, Apple/Google SDKs would yield an ID token that the backend
     * verifies. Here we just tell the backend "this email authenticated via this provider".
     */
    suspend fun login(provider: AuthProvider, email: String, displayName: String? = null): AuthUser =
        withContext(Dispatchers.Default) {
            val resp = api.authStub(AuthStubRequest(provider, email.trim(), displayName))
            tokenHolder.set(resp.token)
            db.wishlistQueries.upsertSession(
                userId = resp.user.id,
                email = resp.user.email,
                displayName = resp.user.displayName,
                avatarUrl = resp.user.avatarUrl,
                provider = provider.wire(),
                token = resp.token,
            )
            _currentUser.value = resp.user
            resp.user
        }

    suspend fun logout(): Unit = withContext(Dispatchers.Default) {
        tokenHolder.set(null)
        db.wishlistQueries.clearSession()
        _currentUser.value = null
    }

    fun isAuthenticated(): Boolean = _currentUser.value != null

    /** User id used to scope anonymous local data until login. */
    fun userIdOrAnon(): String = _currentUser.value?.id ?: ANON_USER_ID

    companion object {
        const val ANON_USER_ID = "anon-local"
    }
}
