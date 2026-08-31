package app.openflow.bubble

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import android.view.accessibility.AccessibilityWindowInfo
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.openflow.R
import app.openflow.prefs.FlowPrefs
import kotlin.math.abs

/**
 * Owns the overlay window: views, layout params, edge-snap animator, IME probes.
 * No dictation logic — service wires behavior through callbacks.
 */
class BubbleWindowController(
    private val context: Context,
    private val windowsProvider: () -> List<AccessibilityWindowInfo>?,
    private val hapticTap: () -> Unit,
) {
    var prefs: FlowPrefs? = null

    private var windowManager: WindowManager? = null
    var view: View? = null
    var bubbleRoot: LinearLayout? = null
    var bubbleIcon: ImageView? = null
    var bubbleLabel: TextView? = null
    var bubblePulseRing: View? = null
    var bubbleCancel: ImageView? = null
    var bubbleDone: ImageView? = null
    var bubbleWave: View? = null
    var bubbleWaveBars: Array<View?> = arrayOfNulls(4)
    var bubbleChipCopy: TextView? = null
    var bubbleChipUndo: TextView? = null
    var bubbleChipPaste: TextView? = null
    var bubbleChipLang: TextView? = null
    var params: WindowManager.LayoutParams? = null
    var snapAnimator: ValueAnimator? = null

    companion object {
        private const val ACTION_CANCEL = 0x1001
        private const val ACTION_DONE = 0x1002
        private const val ACTION_COPY = 0x1003
        private const val ACTION_UNDO = 0x1004
        private const val ACTION_PASTE = 0x1005
        private const val ACTION_SNOOZE = 0x1006
        private const val ACTION_LANG = 0x1007
    }

    /** Soft keyboard present (Wispr: bubble lives with field + keyboard). */
    var imeVisible: Boolean = false
        private set

    /** TYPE_INPUT_METHOD height in px. 0 = IME down / unknown. Not written to prefs. */
    var imeHeightPx: Int = 0
        private set

    /**
     * Inflate, wire cancel/done, add to window manager. Returns false if add failed.
     * [onAdded] runs only after a successful attach.
     */
    fun show(
        scale: Float,
        opacity: Float,
        savedX: Int,
        savedY: Int,
        onCancel: () -> Unit,
        onDone: () -> Unit,
        setupTouch: (View, WindowManager.LayoutParams) -> Unit,
        onAdded: () -> Unit,
    ): Boolean {
        if (view != null) return true
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val v = LayoutInflater.from(context).inflate(R.layout.flow_bubble, null)
        bubbleRoot = v.findViewById(R.id.bubble_root)
        bubbleIcon = v.findViewById(R.id.bubble_icon)
        bubbleLabel = v.findViewById(R.id.bubble_label)
        bubblePulseRing = v.findViewById(R.id.bubble_pulse_ring)
        bubbleCancel = v.findViewById(R.id.bubble_cancel)
        bubbleDone = v.findViewById(R.id.bubble_done)
        bubbleWave = v.findViewById(R.id.bubble_wave)
        bubbleWaveBars[0] = v.findViewById(R.id.bubble_wave_0)
        bubbleWaveBars[1] = v.findViewById(R.id.bubble_wave_1)
        bubbleWaveBars[2] = v.findViewById(R.id.bubble_wave_2)
        bubbleWaveBars[3] = v.findViewById(R.id.bubble_wave_3)
        bubbleChipCopy = v.findViewById(R.id.bubble_chip_copy)
        bubbleChipUndo = v.findViewById(R.id.bubble_chip_undo)
        bubbleChipPaste = v.findViewById(R.id.bubble_chip_paste)
        bubbleChipLang = v.findViewById(R.id.bubble_chip_lang)

        // Wispr: Cancel discards; Done inserts; tap bubble also inserts.
        bubbleCancel?.setOnClickListener { onCancel() }
        bubbleDone?.setOnClickListener { onDone() }

        refreshImeHeight()
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = savedX
            y = parkedY(savedY)
            alpha = opacity
        }
        params = lp
        v.scaleX = scale
        v.scaleY = scale
        setupTouch(v, lp)
        return try {
            windowManager?.addView(v, lp)
            view = v
            onAdded()
            true
        } catch (_: Exception) {
            view = null
            false
        }
    }

    fun hide() {
        snapAnimator?.cancel()
        snapAnimator = null
        view?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {
            }
        }
        view = null
        bubbleRoot = null
        bubbleIcon = null
        bubbleLabel = null
        bubblePulseRing = null
        bubbleCancel = null
        bubbleDone = null
        bubbleWave = null
        bubbleWaveBars = arrayOfNulls(4)
        bubbleChipCopy = null
        bubbleChipUndo = null
        bubbleChipPaste = null
        bubbleChipLang = null
        params = null
    }

    fun updateLayout(v: View, lp: WindowManager.LayoutParams) {
        try {
            windowManager?.updateViewLayout(v, lp)
        } catch (_: Exception) {
        }
    }

    fun hitVisible(target: View?, rawX: Float, rawY: Float): Boolean {
        if (target == null || target.visibility != View.VISIBLE) return false
        val loc = IntArray(2)
        target.getLocationOnScreen(loc)
        val l = loc[0]
        val t = loc[1]
        return rawX >= l && rawX < l + target.width && rawY >= t && rawY < t + target.height
    }

    fun animatePress(v: View, pressed: Boolean, baseScale: Float) {
        val target = if (pressed) baseScale * 0.92f else baseScale
        v.animate()
            .scaleX(target)
            .scaleY(target)
            .setDuration(110L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun snapToEdge(
        v: View,
        lp: WindowManager.LayoutParams,
        vxPxPerSec: Float = 0f,
    ) {
        val dm = context.resources.displayMetrics
        val w = v.width.takeIf { it > 0 } ?: v.measuredWidth.takeIf { it > 0 } ?: 120
        val targetX = BubbleMotion.snapX(
            x = lp.x,
            vxPxPerSec = vxPxPerSec,
            screenWidthPx = dm.widthPixels,
            bubbleWidthPx = w,
        )
        snapAnimator?.cancel()
        val startX = lp.x
        prefs?.bubbleX = targetX
        if (startX == targetX) {
            hapticTap()
            updateLayout(v, lp)
            return
        }
        snapAnimator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = BubbleMotion.snapDurationMs(abs(targetX - startX), vxPxPerSec)
            interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
            addUpdateListener { anim ->
                lp.x = anim.animatedValue as Int
                updateLayout(v, lp)
            }
            start()
        }
        hapticTap()
    }

    fun refreshImeHeight() {
        imeHeightPx = detectImeHeight()
        imeVisible = imeHeightPx > 0 || detectImeVisible()
    }

    /** Display y from saved y. Parked value is never written back to prefs. */
    fun parkedY(savedY: Int): Int {
        val dm = context.resources.displayMetrics
        val h = view?.let { v ->
            v.height.takeIf { it > 0 } ?: v.measuredHeight.takeIf { it > 0 }
        } ?: 120
        val clamped = BubbleGeometry.clampVerticalOffset(
            y = savedY,
            screenHeightPx = dm.heightPixels,
            bubbleHeightPx = h
        )
        return BubbleGeometry.parkYAboveIme(clamped, imeHeightPx)
    }

    fun applyParkedOverlayY() {
        val lp = params ?: return
        val v = view ?: return
        lp.y = parkedY(prefs?.bubbleY ?: lp.y)
        updateLayout(v, lp)
    }

    fun updateA11yActions(
        listening: Boolean,
        stopInProgress: Boolean,
        chipState: PostStopChips.State? = null,
        hasField: Boolean = false,
        onCancel: () -> Unit = {},
        onDone: () -> Unit = {},
        onCopy: () -> Unit = {},
        onUndo: () -> Unit = {},
        onPaste: () -> Unit = {},
        onSnooze: () -> Unit = {},
        onLang: () -> Unit = {},
    ) {
        val root = bubbleRoot ?: view ?: return
        root.isFocusable = true
        root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        val desc = when {
            listening -> "Listening, double tap Done, swipe for actions"
            chipState?.any == true -> "Bubble idle, actions available"
            hasField -> "Tap to talk"
            else -> "Flow bubble, tap to talk"
        }
        root.contentDescription = desc
        // Also keep icon contentDescription in sync for screen readers that focus the icon directly.
        bubbleIcon?.contentDescription = if (listening) "Listening, tap to finish" else "Tap to talk"
        bubbleWave?.contentDescription = if (listening) "Listening" else null
        ViewCompat.setAccessibilityDelegate(root, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = "android.widget.Button"
                info.isClickable = true
                info.isFocusable = true
                if (listening && !stopInProgress) {
                    info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(ACTION_CANCEL, "Cancel"))
                    info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(ACTION_DONE, "Done"))
                } else {
                    if (chipState?.copy == true) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(ACTION_COPY, "Copy"))
                    }
                    if (chipState?.undo == true) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(ACTION_UNDO, "Undo"))
                    }
                    if (chipState?.paste == true) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(ACTION_PASTE, "Paste"))
                    }
                }
                if (!listening) {
                    info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(ACTION_SNOOZE, "Snooze 10 minutes"))
                }
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(ACTION_LANG, "Change language"))
            }

            override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                return when (action) {
                    ACTION_CANCEL -> { onCancel(); true }
                    ACTION_DONE -> { onDone(); true }
                    ACTION_COPY -> { onCopy(); true }
                    ACTION_UNDO -> { onUndo(); true }
                    ACTION_PASTE -> { onPaste(); true }
                    ACTION_SNOOZE -> { onSnooze(); true }
                    ACTION_LANG -> { onLang(); true }
                    else -> super.performAccessibilityAction(host, action, args)
                }
            }
        })
    }

    private fun detectImeVisible(): Boolean {
        return try {
            windowsProvider()?.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } == true
        } catch (_: Exception) {
            false
        }
    }

    private fun detectImeHeight(): Int {
        return try {
            val screenH = context.resources.displayMetrics.heightPixels
            val rect = Rect()
            var h = 0
            windowsProvider()?.forEach { w ->
                if (w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    w.getBoundsInScreen(rect)
                    h = maxOf(h, BubbleGeometry.imeHeightFromBounds(rect.top, rect.bottom, screenH))
                }
            }
            h
        } catch (_: Exception) {
            0
        }
    }
}
