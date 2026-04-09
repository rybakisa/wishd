package com.wishlist.android

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.wishlist.shared.di.initKoin
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class WishlistApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            baseUrl = BuildConfig.API_BASE_URL,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabasePublishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
            callbackScheme = CALLBACK_SCHEME,
            callbackHost = CALLBACK_HOST,
        ) {
            androidContext(this@WishlistApplication)
            androidLogger()
        }

        // Restore session if one exists so the user stays logged in across launches.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            runCatching { get<com.wishlist.shared.auth.AuthRepository>().restore() }
        }
    }

    companion object {
        // OAuth redirect: com.wishlist.android://auth
        const val CALLBACK_SCHEME = "com.wishlist.android"
        const val CALLBACK_HOST = "auth"
    }
}
