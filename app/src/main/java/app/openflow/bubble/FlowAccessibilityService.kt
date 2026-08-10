package app.openflow.bubble

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import app.openflow.R
import app.openflow.stt.SttEngine
import kotlin.math.abs

/**
 * Wispr Flow Android-style: floating bubble + insert text into focused field.
 * Not an IME / keyboard.
 */
class FlowAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var bubbleLabel: TextView? = null
    private var stt: SttEngine? = null
    private var listening = false
    private var focusedEditable: AccessibilityNodeInfo? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        stt = SttEngine(applicationContext, preferOnDevice = true)
        showBubble()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source ?: return
                try {
                    updateFocusFrom(source)
                } finally {
                    source.recycle()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                rootInActiveWindow?.let { root ->
                    try {
                        findFocusedEditable(root)?.let { node ->
                            updateFocusFrom(node)
                            node.recycle()
                        }
                    } finally {
                        root.recycle()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        stopListening()
    }

    override fun onDestroy() {
        stopListening()
        hideBubble()
        stt?.destroy()
        stt = null
        focusedEditable?.recycle()
        focusedEditable = null
        instance = null
        super.onDestroy()
    }

    private fun updateFocusFrom(node: AccessibilityNodeInfo) {
        val target = when {
            isUsableEditable(node) -> AccessibilityNodeInfo.obtain(node)
            else -> findEditableInSubtree(node)
        }
        focusedEditable?.recycle()
        focusedEditable = target
        setBubbleEmphasis(target != null)
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null) {
            if (isUsableEditable(focused)) return focused
            val nested = findEditableInSubtree(focused)
            focused.recycle()
            return nested
        }
        return null
    }

    private fun findEditableInSubtree(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (isUsableEditable(node)) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableInSubtree(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }

    private fun isUsableEditable(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEnabled) return false
        val looksEdit = node.isEditable ||
            FieldPolicy.isEditableClass(node.className?.toString())
        if (!looksEdit) return false
        if (FieldPolicy.isSensitive(
                isPassword = node.isPassword,
                inputType = 0,
                className = node.className?.toString(),
                hintOrDesc = listOfNotNull(
                    node.text?.toString(),
                    node.contentDescription?.toString()
                ).joinToString(" ")
            )
        ) {
            return false
        }
        return true
    }

    private fun showBubble() {
        if (bubbleView != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.flow_bubble, null)
        bubbleLabel = view.findViewById(R.id.bubble_label)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 32
            y = 220
        }
        setupDragAndClick(view, params)
        try {
            windowManager?.addView(view, params)
            bubbleView = view
            setBubbleEmphasis(false)
        } catch (_: Exception) {
            bubbleView = null
        }
    }

    private fun setupDragAndClick(view: View, params: WindowManager.LayoutParams) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) + abs(dy) > 16) dragged = true
                    // gravity BOTTOM|END: x increases toward left, y toward top
                    params.x = (startX - dx).coerceAtLeast(0)
                    params.y = (startY - dy).coerceAtLeast(0)
                    try {
                        windowManager?.updateViewLayout(v, params)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) onBubbleTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun hideBubble() {
        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {
            }
        }
        bubbleView = null
        bubbleLabel = null
    }

    private fun setBubbleEmphasis(hasField: Boolean) {
        bubbleView?.alpha = if (hasField || listening) 1f else 0.65f
    }

    private fun onBubbleTap() {
        if (listening) stopListening() else startListening()
    }

    private fun startListening() {
        listening = true
        bubbleLabel?.text = getString(R.string.flow_bubble_listening)
        setBubbleEmphasis(true)
        stt?.setListener(object : SttEngine.Listener {
            override fun onPartial(text: String) {
                bubbleLabel?.text = text.take(28).ifBlank {
                    getString(R.string.flow_bubble_listening)
                }
            }

            override fun onFinal(text: String) {
                insertText(text)
                bubbleLabel?.text = getString(R.string.flow_bubble_idle)
                if (listening) stt?.start()
            }

            override fun onError(message: String) {
                bubbleLabel?.text = getString(R.string.flow_bubble_idle)
                listening = false
            }

            override fun onReady() {
                bubbleLabel?.text = getString(R.string.flow_bubble_listening)
            }

            override fun onEnd() {}
        })
        stt?.start()
    }

    private fun stopListening() {
        listening = false
        stt?.stop()
        bubbleLabel?.text = getString(R.string.flow_bubble_idle)
        setBubbleEmphasis(focusedEditable != null)
    }

    private fun insertText(spoken: String) {
        val node = focusedEditable ?: return
        if (!isUsableEditable(node)) return
        val merged = FieldPolicy.mergeInsert(node.text, spoken)
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                merged
            )
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    companion object {
        @Volatile
        var instance: FlowAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
