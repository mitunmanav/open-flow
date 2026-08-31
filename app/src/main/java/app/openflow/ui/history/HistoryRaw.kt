package app.openflow.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import app.openflow.R
import app.openflow.bubble.FlowAccessibilityService

fun useHistoryRaw(ctx: Context, raw: String) {
    val said = raw.trim()
    if (said.isBlank()) return
    val svc = FlowAccessibilityService.instance
    if (svc != null) {
        svc.useRawFromHistory(said)
        return
    }
    try {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Open Flow", said))
        Toast.makeText(ctx, ctx.getString(R.string.flow_bubble_copied_clipboard), Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
    }
}
