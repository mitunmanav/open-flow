package app.openflow.ui.a11y

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing + touch. Hard-edge product (light brutal).
 * Soft 20dp cards demoted — prefer low rounding.
 */
object Dimen {
    val Space0: Dp = 0.dp
    val Space1: Dp = 4.dp
    val Space2: Dp = 8.dp
    val Space3: Dp = 12.dp
    val Space4: Dp = 16.dp
    val Space5: Dp = 20.dp
    val Space6: Dp = 24.dp
    val Space8: Dp = 32.dp
    val Space10: Dp = 40.dp
    val Space12: Dp = 48.dp

    /** Min Material 48; 52 for finger comfort. */
    val TOUCH_TARGET: Dp = 52.dp
    val MIN_TOUCH: Dp = 48.dp

    val MIN_PADDING: Dp = Space4
    val PAGE_PAD: Dp = Space5
    val GAP: Dp = Space3
    val GAP_SM: Dp = Space2
    val GAP_LG: Dp = Space4

    val ICON_SIZE: Dp = 24.dp
    val ICON_SIZE_SM: Dp = 20.dp
    val ICON_SIZE_LG: Dp = 28.dp

    val HAIRLINE: Dp = 1.dp
    val DIVIDER: Dp = 1.dp
    val BORDER: Dp = 2.dp

    val CARD_ELEVATION: Dp = 0.dp
    /** Hard-edge default (was soft 20). */
    val CARD_ROUNDING: Dp = 4.dp
    val BUTTON_ROUNDING: Dp = 2.dp
    val CHIP_ROUNDING: Dp = 2.dp
    val FIELD_ROUNDING: Dp = 2.dp
}
