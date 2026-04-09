package com.wishlist.shared.auth

import com.wishlist.shared.data.AuthProvider
import com.wishlist.shared.data.AuthUser
import com.wishlist.shared.network.WishlistApiClient
import com.wishlist.shared.storage.WishlistDatabase
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AuthRepository(
    private val api: WishlistApiClient,
    private val db: WishlistDatabase,
    private val tokenHolder: AuthTokenHolder,
    private val supabaseAuth: SupabaseAuthManager? = null,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncMutex = Mutex()

    /** Token that was last successfully synced or attempted — prevents retry loops. */
    private var lastSyncedToken: String? = null

    init {
        supabaseAuth?.let { mgr ->
            scope.launch {
                mgr.sessionStatus.collect { status ->
                    handleSessionStatus(status)
                }
            }
        }
    }

    private suspend fun handleSessionStatus(status: SessionStatus) {
        when (status) {
            is SessionStatus.Authenticated -> {
                val accessToken = supabaseAuth?.currentAccessToken() ?: return
                tokenHolder.set(accessToken)

                // Don't re-sync if we already processed this token
                if (accessToken == lastSyncedToken) return

                syncMutex.withLock {
                    // Double-check after acquiring lock
                    if (accessToken == lastSyncedToken) return
                    lastSyncedToken = accessToken

                    try {
                        val user = api.syncSession()
                        persistSession(user, accessToken)
                        _currentUser.value = user
                        _authState.value = AuthState.Authenticated(user)
                    } catch (e: Exception) {
                        // Backend sync failed but Supabase session is valid.
                        // Don't set Unauthenticated — that would trigger a loop.
                        // Log and leave state as-is; the user can still use the app
                        // once the backend is reachable.
                        println("Backend session sync failed: ${e.message}")
                    }
                }
            }
            is SessionStatus.NotAuthenticated -> {
                lastSyncedToken = null
                if (_authState.value is AuthState.Authenticated || _authState.value is AuthState.Refreshing) {
                    clearLocal()
                }
                _authState.value = AuthState.Unauthenticated
            }
            is SessionStatus.RefreshFailure -> {
                lastSyncedToken = null
                clearLocal()
                _authState.value = AuthState.Unauthenticated
            }
            is SessionStatus.Initializing -> {
                _authState.value = AuthState.Unknown
            }
        }
    }

    /** Load persisted session on app start. */
    suspend fun restore(): AuthUser? = withContext(Dispatchers.Default) {
        val row = db.wishlistQueries.selectSession().executeAsOneOrNull() ?: run {
            _authState.value = AuthState.Unauthenticated
            return@withContext null
        }
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
        _authState.value = AuthState.Authenticated(user)
        user
    }

    /** Sign in with Google via Supabase. */
    suspend fun signInWithGoogle() {
        supabaseAuth?.signInWithGoogle()
            ?: throw IllegalStateException("Supabase auth not configured")
    }

    /** Sign in with Apple via Supabase. */
    suspend fun signInWithApple() {
        supabaseAuth?.signInWithApple()
            ?: throw IllegalStateException("Supabase auth not configured")
    }

    suspend fun logout(): Unit = withContext(Dispatchers.Default) {
        lastSyncedToken = null
        supabaseAuth?.signOut()
        clearLocal()
        _authState.value = AuthState.Unauthenticated
    }

    fun isAuthenticated(): Boolean = _currentUser.value != null

    fun userIdOrAnon(): String = _currentUser.value?.id ?: ANON_USER_ID

    private fun persistSession(user: AuthUser, token: String) {
        db.wishlistQueries.upsertSession(
            userId = user.id,
            email = user.email,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            provider = user.provider.wire(),
            token = token,
        )
    }

    private fun clearLocal() {
        tokenHolder.set(null)
        db.wishlistQueries.clearSession()
        _currentUser.value = null
    }

    companion object {
        const val ANON_USER_ID = "anon-local"
    }
}
