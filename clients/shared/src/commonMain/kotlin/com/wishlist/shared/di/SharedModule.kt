package com.wishlist.shared.di

import com.wishlist.shared.auth.AuthRepository
import com.wishlist.shared.auth.AuthTokenHolder
import com.wishlist.shared.data.WishlistRepositoryImpl
import com.wishlist.shared.domain.WishlistRepository
import com.wishlist.shared.network.WishlistApiClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun sharedModule(baseUrl: String): Module = module {
    single { AuthTokenHolder() }
    single { WishlistApiClient(baseUrl, get()) }
    single { AuthRepository(get(), get(), get()) }
    single<WishlistRepository> { WishlistRepositoryImpl(get(), get()) }
}

expect fun platformModule(): Module

fun initKoin(baseUrl: String, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            sharedModule(baseUrl),
            platformModule(),
        )
    }
}
