package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val HighDensityColorScheme = darkColorScheme(
    primary = HighDensityAccent,
    onPrimary = HighDensityAccentDark,
    primaryContainer = HighDensityContainer,
    onPrimaryContainer = HighDensityOnContainer,
    secondary = PurpleGrey80,
    background = HighDensityBg,
    onBackground = HighDensityText,
    surface = HighDensitySurface,
    onSurface = HighDensityText,
    surfaceVariant = HighDensityContainer,
    onSurfaceVariant = HighDensityOnContainer,
    error = UrgentBgColor,
    onError = UrgentOnBgColor,
    errorContainer = UrgentBgColor,
    onErrorContainer = UrgentOnBgColor
)

private val DarkColorScheme = HighDensityColorScheme

private val LightColorScheme = HighDensityColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force high density dark mode by default for premium feel
    dynamicColor: Boolean = false, // Disable dynamic colors to keep design theme intact
    content: @Composable () -> Unit,
) {
    val colorScheme = HighDensityColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
