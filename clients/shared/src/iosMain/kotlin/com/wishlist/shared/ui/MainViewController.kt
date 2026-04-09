package com.wishlist.shared.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.wishlist.shared.ui.platform.IosPlatformActions

fun MainViewController() = ComposeUIViewController {
    WishlistAppRoot(IosPlatformActions())
}
