package com.wishlist.shared.ui

import com.wishlist.shared.network.ApiException

fun Throwable.toUserMessage(): String = when (this) {
    is ApiException -> when (status) {
        401 -> "Session expired. Please sign in again."
        403 -> "You don't have permission for this action."
        404 -> "The requested item was not found."
        in 500..599 -> "Server error. Please try again later."
        else -> "Something went wrong (error $status)."
    }
    else -> when {
        message?.contains("UnknownHost", ignoreCase = true) == true ||
            message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            message?.contains("No address associated", ignoreCase = true) == true ->
            "No internet connection."
        message?.contains("timeout", ignoreCase = true) == true ||
            message?.contains("timed out", ignoreCase = true) == true ->
            "Request timed out. Please try again."
        else -> "Something went wrong."
    }
}
