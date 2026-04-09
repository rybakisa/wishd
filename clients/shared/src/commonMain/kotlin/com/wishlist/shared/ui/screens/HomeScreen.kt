package com.wishlist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.wishlist.shared.data.CoverType
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.ui.navigation.Routes
import com.wishlist.shared.ui.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    val vm: HomeViewModel = koinViewModel()
    val wishlists by vm.wishlists.collectAsState()
    val user by vm.currentUser.collectAsState()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wishlists", fontWeight = FontWeight.Bold) },
                actions = {
                    if (user == null) {
                        TextButton(onClick = { nav.navigate(Routes.AUTH) }) { Text("Sign in") }
                    } else {
                        TextButton(onClick = { vm.refresh() }) { Text("Refresh") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (wishlists.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { nav.navigate(Routes.CREATE) },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) { Icon(Icons.Default.Add, null) }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        if (wishlists.isEmpty()) {
            EmptyHome(onAdd = { nav.navigate(Routes.CREATE) }, paddingValues = inner)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(wishlists, key = { it.id }) { w ->
                    WishlistCard(w, onClick = { nav.navigate(Routes.detail(w.id)) })
                }
            }
        }
        error?.let {
            LaunchedEffect(it) { /* surface via snackbar if desired */ }
        }
    }
}

@Composable
private fun EmptyHome(onAdd: () -> Unit, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(140.dp).clip(CircleShape).clickable { onAdd() },
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "+", fontSize = 72.sp, color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Thin,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Create your first wishlist", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap to start",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WishlistCard(w: Wishlist, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverChip(w.coverType, w.coverValue)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(w.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${w.items.size} items · ${w.access.name.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CoverChip(type: CoverType, value: String?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when (type) {
            CoverType.EMOJI -> Text(value ?: "🎁", fontSize = 28.sp)
            CoverType.IMAGE -> Text("🖼", fontSize = 22.sp)
            CoverType.NONE -> Text("🎁", fontSize = 28.sp)
        }
    }
}
