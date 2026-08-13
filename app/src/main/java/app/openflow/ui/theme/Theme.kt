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

// --- Alternate soft skin (opt-in only) ---
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

// --- Ship default: light brutal (cream / charcoal / ink) ---
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
    // Distinct muted (not same as onSurface) so secondary text stays readable
    onSurfaceVariant = Color(0xFF4A4A48),
    outline = BrutalColors.Charcoal,
    outlineVariant = BrutalColors.Stone,
    error = BrutalColors.Error,
    onError = BrutalColors.OnError
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
    onSurface = Color(0xFFF5F2EB), // high-contrast body text on dark
    background = BrutalColors.CreamDark,
    onBackground = Color(0xFFF5F2EB),
    surfaceVariant = BrutalColors.StoneDark,
    // Was Stone (cream-ish) — too low contrast on dark; use muted light gray
    onSurfaceVariant = Color(0xFFC8C3B8),
    outline = Color(0xFFB8B2A6),
    outlineVariant = BrutalColors.StoneDark,
    error = BrutalColors.Error,
    onError = BrutalColors.OnError
)

private val M3Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Hard edges — product default skin. */
private val BrutalShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

/**
 * Product theme. Default = modern brutal already in codebase
 * ([VisualSkin.BRUTAL] + [BrutalColors] + cream surfaces).
 * M3 only when [skin] is explicit M3.
 */
@Composable
fun OpenFlowTheme(
    darkMode: String = "light",
    skin: VisualSkin = VisualSkin.DEFAULT,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    // Prefer light brutal; dark only if user/system dark
    val colors = when (skin) {
        VisualSkin.BRUTAL -> if (isDark) BrutalDark else BrutalLight
        VisualSkin.M3 -> if (isDark) M3Dark else M3Light
    }
    val shapes = when (skin) {
        VisualSkin.BRUTAL -> BrutalShapes
        VisualSkin.M3 -> M3Shapes
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
