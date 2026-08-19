package app.openflow.ui.shell

import androidx.compose.ui.unit.Dp
import app.openflow.ui.a11y.Dimen

/** Five-tab bar: short labels + tight selected pad. A11y keeps full names. */
object NavBarTokens {
    val tabMin: Dp = Dimen.MIN_TOUCH
    val iconMin: Dp = Dimen.TOUCH_TARGET
    val selectedPadH: Dp = Dimen.Space2
    val selectedPadV: Dp = Dimen.Space1
    val tabPadV: Dp = Dimen.Space1

    fun shortLabel(route: AppRoute): String = when (route) {
        AppRoute.Home -> "Home"
        AppRoute.Dictionary -> "Dict"
        AppRoute.Snippets -> "Snips"
        AppRoute.Style -> "Style"
        AppRoute.Insights -> "Stats"
        else -> route.title.take(6)
    }
}
