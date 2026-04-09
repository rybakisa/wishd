package com.wishlist.shared.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wishlist.shared.ui.screens.AddItemScreen
import com.wishlist.shared.ui.screens.AuthScreen
import com.wishlist.shared.ui.screens.CreateWishlistScreen
import com.wishlist.shared.ui.screens.HomeScreen
import com.wishlist.shared.ui.screens.WishlistDetailScreen

object Routes {
    const val HOME = "home"
    const val AUTH = "auth"
    const val CREATE = "create"
    const val DETAIL = "detail/{id}"
    const val ADD_ITEM = "add_item/{id}"
    fun detail(id: String) = "detail/$id"
    fun addItem(id: String) = "add_item/$id"
}

@Composable
fun WishlistApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.AUTH) { AuthScreen(nav) }
        composable(Routes.CREATE) { CreateWishlistScreen(nav) }
        composable(Routes.DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            WishlistDetailScreen(id, nav)
        }
        composable(Routes.ADD_ITEM) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            AddItemScreen(id, nav)
        }
    }
}
