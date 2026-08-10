package app.openflow.bubble

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.SystemClock
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
 * Wispr Flow Android-style: floating bubble + continuous STT insert.
 * Not an IME. Speak as long as you want while listening is ON.
 */
class FlowAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var bubbleLabel: TextView? = null
    private var stt: SttEngine? = null
    private var listening = false
    private var focusedEditable: AccessibilityNodeInfo? = null
    private var listenStartedAt = 0L
    private var sessionIndex = 0

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
            notificationTimeout = 80
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
                    @Suppress("DEPRECATION")
                    source.recycle()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                rootInActiveWindow?.let { root ->
                    try {
                        findFocusedEditable(root)?.let { node ->
                            updateFocusFrom(node)
                            @Suppress("DEPRECATION")
                            node.recycle()
                        }
                    } finally {
                        @Suppress("DEPRECATION")
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
        focusedEditable?.let {
            @Suppress("DEPRECATION")
            it.recycle()
        }
        focusedEditable = null
        instance = null
        super.onDestroy()
    }

    private fun updateFocusFrom(node: AccessibilityNodeInfo) {
        val target = when {
            isUsableEditable(node) -> {
                @Suppress("DEPRECATION")
                AccessibilityNodeInfo.obtain(node)
            }
            else -> findEditableInSubtree(node)
        }
        focusedEditable?.let {
            @Suppress("DEPRECATION")
            it.recycle()
        }
        focusedEditable = target
        setBubbleEmphasis(target != null)
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        if (isUsableEditable(focused)) return focused
        val nested = findEditableInSubtree(focused)
        @Suppress("DEPRECATION")
        focused.recycle()
        return nested
    }

    private fun findEditableInSubtree(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (isUsableEditable(node)) {
            @Suppress("DEPRECATION")
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableInSubtree(child)
            @Suppress("DEPRECATION")
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
            renderIdle()
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
        bubbleView?.alpha = if (hasField || listening) 1f else 0.7f
    }

    private fun onBubbleTap() {
        if (listening) stopListening() else startListening()
    }

    private fun startListening() {
        val micOk = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!micOk) {
            bubbleLabel?.text = getString(R.string.flow_bubble_need_mic)
            listening = false
            return
        }
        listening = true
        listenStartedAt = SystemClock.elapsedRealtime()
        sessionIndex = 0
        bubbleLabel?.text = getString(R.string.flow_bubble_listening)
        setBubbleEmphasis(true)
        stt?.setListener(object : SttEngine.Listener {
            override fun onPartial(text: String) {
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                val preview = text.take(22)
                bubbleLabel?.text = if (preview.isBlank()) {
                    "Listening ${elapsed}s"
                } else {
                    "● $preview"
                }
            }

            override fun onFinal(text: String) {
                insertText(text)
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                bubbleLabel?.text = "Listening ${elapsed}s · tap stop"
            }

            override fun onError(message: String, fatal: Boolean) {
                if (fatal) {
                    bubbleLabel?.text = if (message.contains("Microphone", ignoreCase = true) ||
                        message.contains("Allow mic", ignoreCase = true)
                    ) {
                        getString(R.string.flow_bubble_need_mic)
                    } else {
                        message.take(28)
                    }
                    listening = false
                    setBubbleEmphasis(focusedEditable != null)
                }
                // non-fatal: engine auto-restarts
            }

            override fun onNeedMicPermission() {
                bubbleLabel?.text = getString(R.string.flow_bubble_need_mic)
                listening = false
            }

            override fun onReady() {
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                bubbleLabel?.text = "Listening ${elapsed}s"
            }

            override fun onListeningChanged(isOn: Boolean) {
                listening = isOn
                if (!isOn) renderIdle()
            }

            override fun onSessionTick(sessionIndex: Int) {
                this@FlowAccessibilityService.sessionIndex = sessionIndex
            }
        })
        stt?.startContinuous()
    }

    private fun stopListening() {
        listening = false
        stt?.stop()
        renderIdle()
        setBubbleEmphasis(focusedEditable != null)
    }

    private fun renderIdle() {
        bubbleLabel?.text = getString(R.string.flow_bubble_idle)
    }

    private fun insertText(spoken: String) {
        val root = rootInActiveWindow
        val node = FocusResolver.resolveEditable(
            root = root,
            cached = focusedEditable,
            isUsable = { isUsableEditable(it) },
            findInSubtree = { findEditableInSubtree(it) }
        )
        try {
            root?.let {
                @Suppress("DEPRECATION")
                it.recycle()
            }
        } catch (_: Exception) {
        }
        if (node == null) return
        try {
            if (!isUsableEditable(node)) return
            val merged = FieldPolicy.mergeInsert(node.text, spoken)
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    merged
                )
            }
            val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (!ok) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
            // refresh cache
            focusedEditable?.let {
                @Suppress("DEPRECATION")
                it.recycle()
            }
            @Suppress("DEPRECATION")
            focusedEditable = AccessibilityNodeInfo.obtain(node)
        } finally {
            @Suppress("DEPRECATION")
            node.recycle()
        }
    }

    companion object {
        @Volatile
        var instance: FlowAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
