package com.wishlist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.wishlist.shared.data.Access
import com.wishlist.shared.data.CoverType
import com.wishlist.shared.ui.navigation.Auth
import com.wishlist.shared.ui.navigation.Detail
import com.wishlist.shared.ui.theme.AshGray
import com.wishlist.shared.ui.theme.BubblegumRed
import com.wishlist.shared.ui.theme.ButtonShape
import com.wishlist.shared.ui.theme.CardShape
import com.wishlist.shared.ui.theme.GrapePunch
import com.wishlist.shared.ui.theme.InputGray
import com.wishlist.shared.ui.theme.LightGray
import com.wishlist.shared.ui.theme.LocalWishlistAccents
import com.wishlist.shared.ui.theme.PaperWhite
import com.wishlist.shared.ui.theme.StickerBorder
import com.wishlist.shared.ui.theme.TypeBlack
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
        if (vm.requiresLogin()) nav.navigate(Auth)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "something new",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TypeBlack,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TypeBlack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            SectionLabel("What's it for?")
            Spacer(Modifier.height(12.dp))
            SoftTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Birthday, moving in, just because…",
                singleLine = true,
            )
            Spacer(Modifier.height(30.dp))

            SectionLabel("Cover")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyChip("Emoji", coverType == CoverType.EMOJI) { coverType = CoverType.EMOJI }
                ChunkyChip("Image", coverType == CoverType.IMAGE) { coverType = CoverType.IMAGE }
                ChunkyChip("None", coverType == CoverType.NONE) { coverType = CoverType.NONE }
            }
            Spacer(Modifier.height(16.dp))
            when (coverType) {
                CoverType.EMOJI -> EmojiPicker(selected = coverValue, onSelect = { coverValue = it })
                CoverType.IMAGE -> SoftTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    placeholder = "https://…",
                    singleLine = true,
                )
                CoverType.NONE -> {}
            }

            Spacer(Modifier.height(30.dp))
            SectionLabel("Who can see it?")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChunkyChip("With a link", access == Access.LINK) { access = Access.LINK }
                ChunkyChip("Everyone", access == Access.PUBLIC) { access = Access.PUBLIC }
                ChunkyChip("Just me", access == Access.PRIVATE) { access = Access.PRIVATE }
            }

            Spacer(Modifier.height(30.dp))
            Button(
                onClick = {
                    val v = when (coverType) {
                        CoverType.EMOJI -> coverValue
                        CoverType.IMAGE -> imageUrl
                        CoverType.NONE -> null
                    }
                    vm.create(name.trim(), coverType, v, access) { id ->
                        nav.popBackStack()
                        nav.navigate(Detail(id = id))
                    }
                },
                enabled = !busy && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                shape = ButtonShape,
                border = StickerBorder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrapePunch,
                    contentColor = PaperWhite,
                    disabledContainerColor = LightGray,
                    disabledContentColor = AshGray,
                ),
            ) { Text("Let's go", style = MaterialTheme.typography.titleMedium) }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = BubblegumRed, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = TypeBlack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = AshGray, style = MaterialTheme.typography.bodyLarge)
        },
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = InputGray),
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PaperWhite,
            unfocusedContainerColor = PaperWhite,
            focusedBorderColor = TypeBlack,
            unfocusedBorderColor = TypeBlack,
            cursorColor = TypeBlack,
        ),
    )
}

@Composable
private fun ChunkyChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) GrapePunch else LightGray
    val fg = if (selected) PaperWhite else TypeBlack
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = bg,
        contentColor = fg,
        border = StickerBorder,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    val accents = LocalWishlistAccents.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        gridItems(EMOJIS) { e ->
            val isSelected = e == selected
            val bg = if (isSelected) accents.pickFor(e) else PaperWhite
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .rotate(if (isSelected) -4f else 0f)
                    .clip(CardShape)
                    .background(bg)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else TypeBlack,
                        shape = CardShape,
                    )
                    .clickable { onSelect(e) },
                contentAlignment = Alignment.Center,
            ) { Text(e, fontSize = 24.sp) }
        }
    }
}
