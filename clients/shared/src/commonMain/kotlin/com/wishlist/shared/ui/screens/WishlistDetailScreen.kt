package com.wishlist.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.wishlist.shared.data.WishlistItem
import com.wishlist.shared.ui.navigation.Routes
import com.wishlist.shared.ui.platform.LocalPlatformActions
import com.wishlist.shared.ui.viewmodel.WishlistDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistDetailScreen(wishlistId: String, nav: NavHostController) {
    val vm: WishlistDetailViewModel = koinViewModel(parameters = { parametersOf(wishlistId) })
    val w by vm.wishlist.collectAsState()
    val platformActions = LocalPlatformActions.current

    LaunchedEffect(wishlistId) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(w?.name ?: "Wishlist") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    w?.let { wishlist ->
                        IconButton(onClick = {
                            val url = "https://wishlist.app/s/${wishlist.shareToken}"
                            platformActions.copyToClipboard(url)
                            platformActions.shareText(url)
                        }) {
                            Icon(Icons.Default.Share, null)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate(Routes.addItem(wishlistId)) },
                containerColor = MaterialTheme.colorScheme.primary,
            ) { Icon(Icons.Default.Add, null) }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        val items = w?.items ?: emptyList()
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text(
                    "No items yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    ItemRow(item, onDelete = { vm.deleteItem(item.id) })
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: WishlistItem, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                    )
                }
                Row {
                    item.price?.let {
                        Text(
                            "${formatPrice(it)} ${item.currency}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    item.size?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "size $it", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatPrice(p: Double): String =
    if (p % 1.0 == 0.0) p.toInt().toString() else ((p * 100).toLong() / 100.0).toString()
