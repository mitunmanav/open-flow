package app.openflow.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Snappy motion. Short ms. No soft flourish.
 * Respect system animator scale (0 = reduced motion → skip).
 */
object Motion {
    const val TAB_SWITCH_MS = 90
    const val CHIP_COLOR_MS = 80
    const val ENTER_MS = 100
    const val EXIT_MS = 80
    const val FADE_MS = 60

    fun shouldAnimate(context: Context): Boolean = animatorScale(context) > 0f

    fun animatorScale(context: Context): Float {
        return try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (_: Settings.SettingNotFoundException) {
            1f
        } catch (_: SecurityException) {
            1f
        }
    }

    fun durationMs(context: Context, baseMs: Int): Int {
        val scale = animatorScale(context)
        if (scale <= 0f || baseMs <= 0) return 0
        return (baseMs * scale).toInt().coerceAtLeast(0)
    }
}

@Composable
fun rememberShouldAnimate(): Boolean {
    val context = LocalContext.current
    return remember(context) { Motion.shouldAnimate(context) }
}

@Composable
fun rememberMotionMs(baseMs: Int): Int {
    val context = LocalContext.current
    return remember(context, baseMs) { Motion.durationMs(context, baseMs) }
}
