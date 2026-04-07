package com.wishlist.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.wishlist.android.ui.WishlistApp
import com.wishlist.android.ui.theme.WishlistTheme
import com.wishlist.shared.auth.SupabaseAuthManager
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthRedirect(intent)
        setContent {
            WishlistTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WishlistApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent)
    }

    /**
     * Handle the OAuth redirect deep link from Supabase.
     * The browser redirects to com.wishlist.android://auth#access_token=...
     * and the Supabase SDK parses the fragment to establish the session.
     */
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
