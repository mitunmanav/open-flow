package app.openflow.bubble

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import app.openflow.OpenFlowApp
import app.openflow.ui.MainActivity

/**
 * Android Quick Settings Tile to toggle / launch Open Flow Bubble.
 */
@RequiresApi(Build.VERSION_CODES.N)
class FlowBubbleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = application as? OpenFlowApp
        val a11yActive = FlowAccessibilityService.instance != null

        if (!a11yActive) {
            // Open main activity / settings to enable accessibility
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ startActivityAndCollapse
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } else {
            // If already running, toggle bubble visibility
            app?.prefs?.let { prefs ->
                val current = prefs.bubbleOpacity
                if (current > 0.05f) {
                    prefs.bubbleOpacity = 0.0f
                } else {
                    prefs.bubbleOpacity = 0.80f
                }
            }
            updateTileState()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val a11yActive = FlowAccessibilityService.instance != null
        val app = application as? OpenFlowApp
        val visible = (app?.prefs?.bubbleOpacity ?: 0.80f) > 0.05f

        if (!a11yActive) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Flow Bubble (Off)"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Tap to setup"
            }
        } else if (visible) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Flow Bubble"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Active"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Flow Bubble"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Hidden"
            }
        }
        tile.updateTile()
    }
}
