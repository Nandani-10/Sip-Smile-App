package com.sipsmile.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sipsmile.app.R

/** The pastel palette used all over the app. */
object Pastel {
    val Sky = Color(0xFF5BB5F0)
    val SkyDeep = Color(0xFF2F8FD6)
    val Mint = Color(0xFF8FE3CF)
    val MintDeep = Color(0xFF2E9E85)
    val Peach = Color(0xFFFFC8A8)
    val Pink = Color(0xFFFFB6D9)
    val PinkDeep = Color(0xFFE8689F)
    val Ink = Color(0xFF2D3A4A)
    val Muted = Color(0xFF7A8A9C)
    val Success = Color(0xFF7ED6A5)
    val Cream = Color(0xFFFFF8F2)
}

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_regular, FontWeight.Medium),
    Font(R.font.nunito_bold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_bold, FontWeight.ExtraBold),
)

/** The accent choices offered in Settings: key -> (label, colour). */
val AccentChoices: List<Triple<String, String, Color>> = listOf(
    Triple("sky", "Sky", Color(0xFF5BB5F0)),
    Triple("mint", "Mint", Color(0xFF2FBF9F)),
    Triple("peach", "Peach", Color(0xFFFF9A68)),
    Triple("pink", "Pink", Color(0xFFEE6FA6)),
    Triple("lavender", "Lavender", Color(0xFF9B7FE8)),
)

private fun accentColor(key: String): Color = AccentChoices.firstOrNull { it.first == key }?.third ?: AccentChoices[0].third

private fun lightColors(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    secondary = Pastel.Mint,
    onSecondary = Pastel.Ink,
    tertiary = Pastel.Peach,
    onTertiary = Pastel.Ink,
    background = Color(0xFFF5FAFF),
    onBackground = Pastel.Ink,
    surface = Color.White,
    onSurface = Pastel.Ink,
    surfaceVariant = accent.copy(alpha = 0.14f).compositeOver(Color.White),
    onSurfaceVariant = Pastel.Muted,
)

private fun darkColors(accent: Color) = darkColorScheme(
    primary = lerp(accent, Color.White, 0.25f),
    onPrimary = Color(0xFF0B1F33),
    secondary = Color(0xFF6FD1BC),
    onSecondary = Color(0xFF08251F),
    tertiary = Color(0xFFFFB28A),
    onTertiary = Color(0xFF331A0B),
    background = Color(0xFF121B26),
    onBackground = Color(0xFFEAF3FF),
    surface = Color(0xFF1C2A3A),
    onSurface = Color(0xFFEAF3FF),
    surfaceVariant = Color(0xFF243448),
    onSurfaceVariant = Color(0xFFA9B8CC),
)

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

/** True when the app is currently showing its dark look (follows the Theme setting, not only the phone). */
val LocalDarkTheme = compositionLocalOf { false }

/** [mode] is "light", "dark" or "system". */
@Composable
fun SipSmileTheme(mode: String = "light", accent: String = "sky", content: @Composable () -> Unit) {
    val dark = when (mode) {
        "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> false
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) darkColors(accentColor(accent)) else lightColors(accentColor(accent)),
            typography = Typography().withFont(Nunito),
            content = content
        )
    }
}
