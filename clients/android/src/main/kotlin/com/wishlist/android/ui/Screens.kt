package com.wishlist.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wishlist.android.ui.viewmodel.AddItemViewModel
import com.wishlist.android.ui.viewmodel.AuthViewModel
import com.wishlist.android.ui.viewmodel.CreateWishlistViewModel
import com.wishlist.android.ui.viewmodel.HomeViewModel
import com.wishlist.android.ui.viewmodel.WishlistDetailViewModel
import com.wishlist.shared.data.Access
import com.wishlist.shared.data.AuthProvider
import com.wishlist.shared.data.CoverType
import com.wishlist.shared.data.ItemCreateRequest
import com.wishlist.shared.data.Wishlist
import com.wishlist.shared.data.WishlistItem
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private object Routes {
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

// ============ HOME ============
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
                    Text("+", fontSize = 72.sp, color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Thin)
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

// ============ AUTH ============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(nav: NavHostController) {
    val vm: AuthViewModel = koinViewModel()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var showEmailDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
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
            Modifier.fillMaxSize().padding(inner).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Wishlist", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in to share and sync",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))

            BigBlockButton(
                "Continue with Apple", fill = true, enabled = !busy,
            ) { vm.login(AuthProvider.APPLE, "apple-user@privaterelay.appleid.com", "Apple User") { nav.popBackStack() } }
            Spacer(Modifier.height(12.dp))
            BigBlockButton(
                "Continue with Google", fill = false, enabled = !busy,
            ) { vm.login(AuthProvider.GOOGLE, "google-user@gmail.com", "Google User") { nav.popBackStack() } }
            Spacer(Modifier.height(12.dp))
            BigBlockButton(
                "Continue with Email", fill = false, enabled = !busy,
            ) { showEmailDialog = true }

            Spacer(Modifier.height(16.dp))
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (showEmailDialog) {
        EmailLoginDialog(
            busy = busy,
            onDismiss = { showEmailDialog = false },
            onConfirm = { email ->
                vm.login(AuthProvider.EMAIL, email) {
                    showEmailDialog = false
                    nav.popBackStack()
                }
            },
        )
    }
}

@Composable
private fun BigBlockButton(text: String, fill: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (fill) {
        Button(
            onClick = onClick, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
            ),
        ) { Text(text, fontWeight = FontWeight.SemiBold) }
    } else {
        OutlinedButton(
            onClick = onClick, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = MaterialTheme.shapes.large,
        ) { Text(text, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun EmailLoginDialog(busy: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter your email") },
        text = {
            OutlinedTextField(
                value = email, onValueChange = { email = it }, singleLine = true,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(email.trim()) }, enabled = !busy && email.contains("@")) {
                Text("Continue")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ============ CREATE WISHLIST ============
private val EMOJIS = listOf(
    "🎁", "🎂", "🎄", "🎉", "💝", "💍", "💐", "📱",
    "💻", "🎧", "📚", "🎮", "⚽", "🏀", "🎸", "🎨",
    "🧸", "👟", "👗", "👜", "⌚", "💎", "🧁", "🍰",
    "🏠", "🚗", "✈️", "🏝", "☕", "🍷", "🌸", "🌟",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

@OptIn(ExperimentalFoundationApi::class)
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

// ============ DETAIL ============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistDetailScreen(wishlistId: String, nav: NavHostController) {
    val vm: WishlistDetailViewModel = koinViewModel(parameters = { parametersOf(wishlistId) })
    val w by vm.wishlist.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                        IconButton(onClick = { shareWishlist(context, wishlist.shareToken) }) {
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

private fun shareWishlist(context: Context, shareToken: String) {
    val url = "https://wishlist.app/s/$shareToken"
    // Copy to clipboard
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("wishlist", url))
    // Share intent
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, "Share wishlist").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

// ============ ADD ITEM ============
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(wishlistId: String, nav: NavHostController) {
    val vm: AddItemViewModel = koinViewModel(parameters = { parametersOf(wishlistId) })
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val parsed by vm.parsed.collectAsState()

    var url by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("USD") }
    var size by rememberSaveable { mutableStateOf("") }
    var comment by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(parsed) {
        parsed?.let {
            name = it.name
            imageUrl = it.imageUrl.orEmpty()
            description = it.description.orEmpty()
            price = it.price?.toString().orEmpty()
            currency = it.currency ?: currency
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add item") },
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Paste link (optional)", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        label = { Text("URL") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.parseUrl(url) },
                        enabled = !busy && url.isNotBlank(),
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("Parse") }
                }
            }
            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name *") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                )
            }
            item {
                OutlinedTextField(
                    value = imageUrl, onValueChange = { imageUrl = it },
                    label = { Text("Image URL") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                )
            }
            item {
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                )
            }
            item {
                Row {
                    OutlinedTextField(
                        value = price, onValueChange = { price = it },
                        label = { Text("Price") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium,
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = currency, onValueChange = { currency = it.uppercase().take(3) },
                        label = { Text("Currency") }, singleLine = true,
                        modifier = Modifier.width(120.dp), shape = MaterialTheme.shapes.medium,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = size, onValueChange = { size = it },
                    label = { Text("Size") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                )
            }
            item {
                OutlinedTextField(
                    value = comment, onValueChange = { comment = it },
                    label = { Text("Comment") },
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        vm.save(
                            ItemCreateRequest(
                                name = name.trim(),
                                url = url.ifBlank { null },
                                imageUrl = imageUrl.ifBlank { null },
                                description = description.ifBlank { null },
                                price = price.toDoubleOrNull(),
                                currency = currency.ifBlank { "USD" },
                                size = size.ifBlank { null },
                                comment = comment.ifBlank { null },
                            )
                        ) { nav.popBackStack() }
                    },
                    enabled = !busy && name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.large,
                ) { Text("Save item", fontWeight = FontWeight.SemiBold) }
            }
            error?.let {
                item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
