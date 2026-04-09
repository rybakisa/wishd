package com.wishlist.shared.di

import com.wishlist.shared.auth.AuthRepository
import com.wishlist.shared.auth.AuthTokenHolder
import com.wishlist.shared.auth.SupabaseAuthManager
import com.wishlist.shared.data.WishlistRepositoryImpl
import com.wishlist.shared.domain.WishlistRepository
import com.wishlist.shared.network.WishlistApiClient
import com.wishlist.shared.ui.viewmodel.AddItemViewModel
import com.wishlist.shared.ui.viewmodel.AuthViewModel
import com.wishlist.shared.ui.viewmodel.CreateWishlistViewModel
import com.wishlist.shared.ui.viewmodel.HomeViewModel
import com.wishlist.shared.ui.viewmodel.WishlistDetailViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun sharedModule(
    baseUrl: String,
    supabaseUrl: String = "",
    supabasePublishableKey: String = "",
    callbackScheme: String = "com.wishlist.app",
    callbackHost: String = "auth",
): Module = module {
    single { AuthTokenHolder() }
    single {
        if (supabaseUrl.isNotBlank() && supabasePublishableKey.isNotBlank()) {
            SupabaseAuthManager(supabaseUrl, supabasePublishableKey, callbackScheme, callbackHost)
        } else {
            null as SupabaseAuthManager?
        }
    }
    single { WishlistApiClient(baseUrl, get(), supabaseAuth = get()) }
    single { AuthRepository(get(), get(), get(), supabaseAuth = get()) }
    single<WishlistRepository> { WishlistRepositoryImpl(get(), get()) }

    // ViewModels
    viewModel { HomeViewModel(get(), get()) }
    viewModel { AuthViewModel(get(), get()) }
    viewModel { CreateWishlistViewModel(get(), get()) }
    viewModel { (wishlistId: String) -> WishlistDetailViewModel(wishlistId, get()) }
    viewModel { (wishlistId: String) -> AddItemViewModel(wishlistId, get()) }
}

expect fun platformModule(): Module

fun initKoin(
    baseUrl: String,
    supabaseUrl: String = "",
    supabasePublishableKey: String = "",
    callbackScheme: String = "com.wishlist.app",
    callbackHost: String = "auth",
    appDeclaration: KoinAppDeclaration = {},
) {
    startKoin {
        appDeclaration()
        modules(
            sharedModule(baseUrl, supabaseUrl, supabasePublishableKey, callbackScheme, callbackHost),
            platformModule(),
        )
    }
}
