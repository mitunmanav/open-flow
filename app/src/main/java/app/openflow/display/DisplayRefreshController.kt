package app.openflow.display

import android.app.Activity
import android.os.Build
import android.view.Surface
import app.openflow.BuildConfig

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
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("DisplayRefresh", "failed to read current modeId", e)
            }
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
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("DisplayRefresh", "failed to set preferredDisplayModeId", e)
            }
        }
    }
}
