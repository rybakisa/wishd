package com.wishlist.shared.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf

val LocalPlatformActions = staticCompositionLocalOf<PlatformActions> {
    error("PlatformActions not provided")
}
