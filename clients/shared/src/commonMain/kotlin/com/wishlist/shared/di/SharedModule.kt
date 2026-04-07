package com.wishlist.shared.di

import com.wishlist.shared.auth.AuthRepository
import com.wishlist.shared.auth.AuthTokenHolder
import com.wishlist.shared.auth.SupabaseAuthManager
import com.wishlist.shared.data.WishlistRepositoryImpl
import com.wishlist.shared.domain.WishlistRepository
import com.wishlist.shared.network.WishlistApiClient
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun sharedModule(
    baseUrl: String,
    supabaseUrl: String = "",
    supabaseAnonKey: String = "",
    callbackScheme: String = "com.wishlist.app",
    callbackHost: String = "auth",
): Module = module {
    single { AuthTokenHolder() }
    single {
        if (supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()) {
            SupabaseAuthManager(supabaseUrl, supabaseAnonKey, callbackScheme, callbackHost)
        } else {
            null as SupabaseAuthManager?
        }
    }
    single { WishlistApiClient(baseUrl, get(), supabaseAuth = get()) }
    single { AuthRepository(get(), get(), get(), supabaseAuth = get()) }
    single<WishlistRepository> { WishlistRepositoryImpl(get(), get()) }
}

expect fun platformModule(): Module

fun initKoin(
    baseUrl: String,
    supabaseUrl: String = "",
    supabaseAnonKey: String = "",
    callbackScheme: String = "com.wishlist.app",
    callbackHost: String = "auth",
    appDeclaration: KoinAppDeclaration = {},
) {
    startKoin {
        appDeclaration()
        modules(
            sharedModule(baseUrl, supabaseUrl, supabaseAnonKey, callbackScheme, callbackHost),
            platformModule(),
        )
    }
}
