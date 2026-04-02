package com.wishlist.shared

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.wishlist.shared.storage.WishlistDatabase

actual fun createInMemoryDriver(): SqlDriver =
    NativeSqliteDriver(WishlistDatabase.Schema, ":memory:")
