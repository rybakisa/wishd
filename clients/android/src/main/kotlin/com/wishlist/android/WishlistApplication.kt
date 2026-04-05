package com.wishlist.android

import android.app.Application
import com.wishlist.android.ui.viewmodel.AddItemViewModel
import com.wishlist.android.ui.viewmodel.AuthViewModel
import com.wishlist.android.ui.viewmodel.CreateWishlistViewModel
import com.wishlist.android.ui.viewmodel.HomeViewModel
import com.wishlist.android.ui.viewmodel.WishlistDetailViewModel
import com.wishlist.shared.di.initKoin
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class WishlistApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(baseUrl = BASE_URL) {
            androidContext(this@WishlistApplication)
            androidLogger()
        }
        loadKoinModules(appModule)

        // Restore session if one exists so the user stays logged in across launches.
        GlobalScope.launch {
            runCatching { get<com.wishlist.shared.auth.AuthRepository>().restore() }
        }
    }

    private val appModule = module {
        viewModel { HomeViewModel(get(), get()) }
        viewModel { AuthViewModel(get()) }
        viewModel { CreateWishlistViewModel(get(), get()) }
        viewModel { (wishlistId: String) -> WishlistDetailViewModel(wishlistId, get()) }
        viewModel { (wishlistId: String) -> AddItemViewModel(wishlistId, get()) }
    }

    companion object {
        // Android emulator maps host 127.0.0.1 to 10.0.2.2
        const val BASE_URL = "http://10.0.2.2:4000"
    }
}
