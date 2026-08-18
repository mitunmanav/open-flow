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
    onSurfaceVariant = Color(0xFFD8D3C8),
    outline = Color(0xFFB8B2A6),
    outlineVariant = BrutalColors.StoneDark,
    error = BrutalColors.Error,
    onError = BrutalColors.OnError
)

// 0.dp = hard rect. M3 Shapes slots require CornerBasedShape (not RectangleShape).
private fun cornerShape(skin: VisualSkin, slot: SkinShapes.Slot) =
    RoundedCornerShape(SkinShapes.cornerDp(skin, slot).dp)

private fun shapesFor(skin: VisualSkin) = Shapes(
    extraSmall = cornerShape(skin, SkinShapes.Slot.EXTRA_SMALL),
    small = cornerShape(skin, SkinShapes.Slot.SMALL),
    medium = cornerShape(skin, SkinShapes.Slot.MEDIUM),
    large = cornerShape(skin, SkinShapes.Slot.LARGE),
    extraLarge = cornerShape(skin, SkinShapes.Slot.EXTRA_LARGE)
)

/**
 * Product theme. Default = modern brutal already in codebase
 * ([VisualSkin.BRUTAL] + [BrutalColors] + cream surfaces).
 * M3 only when [skin] is explicit M3.
 */
private fun applyPalette(
    base: androidx.compose.material3.ColorScheme,
    p: AppearancePalette,
): androidx.compose.material3.ColorScheme = base.copy(
    background = Color(p.backgroundArgb),
    onBackground = Color(p.textArgb),
    surface = Color(p.cardsArgb),
    onSurface = Color(p.textArgb),
    primary = Color(p.accentArgb),
    outline = Color(p.borderArgb),
)

@Composable
fun OpenFlowTheme(
    darkMode: String = "light",
    skin: VisualSkin = VisualSkin.DEFAULT,
    palette: AppearancePalette = AppearancePalette.factory(false),
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
    val shapes = shapesFor(skin)

    MaterialTheme(
        colorScheme = applyPalette(colors, palette),
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
