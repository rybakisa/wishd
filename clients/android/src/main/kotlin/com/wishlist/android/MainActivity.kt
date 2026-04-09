package com.wishlist.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wishlist.shared.auth.SupabaseAuthManager
import com.wishlist.shared.ui.WishlistAppRoot
import com.wishlist.shared.ui.platform.AndroidPlatformActions
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthRedirect(intent)
        val platformActions = AndroidPlatformActions(applicationContext)
        setContent {
            WishlistAppRoot(platformActions)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    private fun handleOAuthRedirect(intent: Intent?) {
        intent ?: return
        val uri = intent.data ?: return
        if (uri.scheme != "com.wishlist.android") return
        try {
            val mgr: SupabaseAuthManager = get()
            mgr.client.handleDeeplinks(intent)
        } catch (_: Exception) {
            // Supabase not configured
        }
    }
}
