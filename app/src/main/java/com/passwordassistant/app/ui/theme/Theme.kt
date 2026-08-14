package com.passwordassistant.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.passwordassistant.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF00696E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F7),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6365),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8EA),
    onSecondaryContainer = Color(0xFF051F21),
    tertiary = Color(0xFF515E7E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7E2FF),
    onTertiaryContainer = Color(0xFF0D1B38),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F4849),
    outline = Color(0xFF6F7979),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4DA),
    onPrimary = Color(0xFF00373A),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF9CF1F7),
    secondary = Color(0xFFB0CCCE),
    onSecondary = Color(0xFF1B3436),
    secondaryContainer = Color(0xFF324B4D),
    onSecondaryContainer = Color(0xFFCCE8EA),
    tertiary = Color(0xFFBAC6EA),
    onTertiary = Color(0xFF23304E),
    tertiaryContainer = Color(0xFF3A4765),
    onTertiaryContainer = Color(0xFFD7E2FF),
    background = Color(0xFF191C1C),
    onBackground = Color(0xFFE0E3E2),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE0E3E2),
    surfaceVariant = Color(0xFF3F4849),
    onSurfaceVariant = Color(0xFFBEC8C9),
    outline = Color(0xFF899293),
)

@Composable
fun PasswordAssistantTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
