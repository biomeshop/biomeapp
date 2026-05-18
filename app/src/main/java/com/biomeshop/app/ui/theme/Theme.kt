package com.biomeshop.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BiomeShopColorScheme = darkColorScheme(
    primary = AccentPurple,
    secondary = AccentGold,
    tertiary = AccentCyan,
    background = Night,
    surface = SurfaceGlass,
    onPrimary = TextPrimary,
    onSecondary = NightDeep,
    onTertiary = NightDeep,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun BiomeShopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BiomeShopColorScheme,
        typography = Typography,
        content = content,
    )
}
