package app.openflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val M3Light = lightColorScheme(
    primary = OpenFlowColors.PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = OpenFlowColors.PrimaryContainerLight,
    onPrimaryContainer = OpenFlowColors.OnPrimaryContainerLight,
    secondary = OpenFlowColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = OpenFlowColors.SecondaryContainerLight,
    onSecondaryContainer = Color(0xFF075985),
    surface = OpenFlowColors.SurfaceLight,
    onSurface = OpenFlowColors.OnSurfaceLight,
    background = OpenFlowColors.BackgroundLight,
    onBackground = OpenFlowColors.OnBackgroundLight,
    surfaceVariant = OpenFlowColors.SurfaceVariantLight,
    onSurfaceVariant = OpenFlowColors.OnSurfaceVariantLight,
    outline = OpenFlowColors.CardBorderLight,
    outlineVariant = Color(0xFFCBD5E1),
    error = OpenFlowColors.Error,
    onError = Color.White
)

private val M3Dark = darkColorScheme(
    primary = OpenFlowColors.Primary,
    onPrimary = Color.White,
    primaryContainer = OpenFlowColors.PrimaryContainerDark,
    onPrimaryContainer = OpenFlowColors.OnPrimaryContainerDark,
    secondary = OpenFlowColors.SecondaryLight,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = OpenFlowColors.SecondaryContainerDark,
    onSecondaryContainer = Color(0xFFBAE6FD),
    surface = OpenFlowColors.SurfaceDark,
    onSurface = OpenFlowColors.OnSurfaceDark,
    background = OpenFlowColors.BackgroundDark,
    onBackground = OpenFlowColors.OnBackgroundDark,
    surfaceVariant = OpenFlowColors.SurfaceVariantDark,
    onSurfaceVariant = OpenFlowColors.OnSurfaceVariantDark,
    outline = OpenFlowColors.CardBorderDark,
    outlineVariant = Color(0xFF3F3F46),
    error = OpenFlowColors.ErrorDark,
    onError = Color.White
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
    outline = BrutalColors.Charcoal,
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
    outline = BrutalColors.Stone,
    error = BrutalColors.Error,
    onError = BrutalColors.Cream
)

private val M3Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

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
