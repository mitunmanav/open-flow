package app.openflow.ui.theme

import android.content.Context
import android.content.res.Configuration

object Motion {
    const val TAB_SWITCH_MS = 150
    const val CHIP_COLOR_MS = 200

    fun shouldAnimate(context: Context): Boolean {
        return (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
    }
}
