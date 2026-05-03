package com.wishlist.shared.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Fictional palette
val CanvasAlmond = Color(0xFFFFE9CE)
val PaperWhite = Color(0xFFFFFFFF)
val TypeBlack = Color(0xFF000000)
val GrapePunch = Color(0xFF8A53FF)
val BubblegumRed = Color(0xFFFD4B38)
val SunshineYellow = Color(0xFFFFD80C)
val LeafyGreen = Color(0xFF3CCB09)
val DeepIndigo = Color(0xFF0500A3)
val AshGray = Color(0xFF666666)
val LightGray = Color(0xFFDDDDDD)
val InputGray = Color(0xFF101010)

// Dark variants — warm darks; accents kept vivid
private val DarkCanvas = Color(0xFF1A140B)
private val DarkPaper = Color(0xFF2A211A)
private val DarkInk = Color(0xFFFFE9CE)
private val DarkSubtle = Color(0xFFB6AC9A)
private val DarkLine = Color(0xFFFFE9CE)
private val DarkSurfaceVariant = Color(0xFF3A2F25)

private val LightScheme = lightColorScheme(
    primary = GrapePunch,
    onPrimary = PaperWhite,
    primaryContainer = GrapePunch,
    onPrimaryContainer = PaperWhite,
    secondary = BubblegumRed,
    onSecondary = PaperWhite,
    tertiary = SunshineYellow,
    onTertiary = TypeBlack,
    background = CanvasAlmond,
    onBackground = TypeBlack,
    surface = PaperWhite,
    onSurface = TypeBlack,
    surfaceVariant = LightGray,
    onSurfaceVariant = AshGray,
    outline = TypeBlack,
    outlineVariant = LightGray,
    error = BubblegumRed,
    onError = PaperWhite,
)

private val DarkScheme = darkColorScheme(
    primary = GrapePunch,
    onPrimary = PaperWhite,
    primaryContainer = GrapePunch,
    onPrimaryContainer = PaperWhite,
    secondary = BubblegumRed,
    onSecondary = PaperWhite,
    tertiary = SunshineYellow,
    onTertiary = TypeBlack,
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkPaper,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSubtle,
    outline = DarkLine,
    outlineVariant = DarkSurfaceVariant,
    error = BubblegumRed,
    onError = PaperWhite,
)

// Shapes — sticker style: 5px buttons, 15px cards, 144px speech bubbles
private val appShapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(15.dp),
    large = RoundedCornerShape(15.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val SpeechBubbleShape = RoundedCornerShape(144.dp)
val CardShape = RoundedCornerShape(15.dp)
val ButtonShape = RoundedCornerShape(5.dp)
val StickerBorder = BorderStroke(1.dp, TypeBlack)

// Typography — single weight handwritten via FontFamily.Cursive
private val Hand = FontFamily.Cursive
private val LS = 0.012.em

private val appTypography = Typography(
    displayLarge = TextStyle(fontFamily = Hand, fontSize = 72.sp, fontWeight = FontWeight.Normal, lineHeight = 79.sp, letterSpacing = LS),
    displayMedium = TextStyle(fontFamily = Hand, fontSize = 48.sp, fontWeight = FontWeight.Normal, lineHeight = 58.sp, letterSpacing = LS),
    displaySmall = TextStyle(fontFamily = Hand, fontSize = 36.sp, fontWeight = FontWeight.Normal, lineHeight = 43.sp, letterSpacing = LS),
    headlineLarge = TextStyle(fontFamily = Hand, fontSize = 48.sp, fontWeight = FontWeight.Normal, lineHeight = 58.sp, letterSpacing = LS),
    headlineMedium = TextStyle(fontFamily = Hand, fontSize = 36.sp, fontWeight = FontWeight.Normal, lineHeight = 43.sp, letterSpacing = LS),
    headlineSmall = TextStyle(fontFamily = Hand, fontSize = 29.sp, fontWeight = FontWeight.Normal, lineHeight = 38.sp, letterSpacing = LS),
    titleLarge = TextStyle(fontFamily = Hand, fontSize = 29.sp, fontWeight = FontWeight.Normal, lineHeight = 38.sp, letterSpacing = LS),
    titleMedium = TextStyle(fontFamily = Hand, fontSize = 22.sp, fontWeight = FontWeight.Normal, lineHeight = 31.sp, letterSpacing = LS),
    titleSmall = TextStyle(fontFamily = Hand, fontSize = 22.sp, fontWeight = FontWeight.Normal, lineHeight = 31.sp, letterSpacing = LS),
    bodyLarge = TextStyle(fontFamily = Hand, fontSize = 22.sp, fontWeight = FontWeight.Normal, lineHeight = 31.sp, letterSpacing = LS),
    bodyMedium = TextStyle(fontFamily = Hand, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 27.sp, letterSpacing = LS),
    bodySmall = TextStyle(fontFamily = Hand, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 27.sp, letterSpacing = LS),
    labelLarge = TextStyle(fontFamily = Hand, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 27.sp, letterSpacing = LS),
    labelMedium = TextStyle(fontFamily = Hand, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 27.sp, letterSpacing = LS),
    labelSmall = TextStyle(fontFamily = Hand, fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 27.sp, letterSpacing = LS),
)

// Field names preserved for backwards compatibility with screens — values
// remapped to the Fictional palette.
data class WishlistAccents(
    val apricot: Color,
    val lavender: Color,
    val mint: Color,
    val butter: Color,
    val blush: Color,
    val sky: Color,
) {
    fun pickFor(seed: Any?): Color {
        val list = listOf(apricot, lavender, mint, butter, blush, sky)
        val h = (seed?.hashCode() ?: 0)
        return list[((h % list.size) + list.size) % list.size]
    }
}

private val LightAccents = WishlistAccents(
    apricot = GrapePunch,
    lavender = BubblegumRed,
    mint = LeafyGreen,
    butter = SunshineYellow,
    blush = DeepIndigo,
    sky = GrapePunch,
)

private val DarkAccents = LightAccents

val LocalWishlistAccents = staticCompositionLocalOf { LightAccents }

// Kept for source compatibility — italic dropped (single weight per spec).
val TitleItalic = TextStyle(
    fontFamily = Hand,
    fontSize = 36.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 43.sp,
    letterSpacing = LS,
)

fun contrastingText(bg: Color): Color =
    if (bg == SunshineYellow || bg == LeafyGreen || bg == CanvasAlmond) TypeBlack
    else PaperWhite

@Composable
fun WishlistTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = appTypography,
        shapes = appShapes,
    ) {
        CompositionLocalProvider(
            LocalWishlistAccents provides if (dark) DarkAccents else LightAccents,
            content = content,
        )
    }
}
