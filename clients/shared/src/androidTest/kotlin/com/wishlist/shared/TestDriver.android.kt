package com.wishlist.shared

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.wishlist.shared.storage.WishlistDatabase

actual fun createInMemoryDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { WishlistDatabase.Schema.create(it) }
