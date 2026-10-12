package com.sipsmile.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

private val LightColors = lightColorScheme(
    primary = Pastel.Sky,
    onPrimary = Color.White,
    secondary = Pastel.Mint,
    onSecondary = Pastel.Ink,
    tertiary = Pastel.Peach,
    onTertiary = Pastel.Ink,
    background = Color(0xFFF5FAFF),
    onBackground = Pastel.Ink,
    surface = Color.White,
    onSurface = Pastel.Ink,
    surfaceVariant = Color(0xFFE6F4FF),
    onSurfaceVariant = Pastel.Muted,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CC8FF),
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

@Composable
fun SipSmileTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography().withFont(Nunito),
        content = content
    )
}
