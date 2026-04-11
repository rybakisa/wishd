package com.wishlist.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wishlist.shared.ui.screens.AddItemScreen
import com.wishlist.shared.ui.screens.AuthScreen
import com.wishlist.shared.ui.screens.CreateWishlistScreen
import com.wishlist.shared.ui.screens.HomeScreen
import com.wishlist.shared.ui.screens.WishlistDetailScreen
import kotlinx.serialization.Serializable

@Serializable data object Home
@Serializable data object Auth
@Serializable data object Create
@Serializable data class Detail(val id: String)
@Serializable data class AddItem(val wishlistId: String)

@Composable
fun WishlistApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Home) {
        composable<Home> { HomeScreen(nav) }
        composable<Auth> { AuthScreen(nav) }
        composable<Create> { CreateWishlistScreen(nav) }
        composable<Detail> { backStackEntry ->
            val route = backStackEntry.toRoute<Detail>()
            WishlistDetailScreen(route.id, nav)
        }
        composable<AddItem> { backStackEntry ->
            val route = backStackEntry.toRoute<AddItem>()
            AddItemScreen(route.wishlistId, nav)
        }
    }
}
