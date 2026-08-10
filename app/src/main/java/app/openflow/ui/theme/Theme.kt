package app.openflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = OpenFlowColors.Primary,
    onPrimary = OpenFlowColors.OnPrimaryLight,
    primaryContainer = OpenFlowColors.PrimaryLight,
    secondary = OpenFlowColors.Secondary,
    secondaryContainer = OpenFlowColors.SecondaryLight,
    surface = OpenFlowColors.SurfaceLight,
    onSurface = OpenFlowColors.OnSurfaceLight,
    background = OpenFlowColors.BackgroundLight,
    onBackground = OpenFlowColors.OnBackgroundLight,
    surfaceVariant = OpenFlowColors.SurfaceVariantLight,
    onSurfaceVariant = OpenFlowColors.OnSurfaceVariantLight,
    error = OpenFlowColors.Error,
    onError = OpenFlowColors.OnError
)

private val DarkColorScheme = darkColorScheme(
    primary = OpenFlowColors.PrimaryLight,
    onPrimary = OpenFlowColors.OnPrimaryDark,
    primaryContainer = OpenFlowColors.Primary,
    secondary = OpenFlowColors.SecondaryLight,
    secondaryContainer = OpenFlowColors.Secondary,
    surface = OpenFlowColors.SurfaceDark,
    onSurface = OpenFlowColors.OnSurfaceDark,
    background = OpenFlowColors.BackgroundDark,
    onBackground = OpenFlowColors.OnBackgroundDark,
    surfaceVariant = OpenFlowColors.SurfaceVariantDark,
    onSurfaceVariant = OpenFlowColors.OnSurfaceVariantDark,
    error = OpenFlowColors.ErrorDark,
    onError = OpenFlowColors.OnError
)

@Composable
fun OpenFlowTheme(
    darkMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
