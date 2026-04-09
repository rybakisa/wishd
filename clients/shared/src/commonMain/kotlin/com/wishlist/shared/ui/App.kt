package com.wishlist.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.wishlist.shared.ui.navigation.WishlistApp
import com.wishlist.shared.ui.platform.LocalPlatformActions
import com.wishlist.shared.ui.platform.PlatformActions
import com.wishlist.shared.ui.theme.WishlistTheme

@Composable
fun WishlistAppRoot(platformActions: PlatformActions) {
    WishlistTheme {
        CompositionLocalProvider(LocalPlatformActions provides platformActions) {
            WishlistApp()
        }
    }
}
