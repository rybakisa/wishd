package com.wishlist.android

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wishlist.android.ui.viewmodel.AddItemViewModel
import com.wishlist.android.ui.viewmodel.AuthViewModel
import com.wishlist.android.ui.viewmodel.CreateWishlistViewModel
import com.wishlist.android.ui.viewmodel.HomeViewModel
import com.wishlist.android.ui.viewmodel.WishlistDetailViewModel
import com.wishlist.shared.di.initKoin
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
        initKoin(
            baseUrl = BuildConfig.API_BASE_URL,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            callbackScheme = CALLBACK_SCHEME,
            callbackHost = CALLBACK_HOST,
        ) {
            androidContext(this@WishlistApplication)
            androidLogger()
        }
        loadKoinModules(appModule)

        // Restore session if one exists so the user stays logged in across launches.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            runCatching { get<com.wishlist.shared.auth.AuthRepository>().restore() }
        }
    }

    private val appModule = module {
        viewModel { HomeViewModel(get(), get()) }
        viewModel { AuthViewModel(get(), get()) }
        viewModel { CreateWishlistViewModel(get(), get()) }
        viewModel { (wishlistId: String) -> WishlistDetailViewModel(wishlistId, get()) }
        viewModel { (wishlistId: String) -> AddItemViewModel(wishlistId, get()) }
    }

    companion object {
        // OAuth redirect: com.wishlist.android://auth
        const val CALLBACK_SCHEME = "com.wishlist.android"
        const val CALLBACK_HOST = "auth"
    }
}
