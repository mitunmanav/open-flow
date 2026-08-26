package app.openflow.bubble

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import app.openflow.R
import app.openflow.prefs.FlowPrefs
import java.io.File

/**
 * Paints every bubble chrome state (idle / listen / post-stop chips) from
 * prefs + palette. Pure view work — no session or STT logic lives here.
 */
class BubbleVisualPainter(
    private val win: BubbleWindowController,
    private val env: Env,
) {
    interface Env {
        val prefs: FlowPrefs?
        val listening: Boolean
        fun postStopActive(): Boolean
        fun density(): Float
        val filesDir: File
        val contentResolver: android.content.ContentResolver
    }

    private val density: Float
        get() = env.density()

    fun paint() {
        val p = env.prefs ?: return
        val root = win.bubbleRoot ?: return
        val icon = win.bubbleIcon ?: return
        val label = win.bubbleLabel ?: return
        val pulseRing = win.bubblePulseRing ?: return
        val cancel = win.bubbleCancel
        val shape = FlowPrefs.normalizeBubbleShape(p.bubbleShape)

        val mode = FlowPrefs.normalizeBubbleMode(p.bubbleMode)
        val orbDp = when (mode) {
            "dot" -> 40f
            "compact" -> 44f
            else -> 48f
        }
        val stroke = BubbleChrome.strokePx(density)
        val pal = p.palette()
        val fill = if (env.listening) pal.bubbleListenArgb else pal.bubbleIdleArgb
        val on = pal.bubbleTextArgb

        if (env.listening) {
            root.background = null
            root.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            root.setPadding(0, 0, 0, 0)

            cancel?.visibility = if (p.bubbleShowCancel) View.VISIBLE else View.GONE
            cancel?.setColorFilter(on)
            win.bubbleDone?.visibility = if (p.bubbleShowDone) View.VISIBLE else View.GONE
            win.bubbleDone?.setColorFilter(on)
            win.bubbleWave?.visibility = View.VISIBLE
            paintListenDisc(cancel, fill, on, stroke)
            paintListenDisc(win.bubbleDone, fill, on, stroke)
            paintListenDisc(win.bubbleWave, fill, on, stroke)
            applyWaveFill(on)
            icon.visibility = View.GONE
            icon.layoutParams = LinearLayout.LayoutParams(
                (18f * density).toInt(),
                (18f * density).toInt()
            ).apply { gravity = Gravity.CENTER }
            label.visibility = View.GONE

            // Pulse WRAP_CONTENT was the grey veil. Idle-only; never on listen.
            pulseRing.visibility = View.GONE
            win.bubbleChipCopy?.visibility = View.GONE
            win.bubbleChipUndo?.visibility = View.GONE
            win.bubbleChipPaste?.visibility = View.GONE
        } else if (env.postStopActive()) {
            cancel?.visibility = View.GONE
            win.bubbleDone?.visibility = View.GONE
            win.bubbleWave?.visibility = View.GONE
            label.visibility = View.GONE
            pulseRing.visibility = View.GONE
            val bg = GradientDrawable().apply {
                this.shape = GradientDrawable.RECTANGLE
                cornerRadius = BubbleChrome.cornerPx("listen", density, p.bubbleRoundPct)
                setColor(fill)
                setStroke(stroke, on)
            }
            root.background = bg
            root.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            val padH = (BubbleTouch.PAD_H_DP * density).toInt()
            val padV = (BubbleTouch.PAD_V_DP * density).toInt()
            root.setPadding(padH, padV, padH, padV)
            icon.visibility = View.VISIBLE
            icon.layoutParams = LinearLayout.LayoutParams(
                (18f * density).toInt(),
                (18f * density).toInt()
            ).apply { gravity = Gravity.CENTER }
            listOf(win.bubbleChipCopy, win.bubbleChipUndo, win.bubbleChipPaste).forEach { chip ->
                chip?.setTextColor(on)
            }
        } else {
            cancel?.visibility = View.GONE
            win.bubbleDone?.visibility = View.GONE
            win.bubbleWave?.visibility = View.GONE
            label.visibility = View.GONE
            pulseRing.visibility = View.GONE
            win.bubbleChipCopy?.visibility = View.GONE
            win.bubbleChipUndo?.visibility = View.GONE
            win.bubbleChipPaste?.visibility = View.GONE

            val (w, h) = BubbleGeometry.overlaySizePx(listening = false, density = density, shape = shape)
            root.layoutParams = FrameLayout.LayoutParams(w, h, Gravity.CENTER)
            root.setPadding(0, 0, 0, 0)
            val useOval = shape == "circle" || shape == "dot"
            root.background = GradientDrawable().apply {
                this.shape = if (useOval) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
                if (!useOval) {
                    cornerRadius = BubbleChrome.cornerPx(shape, density, p.bubbleRoundPct)
                }
                setColor(fill)
                setStroke(stroke, on)
            }
            icon.visibility = View.VISIBLE
            val iconSz = ((if (shape == "pill") 22f else orbDp * 0.42f) * density).toInt()
            icon.layoutParams = LinearLayout.LayoutParams(iconSz, iconSz).apply {
                gravity = Gravity.CENTER
            }
        }
        applyBubbleIcon(icon, p.bubbleIconUri, on)
        applyOverlayWindowSize()
    }

    private fun applyBubbleIcon(icon: ImageView, uri: String, on: Int) {
        val local = BubbleIconPolicy.localFile(env.filesDir)
        val loadUri = when {
            local.isFile && local.length() > 0L -> android.net.Uri.fromFile(local)
            BubbleIconPolicy.validUri(uri) -> android.net.Uri.parse(uri)
            else -> null
        }
        if (loadUri == null) {
            icon.setImageResource(R.drawable.ic_mic)
            icon.setColorFilter(on)
            return
        }
        try {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            env.contentResolver.openInputStream(loadUri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, bounds)
            }
            val opts = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = BubbleIconPolicy.decodeSampleSize(bounds.outWidth, bounds.outHeight)
            }
            env.contentResolver.openInputStream(loadUri)?.use { stream ->
                val bmp = android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                if (bmp != null) {
                    icon.colorFilter = null
                    icon.setImageBitmap(bmp)
                    return
                }
            }
        } catch (_: Exception) {
        }
        icon.setImageResource(R.drawable.ic_mic)
        icon.setColorFilter(on)
    }

    /** Pin overlay window. WRAP_CONTENT measures against the screen. */
    private fun applyOverlayWindowSize() {
        val params = win.params ?: return
        val shape = FlowPrefs.normalizeBubbleShape(env.prefs?.bubbleShape.orEmpty())
        val wide = env.listening || env.postStopActive()
        val (w, h) = BubbleGeometry.overlaySizePx(
            env.listening,
            density,
            shape,
            chips = wide && !env.listening,
            cancel = env.prefs?.bubbleShowCancel != false,
            done = env.prefs?.bubbleShowDone != false,
        )
        params.width = if (w > 0) w else WindowManager.LayoutParams.WRAP_CONTENT
        params.height = if (h > 0) h else WindowManager.LayoutParams.WRAP_CONTENT
        val view = win.view ?: return
        try {
            win.updateLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun paintListenDisc(view: View?, fill: Int, on: Int, stroke: Int) {
        view ?: return
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            setStroke(stroke, on)
        }
    }

    private fun applyWaveFill(on: Int) {
        win.bubbleWaveBars.forEach { bar -> bar?.setBackgroundColor(on) }
    }

    fun applyWaveHeights(filled: Int) {
        win.bubbleWaveBars.forEachIndexed { i, bar ->
            val view = bar ?: return@forEachIndexed
            val hDp = if (i < filled) 8 + filled * 4 else 8
            val hPx = (hDp * density).toInt()
            val lp = view.layoutParams
            if (lp.height == hPx) return@forEachIndexed
            lp.height = hPx
            view.layoutParams = lp
        }
    }
}
