package com.wishlist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.wishlist.shared.data.Access
import com.wishlist.shared.data.CoverType
import com.wishlist.shared.ui.navigation.Routes
import com.wishlist.shared.ui.viewmodel.CreateWishlistViewModel
import org.koin.compose.viewmodel.koinViewModel

private val EMOJIS = listOf(
    "🎁", "🎂", "🎄", "🎉", "💝", "💍", "💐", "📱",
    "💻", "🎧", "📚", "🎮", "⚽", "🏀", "🎸", "🎨",
    "🧸", "👟", "👗", "👜", "⌚", "💎", "🧁", "🍰",
    "🏠", "🚗", "✈️", "🏝", "☕", "🍷", "🌸", "🌟",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWishlistScreen(nav: NavHostController) {
    val vm: CreateWishlistViewModel = koinViewModel()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var coverType by rememberSaveable { mutableStateOf(CoverType.EMOJI) }
    var coverValue by rememberSaveable { mutableStateOf("🎁") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var access by rememberSaveable { mutableStateOf(Access.LINK) }

    LaunchedEffect(Unit) {
        if (vm.requiresLogin()) nav.navigate(Routes.AUTH)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New wishlist") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(16.dp),
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(20.dp))

            Text("Cover", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = coverType == CoverType.EMOJI,
                    onClick = { coverType = CoverType.EMOJI },
                    label = { Text("Emoji") },
                )
                FilterChip(
                    selected = coverType == CoverType.IMAGE,
                    onClick = { coverType = CoverType.IMAGE },
                    label = { Text("Image URL") },
                )
                FilterChip(
                    selected = coverType == CoverType.NONE,
                    onClick = { coverType = CoverType.NONE },
                    label = { Text("None") },
                )
            }
            Spacer(Modifier.height(12.dp))
            when (coverType) {
                CoverType.EMOJI -> EmojiPicker(selected = coverValue, onSelect = { coverValue = it })
                CoverType.IMAGE -> OutlinedTextField(
                    value = imageUrl, onValueChange = { imageUrl = it },
                    label = { Text("Image URL") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
                CoverType.NONE -> {}
            }

            Spacer(Modifier.height(20.dp))
            Text("Access", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccessChip("Link", access == Access.LINK) { access = Access.LINK }
                AccessChip("Public", access == Access.PUBLIC) { access = Access.PUBLIC }
                AccessChip("Private", access == Access.PRIVATE) { access = Access.PRIVATE }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    val v = when (coverType) {
                        CoverType.EMOJI -> coverValue
                        CoverType.IMAGE -> imageUrl
                        CoverType.NONE -> null
                    }
                    vm.create(name.trim(), coverType, v, access) { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.detail(id))
                    }
                },
                enabled = !busy && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.large,
            ) { Text("Create", fontWeight = FontWeight.SemiBold) }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AccessChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        gridItems(EMOJIS) { e ->
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (e == selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) { Text(e, fontSize = 22.sp) }
        }
    }
}
