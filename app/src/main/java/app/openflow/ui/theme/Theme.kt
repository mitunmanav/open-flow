package app.openflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val M3Light = lightColorScheme(
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

private val M3Dark = darkColorScheme(
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

private val BrutalLight = lightColorScheme(
    primary = BrutalColors.Charcoal,
    onPrimary = BrutalColors.OnCharcoal,
    primaryContainer = BrutalColors.Stone,
    onPrimaryContainer = BrutalColors.Charcoal,
    secondary = BrutalColors.Ink,
    onSecondary = BrutalColors.OnCharcoal,
    secondaryContainer = BrutalColors.Stone,
    surface = BrutalColors.Cream,
    onSurface = BrutalColors.OnCream,
    background = BrutalColors.Cream,
    onBackground = BrutalColors.OnCream,
    surfaceVariant = BrutalColors.Stone,
    onSurfaceVariant = BrutalColors.Charcoal,
    error = BrutalColors.Error,
    onError = BrutalColors.OnCharcoal
)

private val BrutalDark = darkColorScheme(
    primary = BrutalColors.Cream,
    onPrimary = BrutalColors.Charcoal,
    primaryContainer = BrutalColors.StoneDark,
    onPrimaryContainer = BrutalColors.Cream,
    secondary = BrutalColors.InkLight,
    onSecondary = BrutalColors.Charcoal,
    secondaryContainer = BrutalColors.StoneDark,
    surface = BrutalColors.CreamDark,
    onSurface = BrutalColors.Cream,
    background = BrutalColors.CreamDark,
    onBackground = BrutalColors.Cream,
    surfaceVariant = BrutalColors.StoneDark,
    onSurfaceVariant = BrutalColors.Stone,
    error = BrutalColors.Error,
    onError = BrutalColors.Cream
)

private val M3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Near-square corners for subtle brutal. */
private val BrutalShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(4.dp),
    extraLarge = RoundedCornerShape(6.dp)
)

@Composable
fun OpenFlowTheme(
    darkMode: String = "system",
    skin: VisualSkin = VisualSkin.M3,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val colors = when (skin) {
        VisualSkin.M3 -> if (isDark) M3Dark else M3Light
        VisualSkin.BRUTAL -> if (isDark) BrutalDark else BrutalLight
    }
    val shapes = when (skin) {
        VisualSkin.M3 -> M3Shapes
        VisualSkin.BRUTAL -> BrutalShapes
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
