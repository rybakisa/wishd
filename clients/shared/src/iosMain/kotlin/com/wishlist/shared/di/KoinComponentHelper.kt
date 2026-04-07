package com.wishlist.shared.di

import com.wishlist.shared.auth.AuthRepository
import com.wishlist.shared.auth.SupabaseAuthManager
import com.wishlist.shared.domain.WishlistRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Koin helper exposed to Swift so the iOS app can resolve shared dependencies
 * without importing Koin types directly into Swift files.
 */
class KoinComponentHelper : KoinComponent {
    private val wishlistRepository: WishlistRepository by inject()
    private val authRepository: AuthRepository by inject()
    fun getWishlistRepository(): WishlistRepository = wishlistRepository
    fun getAuthRepository(): AuthRepository = authRepository
}

fun getWishlistRepository(): WishlistRepository = KoinComponentHelper().getWishlistRepository()
fun getAuthRepository(): AuthRepository = KoinComponentHelper().getAuthRepository()

/**
 * Access the SupabaseAuthManager from Swift for deep link handling.
 * Returns null if Supabase is not configured.
 */
fun getSupabaseAuthManager(): SupabaseAuthManager? {
    return try {
        val helper = object : KoinComponent {
            val mgr: SupabaseAuthManager? by inject()
        }
        helper.mgr
    } catch (_: Exception) {
        null
    }
}
