package com.wishlist.shared

import app.cash.sqldelight.db.SqlDriver

expect fun createInMemoryDriver(): SqlDriver
