package app.openflow.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Open Flow Brand Palette — Electric Indigo & Deep Obsidian Glass.
 */
object OpenFlowColors {
    // Primary Brand Accents
    val Primary = Color(0xFF6366F1)             // Electric Indigo 500
    val PrimaryDark = Color(0xFF4F46E5)         // Indigo 600
    val PrimaryLight = Color(0xFF818CF8)        // Indigo 400
    val PrimaryContainerLight = Color(0xFFEEF2FF)
    val PrimaryContainerDark = Color(0xFF1E1B4B)
    val OnPrimaryContainerLight = Color(0xFF312E81)
    val OnPrimaryContainerDark = Color(0xFFE0E7FF)

    // Secondary / Atmospheric Accents
    val Secondary = Color(0xFF0EA5E9)           // Sky 500
    val SecondaryLight = Color(0xFF38BDF8)
    val SecondaryContainerLight = Color(0xFFE0F2FE)
    val SecondaryContainerDark = Color(0xFF082F49)

    // Surfaces & Backgrounds (Dark Mode - Deep Slate/Obsidian)
    val BackgroundDark = Color(0xFF09090B)      // Zinc 950
    val SurfaceDark = Color(0xFF121216)         // Elevated surface
    val SurfaceVariantDark = Color(0xFF18181B)  // Card surface
    val CardBorderDark = Color(0xFF27272A)      // Subtle 1dp border
    val CardBorderLight = Color(0xFFE2E8F0)

    // Surfaces & Backgrounds (Light Mode - Clean Porcelain)
    val BackgroundLight = Color(0xFFF8FAFC)     // Slate 50
    val SurfaceLight = Color(0xFFFFFFFF)        // Pure white card
    val SurfaceVariantLight = Color(0xFFF1F5F9) // Slate 100

    // High-Contrast Typography
    val OnBackgroundDark = Color(0xFFFAFAFA)
    val OnSurfaceDark = Color(0xFFF4F4F5)
    val OnSurfaceVariantDark = Color(0xFFA1A1AA) // Zinc 400

    val OnBackgroundLight = Color(0xFF09090B)
    val OnSurfaceLight = Color(0xFF0F172A)
    val OnSurfaceVariantLight = Color(0xFF64748B) // Slate 500

    // States & Accents
    val Success = Color(0xFF10B981)             // Emerald 500
    val SuccessContainer = Color(0xFF064E3B)    // dark surfaces
    val SuccessContainerLight = Color(0xFFD1FAE5) // light surfaces
    val OnSuccess = Color(0xFFFFFFFF)
    val Warning = Color(0xFFF59E0B)             // Amber 500
    val Error = Color(0xFFEF4444)               // Rose 500
    val ErrorDark = Color(0xFFF87171)
    val OnError = Color(0xFFFFFFFF)

    // Bubble State Indicators
    val BubbleActiveGlow = Color(0xFF6366F1)
    val BubbleActiveRing = Color(0x66818CF8)
    val BubbleIdleDark = Color(0xFF18181B)
}
