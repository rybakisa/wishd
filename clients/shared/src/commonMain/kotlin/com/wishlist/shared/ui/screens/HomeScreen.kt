package com.wishlist.shared.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.wishlist.shared.data.CoverType
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.ui.navigation.Auth
import com.wishlist.shared.ui.navigation.Create
import com.wishlist.shared.ui.navigation.Detail
import com.wishlist.shared.ui.theme.BubblegumRed
import com.wishlist.shared.ui.theme.ButtonShape
import com.wishlist.shared.ui.theme.CardShape
import com.wishlist.shared.ui.theme.GrapePunch
import com.wishlist.shared.ui.theme.LightGray
import com.wishlist.shared.ui.theme.LocalWishlistAccents
import com.wishlist.shared.ui.theme.PaperWhite
import com.wishlist.shared.ui.theme.SpeechBubbleShape
import com.wishlist.shared.ui.theme.StickerBorder
import com.wishlist.shared.ui.theme.SunshineYellow
import com.wishlist.shared.ui.theme.TypeBlack
import com.wishlist.shared.ui.theme.contrastingText
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
                title = {
                    Text(
                        "wishes",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                actions = {
                    if (user == null) {
                        AccentChip(text = "Sign in", onClick = { nav.navigate(Auth) })
                    } else {
                        AccentChip(text = "Refresh", onClick = { vm.refresh() })
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (wishlists.isNotEmpty()) {
                Button(
                    onClick = { nav.navigate(Create) },
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
                    Text("New wish", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        if (wishlists.isEmpty()) {
            EmptyHome(onAdd = { nav.navigate(Create) }, paddingValues = inner)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                itemsIndexed(wishlists, key = { _, w -> w.id }) { index, w ->
                    WishlistCard(
                        w = w,
                        index = index,
                        onClick = { nav.navigate(Detail(id = w.id)) },
                    )
                }
            }
        }
        error?.let {
            LaunchedEffect(it) { /* surface via snackbar if desired */ }
        }
    }
}

@Composable
private fun AccentChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = LightGray,
        contentColor = TypeBlack,
        border = StickerBorder,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun EmptyHome(onAdd: () -> Unit, paddingValues: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
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
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(SunshineYellow)
                        .border(1.dp, TypeBlack, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎁", fontSize = 64.sp)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "nothing here yet",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TypeBlack,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "what are you secretly hoping for?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TypeBlack,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onAdd,
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GrapePunch,
                        contentColor = PaperWhite,
                    ),
                    border = StickerBorder,
                    modifier = Modifier.heightIn(min = 60.dp),
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start a wishlist", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun WishlistCard(w: Wishlist, index: Int, onClick: () -> Unit) {
    val accents = LocalWishlistAccents.current
    val bg = remember(w.id) { accents.pickFor(w.id) }
    val fg = contrastingText(bg)
    val tilt by animateFloatAsState(
        targetValue = if (index % 2 == 0) -1.2f else 1.2f,
        label = "tilt",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().rotate(tilt),
        shape = CardShape,
        color = bg,
        contentColor = fg,
        border = StickerBorder,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverChip(type = w.coverType, value = w.coverValue)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    w.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = fg,
                )
                Spacer(Modifier.height(4.dp))
                val count = w.items.size
                val itemsLabel = if (count == 1) "1 wish" else "$count wishes"
                Text(
                    "$itemsLabel · ${w.access.name.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg,
                )
            }
        }
    }
}

@Composable
private fun CoverChip(type: CoverType, value: String?) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CardShape)
            .background(PaperWhite)
            .border(1.dp, TypeBlack, CardShape),
        contentAlignment = Alignment.Center,
    ) {
        when (type) {
            CoverType.EMOJI -> Text(value ?: "🎁", fontSize = 30.sp)
            CoverType.IMAGE -> Text("🖼", fontSize = 26.sp)
            CoverType.NONE -> Text("🎁", fontSize = 30.sp)
        }
    }
}
