package com.wishlist.android

import android.app.Application
import com.wishlist.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class WishlistApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(baseUrl = BASE_URL) {
            androidContext(this@WishlistApplication)
            androidLogger()
        }
    }

    companion object {
        // Override via a build-config or remote config in production
        const val BASE_URL = "https://api.wishlistapp.example.com"
    }
}
