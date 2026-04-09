package com.wishlist.shared.ui.platform

/**
 * Platform-specific actions that need native implementations.
 */
interface PlatformActions {
    fun copyToClipboard(text: String)
    fun shareText(text: String)
}
