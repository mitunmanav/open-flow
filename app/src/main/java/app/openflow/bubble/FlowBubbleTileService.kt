package app.openflow.bubble

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.openflow.OpenFlowApp
import app.openflow.ui.MainActivity

/**
 * Quick Settings tile: dead service → open setup; live service → hard-hide
 * the bubble via prefs.bubbleHidden (never touches user's opacity).
 */
class FlowBubbleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = application as? OpenFlowApp ?: return
        val alive = FlowAccessibilityService.instance != null
        val current = app.prefs.bubbleHidden
        when (TileTogglePolicy.nextHidden(serviceAlive = alive, hidden = current)) {
            null -> launchSetup()
            else -> {
                app.prefs.bubbleHidden = !current
                FlowAccessibilityService.instance?.applyPrefsVisual()
                updateTileState()
            }
        }
    }

    private fun launchSetup() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
            @SuppressLint("StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val a11yActive = FlowAccessibilityService.instance != null
        val app = application as? OpenFlowApp
        val hidden = app?.prefs?.bubbleHidden ?: false

        when {
            !a11yActive -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Flow Bubble (Off)"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to setup"
                }
            }
            hidden -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Flow Bubble"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Hidden"
                }
            }
            else -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Flow Bubble"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Active"
                }
            }
        }
        tile.updateTile()
    }
}
