package com.wishlist.shared.di

import com.wishlist.shared.data.WishlistRepositoryImpl
import com.wishlist.shared.domain.WishlistRepository
import com.wishlist.shared.network.WishlistApiClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun sharedModule(baseUrl: String): Module = module {
    single { WishlistApiClient(baseUrl) }
    single<WishlistRepository> {
        WishlistRepositoryImpl(get(), get())
    }
}

expect fun platformModule(): Module

/**
 * Starts Koin with all shared + platform modules.
 * Called from Android `Application.onCreate` and from iOS `WishlistApp.init`
 * via `KoinHelper.shared.start(baseUrl:)`.
 */
fun initKoin(baseUrl: String, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            sharedModule(baseUrl),
            platformModule(),
        )
    }
}
