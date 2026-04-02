package com.wishlist.shared.di

import com.wishlist.shared.storage.DatabaseDriverFactory
import com.wishlist.shared.storage.WishlistDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { WishlistDatabase(get<DatabaseDriverFactory>().createDriver()) }
}
