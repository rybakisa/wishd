package com.wishlist.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WishlistApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistApp(viewModel: WishlistViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Wishlists") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Wishlist")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is WishlistUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is WishlistUiState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                )
                is WishlistUiState.Success -> WishlistList(
                    wishlists = state.wishlists,
                    onDelete = { viewModel.deleteWishlist(it.id) },
                    onToggleItem = { viewModel.toggleItemPurchased(it) },
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateWishlistDialog(
            onConfirm = { name -> viewModel.createWishlist(name); showCreateDialog = false },
            onDismiss = { showCreateDialog = false },
        )
    }
}

@Composable
private fun WishlistList(
    wishlists: List<Wishlist>,
    onDelete: (Wishlist) -> Unit,
    onToggleItem: (WishlistItem) -> Unit,
) {
    if (wishlists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No wishlists yet. Tap + to create one.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(wishlists, key = { it.id }) { wishlist ->
                WishlistCard(
                    wishlist = wishlist,
                    onDelete = { onDelete(wishlist) },
                    onToggleItem = onToggleItem,
                )
            }
        }
    }
}

@Composable
private fun WishlistCard(
    wishlist: Wishlist,
    onDelete: () -> Unit,
    onToggleItem: (WishlistItem) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = wishlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete wishlist")
                }
            }
            if (wishlist.description.isNotBlank()) {
                Text(text = wishlist.description, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            wishlist.items.forEach { item ->
                WishlistItemRow(item = item, onToggle = { onToggleItem(item) })
            }
        }
    }
}

@Composable
private fun WishlistItemRow(item: WishlistItem, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = item.isPurchased, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                style = MaterialTheme.typography.bodyMedium,
            )
            item.price?.let { price ->
                Text(
                    text = "$${String.format("%.2f", price)} ${item.currency}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CreateWishlistDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Wishlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
