package com.wishlist.shared.di

import com.wishlist.shared.domain.WishlistRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Koin helper exposed to Swift so the iOS app can resolve shared dependencies
 * without importing Koin types directly into Swift files.
 *
 * Usage from Swift:
 *   let repo: WishlistRepository = KoinComponentKt.getWishlistRepository()
 */
class KoinComponentHelper : KoinComponent {
    private val wishlistRepository: WishlistRepository by inject()
    fun getWishlistRepository(): WishlistRepository = wishlistRepository
}

fun getWishlistRepository(): WishlistRepository = KoinComponentHelper().getWishlistRepository()
