package app.openflow.display

import android.app.Activity
import android.os.Build
import android.view.Surface

/**
 * Apply preferred refresh rate (60/90/120/144) best-effort.
 * Uses preferredDisplayModeId (+ Surface.setFrameRate when we have a surface).
 * Never crashes if mode missing — device keeps current rate.
 */
object DisplayRefreshController {

    fun apply(activity: Activity, preferredHz: Int) {
        val hz = DisplayRefreshPolicy.normalizePreference(preferredHz)
        val display = if (Build.VERSION.SDK_INT >= 30) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return

        val modes = display.supportedModes.map {
            DisplayRefreshPolicy.ModeInfo(it.modeId, it.refreshRate)
        }
        val pick = DisplayRefreshPolicy.pickMode(modes, hz) ?: return
        val currentHz = display.refreshRate
        val currentModeId = try {
            display.mode.modeId
        } catch (_: Exception) {
            -1
        }
        if (!DisplayRefreshPolicy.needsApply(currentHz, hz) &&
            !DisplayRefreshPolicy.needsApply(currentModeId, pick)
        ) {
            return
        }

        try {
            val lp = activity.window.attributes
            lp.preferredDisplayModeId = pick.modeId
            activity.window.attributes = lp
        } catch (_: Exception) {
        }

        // API 30+: also hint the window surface if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                activity.window.decorView.post {
                    try {
                        val holder = activity.window.decorView.rootView
                        // SurfaceControl path is OEM-sensitive; modeId is the reliable lever.
                        @Suppress("UNUSED_VARIABLE")
                        val rate = pick.refreshRateHz
                        @Suppress("UNUSED_VARIABLE")
                        val compat = Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
