package com.wishlist.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.wishlist.shared.ui.theme.BubblegumRed
import com.wishlist.shared.ui.theme.ButtonShape
import com.wishlist.shared.ui.theme.PaperWhite
import com.wishlist.shared.ui.theme.SpeechBubbleShape
import com.wishlist.shared.ui.theme.StickerBorder
import com.wishlist.shared.ui.theme.SunshineYellow
import com.wishlist.shared.ui.theme.TypeBlack
import com.wishlist.shared.ui.viewmodel.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(nav: NavHostController) {
    val vm: AuthViewModel = koinViewModel()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
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
            Modifier.fillMaxSize().padding(inner).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .rotate(-8f)
                    .clip(CircleShape)
                    .background(SunshineYellow)
                    .border(1.dp, TypeBlack, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("✨", fontSize = 48.sp, modifier = Modifier.rotate(8f))
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "make a wish",
                style = MaterialTheme.typography.headlineLarge,
                color = TypeBlack,
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = SpeechBubbleShape,
                color = PaperWhite,
                contentColor = TypeBlack,
                border = StickerBorder,
            ) {
                Text(
                    "sign in to save and share the things you've been dreaming of",
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TypeBlack,
                )
            }
            Spacer(Modifier.height(30.dp))

            StickerButton(
                text = "Continue with Apple",
                filled = true,
                enabled = !busy,
            ) { vm.signInWithApple { nav.popBackStack() } }
            Spacer(Modifier.height(12.dp))
            StickerButton(
                text = "Continue with Google",
                filled = false,
                enabled = !busy,
            ) { vm.signInWithGoogle { nav.popBackStack() } }

            Spacer(Modifier.height(20.dp))
            error?.let {
                Text(
                    it,
                    color = BubblegumRed,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StickerButton(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            shape = ButtonShape,
            border = StickerBorder,
            colors = ButtonDefaults.buttonColors(
                containerColor = BubblegumRed,
                contentColor = PaperWhite,
                disabledContainerColor = BubblegumRed,
                disabledContentColor = PaperWhite,
            ),
        ) { Text(text, style = MaterialTheme.typography.titleMedium) }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            shape = ButtonShape,
            border = StickerBorder,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PaperWhite,
                contentColor = TypeBlack,
                disabledContainerColor = PaperWhite,
                disabledContentColor = TypeBlack,
            ),
        ) { Text(text, style = MaterialTheme.typography.titleMedium) }
    }
}
