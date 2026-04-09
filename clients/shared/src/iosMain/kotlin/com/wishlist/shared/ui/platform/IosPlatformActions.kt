package com.wishlist.shared.ui.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

class IosPlatformActions : PlatformActions {
    override fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }

    override fun shareText(text: String) {
        val activityVC = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootVC?.presentViewController(activityVC, animated = true, completion = null)
    }
}
