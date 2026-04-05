package com.wishlist.shared.di

import com.wishlist.shared.auth.AuthRepository
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
