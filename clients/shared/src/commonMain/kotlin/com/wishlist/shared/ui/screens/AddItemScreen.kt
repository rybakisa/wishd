package com.wishlist.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.wishlist.shared.data.ItemCreateRequest
import com.wishlist.shared.ui.theme.AshGray
import com.wishlist.shared.ui.theme.BubblegumRed
import com.wishlist.shared.ui.theme.ButtonShape
import com.wishlist.shared.ui.theme.CardShape
import com.wishlist.shared.ui.theme.InputGray
import com.wishlist.shared.ui.theme.LeafyGreen
import com.wishlist.shared.ui.theme.LightGray
import com.wishlist.shared.ui.theme.PaperWhite
import com.wishlist.shared.ui.theme.SpeechBubbleShape
import com.wishlist.shared.ui.theme.StickerBorder
import com.wishlist.shared.ui.theme.SunshineYellow
import com.wishlist.shared.ui.theme.TypeBlack
import com.wishlist.shared.ui.viewmodel.AddItemViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
                title = {
                    Text(
                        "a new wish",
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    shape = SpeechBubbleShape,
                    color = PaperWhite,
                    contentColor = TypeBlack,
                    border = StickerBorder,
                ) {
                    Text(
                        "Drop a link, we'll fill the rest",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TypeBlack,
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "Paste a link (optional)",
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { vm.parseUrl(url) },
                        enabled = !busy && url.isNotBlank(),
                        shape = ButtonShape,
                        border = StickerBorder,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SunshineYellow,
                            contentColor = TypeBlack,
                            disabledContainerColor = LightGray,
                            disabledContentColor = AshGray,
                        ),
                        modifier = Modifier.heightIn(min = 60.dp),
                    ) { Text("Parse", style = MaterialTheme.typography.titleMedium) }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                SoftField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "What do you wish for?",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SoftField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    placeholder = "Image URL",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SoftField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "A little description…",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row {
                    SoftField(
                        value = price,
                        onValueChange = { price = it },
                        placeholder = "Price",
                        singleLine = true,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    SoftField(
                        value = currency,
                        onValueChange = { currency = it.uppercase().take(3) },
                        placeholder = "USD",
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
            item {
                SoftField(
                    value = size,
                    onValueChange = { size = it },
                    placeholder = "Size",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SoftField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = "Any notes?",
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    shape = ButtonShape,
                    border = StickerBorder,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeafyGreen,
                        contentColor = TypeBlack,
                        disabledContainerColor = LightGray,
                        disabledContentColor = AshGray,
                    ),
                ) { Text("Save this wish", style = MaterialTheme.typography.titleMedium) }
            }
            error?.let {
                item {
                    Text(
                        it,
                        color = BubblegumRed,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, color = AshGray, style = MaterialTheme.typography.bodyLarge)
        },
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = InputGray),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
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
