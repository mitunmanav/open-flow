package app.openflow.bubble

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import app.openflow.OpenFlowApp
import app.openflow.R
import app.openflow.prefs.FlowPrefs
import app.openflow.stt.SttEngine
import app.openflow.text.TextPostProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Wispr-style Flow Bubble + continuous on-device STT insert.
 * Features: long-press PTT, drag, snooze, post-process, dictionary/snippets, history.
 */
class FlowAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var bubbleLabel: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var stt: SttEngine? = null
    private var listening = false
    private var pushToTalk = false
    private var focusedEditable: AccessibilityNodeInfo? = null
    private var listenStartedAt = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var prefs: FlowPrefs? = null
    private var sessionBuffer = StringBuilder()

    private val app: OpenFlowApp get() = application as OpenFlowApp

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = FlowPrefs(this)
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
        refreshBubbleVisibility()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (prefs?.isSnoozed() == true) return
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
        stopListening(save = false)
    }

    override fun onDestroy() {
        stopListening(save = false)
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
        val p = prefs ?: FlowPrefs(this)
        val scale = p.bubbleScale
        val opacity = p.bubbleOpacity
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
            alpha = opacity
        }
        bubbleParams = params
        view.scaleX = scale
        view.scaleY = scale
        setupTouch(view, params)
        try {
            windowManager?.addView(view, params)
            bubbleView = view
            renderIdle()
            setBubbleEmphasis(false)
        } catch (_: Exception) {
            bubbleView = null
        }
    }

    private fun setupTouch(view: View, params: WindowManager.LayoutParams) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        var longPressFired = false
        val longPress = Runnable {
            longPressFired = true
            pushToTalk = true
            if (!listening) startListening()
        }
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    longPressFired = false
                    mainHandler.postDelayed(longPress, 450)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) + abs(dy) > 16) {
                        dragged = true
                        mainHandler.removeCallbacks(longPress)
                    }
                    params.x = (startX - dx).coerceAtLeast(0)
                    params.y = (startY - dy).coerceAtLeast(0)
                    // snooze zone: drag near bottom
                    if (params.y < 40 && dragged) {
                        bubbleLabel?.text = getString(R.string.flow_bubble_snooze_hint)
                    }
                    try {
                        windowManager?.updateViewLayout(v, params)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mainHandler.removeCallbacks(longPress)
                    if (dragged && params.y < 40) {
                        prefs?.snoozeMinutes(10)
                        Toast.makeText(this, R.string.flow_bubble_snoozed, Toast.LENGTH_SHORT).show()
                        refreshBubbleVisibility()
                        return@setOnTouchListener true
                    }
                    if (longPressFired || pushToTalk) {
                        // release ends PTT
                        if (listening) stopListening(save = true)
                        pushToTalk = false
                    } else if (!dragged) {
                        if (listening) stopListening(save = true) else startListening()
                    }
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
        bubbleParams = null
    }

    private fun refreshBubbleVisibility() {
        val snoozed = prefs?.isSnoozed() == true
        bubbleView?.visibility = if (snoozed) View.GONE else View.VISIBLE
    }

    private fun setBubbleEmphasis(hasField: Boolean) {
        bubbleView?.alpha = if (hasField || listening) {
            (prefs?.bubbleOpacity ?: 0.9f)
        } else {
            (prefs?.bubbleOpacity ?: 0.8f) * 0.75f
        }
    }

    private fun startListening() {
        if (prefs?.isSnoozed() == true) {
            prefs?.clearSnooze()
            refreshBubbleVisibility()
        }
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
        sessionBuffer = StringBuilder()
        listenStartedAt = SystemClock.elapsedRealtime()
        bubbleLabel?.text = getString(R.string.flow_bubble_listening)
        setBubbleEmphasis(true)
        val lang = prefs?.languageTag ?: java.util.Locale.getDefault().toLanguageTag()
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
                val cleaned = polish(text)
                if (cleaned.isNotBlank()) {
                    insertText(cleaned)
                    if (sessionBuffer.isNotEmpty()) sessionBuffer.append(' ')
                    sessionBuffer.append(cleaned)
                    copyToClipboard(cleaned)
                }
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                bubbleLabel?.text = "Listening ${elapsed}s · stop"
            }

            override fun onError(message: String, fatal: Boolean) {
                if (fatal) {
                    bubbleLabel?.text = if (
                        message.contains("Microphone", true) ||
                        message.contains("Allow mic", true)
                    ) {
                        getString(R.string.flow_bubble_need_mic)
                    } else message.take(28)
                    listening = false
                    setBubbleEmphasis(focusedEditable != null)
                }
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

            override fun onSessionTick(sessionIndex: Int) {}
        })
        stt?.startContinuous(lang)
    }

    private fun stopListening(save: Boolean) {
        listening = false
        pushToTalk = false
        stt?.stop()
        if (save && sessionBuffer.isNotBlank()) {
            val text = sessionBuffer.toString()
            val dur = SystemClock.elapsedRealtime() - listenStartedAt
            val lang = prefs?.languageTag ?: "en"
            scope.launch(Dispatchers.IO) {
                runCatching {
                    app.dictations.saveDictation(text, dur, lang)
                }
            }
        }
        sessionBuffer = StringBuilder()
        renderIdle()
        setBubbleEmphasis(focusedEditable != null)
    }

    private fun polish(raw: String): String {
        var t = raw
        // snippets + dictionary applied async-safe: use last cached maps if needed
        // For simplicity, blocking-ish on Main is bad — use empty if not loaded
        // Apply pure post-process always
        t = TextPostProcessor.process(t, prefs?.style() ?: TextPostProcessor.Style.CASUAL)
        return t
    }

    private fun applyDictAndSnippets(text: String, onDone: (String) -> Unit) {
        scope.launch {
            val dict = app.dictations.dictionaryMap()
            val snip = app.dictations.snippetMap()
            var t = TextPostProcessor.expandSnippets(text, snip)
            t = TextPostProcessor.applyDictionary(t, dict)
            t = TextPostProcessor.process(t, prefs?.style() ?: TextPostProcessor.Style.CASUAL)
            mainHandler.post { onDone(t) }
        }
    }

    private fun insertText(spoken: String) {
        applyDictAndSnippets(spoken) { finalText ->
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
            if (node == null) return@applyDictAndSnippets
            try {
                if (!isUsableEditable(node)) return@applyDictAndSnippets
                val merged = FieldPolicy.mergeInsert(node.text, finalText)
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
    }

    private fun copyToClipboard(text: String) {
        try {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("open-flow", text))
        } catch (_: Exception) {
        }
    }

    private fun renderIdle() {
        bubbleLabel?.text = getString(R.string.flow_bubble_idle)
    }

    fun applyPrefsVisual() {
        val p = prefs ?: return
        bubbleView?.scaleX = p.bubbleScale
        bubbleView?.scaleY = p.bubbleScale
        bubbleParams?.alpha = p.bubbleOpacity
        bubbleView?.let { v ->
            bubbleParams?.let { params ->
                try {
                    windowManager?.updateViewLayout(v, params)
                } catch (_: Exception) {
                }
            }
        }
        refreshBubbleVisibility()
    }

    companion object {
        @Volatile
        var instance: FlowAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
