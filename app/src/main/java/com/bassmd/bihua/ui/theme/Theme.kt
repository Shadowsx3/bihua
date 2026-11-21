package com.bassmd.bihua.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bassmd.bihua.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = Sky,
    onPrimary = LightPalette.inkDark,
    primaryContainer = Color(0xFFDCEEDF),
    onPrimaryContainer = LightPalette.inkDark,
    secondary = Lilac,
    onSecondary = LightPalette.inkDark,
    secondaryContainer = Color(0xFFEADFF6),
    onSecondaryContainer = LightPalette.inkDark,
    tertiary = Mint,
    onTertiary = LightPalette.inkDark,
    tertiaryContainer = Color(0xFFD7F0E5),
    onTertiaryContainer = LightPalette.inkDark,
    background = LightPalette.paper,
    onBackground = LightPalette.inkDark,
    surface = LightPalette.paper,
    onSurface = LightPalette.inkDark,
    surfaceVariant = Color(0xFFE9EBD9),
    onSurfaceVariant = LightPalette.inkSoft,
    surfaceContainerLowest = Color(0xFFFFFCF1),
    surfaceContainerLow = LightPalette.paper,
    surfaceContainer = LightPalette.paperWarm,
    surfaceContainerHigh = Color(0xFFEDEAD6),
    surfaceContainerHighest = Color(0xFFE5E2CC),
    outline = LightPalette.gridLine,
    outlineVariant = Color(0xFFDDE1C9),
    error = Rose,
    onError = LightPalette.inkDark,
    errorContainer = Color(0xFFFFE2E8),
    onErrorContainer = LightPalette.inkDark,
)

private val DarkColors = darkColorScheme(
    primary = Sky,
    onPrimary = Color(0xFF0B1A20),
    primaryContainer = Color(0xFF24463A),
    onPrimaryContainer = DarkPalette.inkDark,
    secondary = Lilac,
    onSecondary = Color(0xFF1A1224),
    secondaryContainer = Color(0xFF392B4A),
    onSecondaryContainer = DarkPalette.inkDark,
    tertiary = Mint,
    onTertiary = Color(0xFF0C2017),
    tertiaryContainer = Color(0xFF214536),
    onTertiaryContainer = DarkPalette.inkDark,
    background = DarkPalette.paper,
    onBackground = DarkPalette.inkDark,
    surface = DarkPalette.paper,
    onSurface = DarkPalette.inkDark,
    surfaceVariant = Color(0xFF2A372E),
    onSurfaceVariant = DarkPalette.inkSoft,
    surfaceContainerLowest = Color(0xFF0B110C),
    surfaceContainerLow = DarkPalette.paper,
    surfaceContainer = DarkPalette.paperWarm,
    surfaceContainerHigh = Color(0xFF202C23),
    surfaceContainerHighest = Color(0xFF29372D),
    outline = DarkPalette.gridLine,
    outlineVariant = Color(0xFF2F3D33),
    error = Rose,
    onError = Color(0xFF2A0D14),
    errorContainer = Color(0xFF4B202A),
    onErrorContainer = DarkPalette.inkDark,
)

private val BihuaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun BihuaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(
        LocalBihuaPalette provides if (dark) DarkPalette else LightPalette,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = BihuaTypography,
            shapes = BihuaShapes,
            content = content,
        )
    }
}
