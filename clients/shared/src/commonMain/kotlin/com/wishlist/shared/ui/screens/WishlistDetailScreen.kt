package com.wishlist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.wishlist.shared.data.WishlistItem
import com.wishlist.shared.ui.navigation.AddItem
import com.wishlist.shared.ui.platform.LocalPlatformActions
import com.wishlist.shared.ui.theme.BubblegumRed
import com.wishlist.shared.ui.theme.ButtonShape
import com.wishlist.shared.ui.theme.CardShape
import com.wishlist.shared.ui.theme.LocalWishlistAccents
import com.wishlist.shared.ui.theme.PaperWhite
import com.wishlist.shared.ui.theme.SpeechBubbleShape
import com.wishlist.shared.ui.theme.StickerBorder
import com.wishlist.shared.ui.theme.SunshineYellow
import com.wishlist.shared.ui.theme.TypeBlack
import com.wishlist.shared.ui.theme.contrastingText
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
                title = {
                    Text(
                        w?.name ?: "Wishlist",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TypeBlack,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TypeBlack)
                    }
                },
                actions = {
                    w?.let { wishlist ->
                        IconButton(onClick = {
                            val url = "https://wishlist.app/s/${wishlist.shareToken}"
                            platformActions.copyToClipboard(url)
                            platformActions.shareText(url)
                        }) {
                            Icon(Icons.Default.Share, null, tint = TypeBlack)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            Button(
                onClick = { nav.navigate(AddItem(wishlistId = wishlistId)) },
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BubblegumRed,
                    contentColor = PaperWhite,
                ),
                border = StickerBorder,
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add", style = MaterialTheme.typography.titleMedium)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        val items = w?.items ?: emptyList()
        if (items.isEmpty()) {
            EmptyDetail(paddingValues = inner)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
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
private fun EmptyDetail(paddingValues: PaddingValues) {
    Box(
        Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = SpeechBubbleShape,
            color = PaperWhite,
            contentColor = TypeBlack,
            border = StickerBorder,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 44.dp, vertical = 30.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(SunshineYellow)
                        .border(1.dp, TypeBlack, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✨", fontSize = 48.sp)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "empty for now",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TypeBlack,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "add the first thing you've been eyeing",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TypeBlack,
                )
            }
        }
    }
}

@Composable
private fun ItemRow(item: WishlistItem, onDelete: () -> Unit) {
    val accents = LocalWishlistAccents.current
    val bg = remember(item.id) { accents.pickFor(item.id) }
    val fg = contrastingText(bg)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = bg,
        contentColor = fg,
        border = StickerBorder,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = fg,
                )
                item.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = fg,
                        maxLines = 2,
                    )
                }
                if (item.price != null || !item.size.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Row {
                        item.price?.let {
                            CalloutLabel("${formatPrice(it)} ${item.currency}")
                        }
                        item.size?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.width(8.dp))
                            CalloutLabel("size $it")
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = fg)
            }
        }
    }
}

@Composable
private fun CalloutLabel(text: String) {
    Surface(
        shape = CardShape,
        color = PaperWhite,
        contentColor = TypeBlack,
        border = StickerBorder,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun formatPrice(p: Double): String =
    if (p % 1.0 == 0.0) p.toInt().toString() else ((p * 100).toLong() / 100.0).toString()
