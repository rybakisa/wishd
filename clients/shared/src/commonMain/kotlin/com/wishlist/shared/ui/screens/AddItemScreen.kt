package com.wishlist.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.wishlist.shared.data.ItemCreateRequest
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Paste link (optional)", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
