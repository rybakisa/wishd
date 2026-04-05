package com.wishlist.android

import android.app.Application
import com.wishlist.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.loadKoinModules
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

class WishlistApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(baseUrl = BASE_URL) {
            androidContext(this@WishlistApplication)
            androidLogger()
        }
        loadKoinModules(appModule)
    }

    private val appModule = module {
        viewModel { WishlistViewModel(get()) }
    }

    companion object {
        // Override via a build-config or remote config in production
        const val BASE_URL = "https://api.wishlistapp.example.com"
    }
}
