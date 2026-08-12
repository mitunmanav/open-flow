package app.openflow.bubble

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import app.openflow.stt.LanguagePolicy
import app.openflow.stt.SttEngine
import app.openflow.notify.DictationNotifier
import app.openflow.stt.SttTuning
import app.openflow.text.TextPostProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Wispr-style Flow Bubble + continuous on-device STT.
 *
 * Insert model (matches Wispr Android docs, local only):
 * - Listen accumulates speech on bubble only (no raw dump into keyboard).
 * - Stop / PTT release → course-correct + polish once → single SET_TEXT.
 * - Clipboard only if field insert fails (Wispr fallback).
 */
class FlowAccessibilityService : AccessibilityService(), SensorEventListener {

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
    /** Raw STT segments for this listen (not written to field until stop). */
    private var sessionBuffer = StringBuilder()
    /** Field text snapshot at listen start — session rewrite base. */
    private var fieldPrefix: String = ""
    private var lastPackage: String? = null
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val shakeDetector = ShakeDetector()
    private var shakeRegistered = false
    private var pulseUp = true

    private val pulseRunnable = object : Runnable {
        override fun run() {
            if (!listening) return
            val base = prefs?.bubbleOpacity ?: 0.8f
            bubbleView?.alpha = if (pulseUp) base else (base * 0.55f)
            pulseUp = !pulseUp
            mainHandler.postDelayed(this, 400L)
        }
    }

    private val app: OpenFlowApp get() = application as OpenFlowApp

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = FlowPrefs(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
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
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let { lastPackage = it }
            refreshBubbleVisibility()
        }
        if (prefs?.isSnoozed() == true) return
        if (PackagePolicy.shouldHideBubble(lastPackage)) return
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
                        root.packageName?.toString()?.let { lastPackage = it }
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
        hideBubble()
        stopPulse()
    }


    override fun onDestroy() {
        stopPulse()
        unregisterShake()
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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (prefs?.isSnoozed() != true) return
        val hit = shakeDetector.onAccel(
            event.values[0],
            event.values[1],
            event.values[2],
            System.currentTimeMillis()
        )
        if (hit) {
            prefs?.clearSnooze()
            refreshBubbleVisibility()
            Toast.makeText(this, R.string.flow_bubble_unsnooze_shake, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

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
        if (prefs == null) prefs = FlowPrefs(this)
        val p = prefs!!
        val scale = effectiveScale()
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
                    if (dragged && prefs?.bubbleEdgeSnap == true) {
                        snapBubbleToEdge(v, params)
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

    private fun snapBubbleToEdge(view: View, params: WindowManager.LayoutParams) {
        val dm = resources.displayMetrics
        val w = view.width.takeIf { it > 0 } ?: view.measuredWidth.takeIf { it > 0 } ?: 120
        params.x = BubbleGeometry.snapOffsetFromEnd(
            x = params.x,
            screenWidthPx = dm.widthPixels,
            bubbleWidthPx = w
        )
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
        if (prefs?.bubbleHaptics != false) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
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
        val bankHide = PackagePolicy.shouldHideBubble(lastPackage)
        val hide = snoozed || bankHide
        bubbleView?.visibility = if (hide) View.GONE else View.VISIBLE
        if (snoozed) registerShake() else unregisterShake()
        if (!hide && !listening) applyModeLabel()
    }

    private fun registerShake() {
        if (shakeRegistered) return
        val sm = sensorManager ?: return
        val accel = accelerometer ?: return
        sm.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        shakeRegistered = true
    }

    private fun unregisterShake() {
        if (!shakeRegistered) return
        sensorManager?.unregisterListener(this)
        shakeRegistered = false
    }

    private fun effectiveScale(): Float {
        val p = prefs ?: return 0.85f
        val modeMul = when (FlowPrefs.normalizeBubbleMode(p.bubbleMode)) {
            "compact" -> 0.65f
            "dot" -> 0.42f
            else -> 1f
        }
        return p.bubbleScale * modeMul
    }

    private fun applyModeLabel() {
        if (listening) return
        when (FlowPrefs.normalizeBubbleMode(prefs?.bubbleMode ?: "full")) {
            "dot" -> bubbleLabel?.text = "🎙"
            else -> renderIdle()
        }
    }

    private fun startPulse() {
        stopPulse()
        pulseUp = true
        mainHandler.post(pulseRunnable)
    }

    private fun stopPulse() {
        mainHandler.removeCallbacks(pulseRunnable)
    }

    private fun setBubbleEmphasis(hasField: Boolean) {
        if (listening) return // pulse owns alpha while listening
        bubbleView?.alpha = if (hasField) {
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
        fieldPrefix = captureFieldPrefix()
        listenStartedAt = SystemClock.elapsedRealtime()
        // Dot mode: grow slightly while active
        if (FlowPrefs.normalizeBubbleMode(prefs?.bubbleMode ?: "full") == "dot") {
            bubbleView?.scaleX = (prefs?.bubbleScale ?: 0.85f) * 0.75f
            bubbleView?.scaleY = (prefs?.bubbleScale ?: 0.85f) * 0.75f
        }
        if (prefs?.bubblePulse != false) startPulse()
        setListenChrome(0)
        setBubbleEmphasis(true)
        val lang = LanguagePolicy.LOCKED
        stt?.setListener(object : SttEngine.Listener {
            override fun onPartial(text: String) {
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                // Never dump partials into the field. Bubble caption only if pref on.
                if (prefs?.bubbleShowText == true) {
                    val preview = if (sessionBuffer.isEmpty()) text
                    else "${sessionBuffer} $text"
                    bubbleLabel?.text = BubbleLabelFormatter.partial(preview, elapsed)
                } else {
                    setListenChrome(elapsed)
                }
            }

            override fun onFinal(text: String) {
                if (text.isBlank()) return
                // Accumulate only. Field write happens once on stop (Wispr checkmark model).
                if (sessionBuffer.isNotEmpty()) sessionBuffer.append(' ')
                sessionBuffer.append(text.trim())
                if (prefs?.bubbleShowText == true) {
                    bubbleLabel?.text = BubbleLabelFormatter.finalChunk(sessionBuffer.toString())
                } else {
                    setListenChrome(
                        (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                    )
                }
            }

            override fun onError(message: String, fatal: Boolean) {
                if (fatal) {
                    bubbleLabel?.text = if (
                        message.contains("Microphone", true) ||
                        message.contains("Allow mic", true)
                    ) {
                        BubbleLabelFormatter.needMic()
                    } else message.take(40)
                    listening = false
                    stopPulse()
                    applyPrefsVisual()
                    setBubbleEmphasis(focusedEditable != null)
                }
            }

            override fun onNeedMicPermission() {
                bubbleLabel?.text = BubbleLabelFormatter.needMic()
                listening = false
            }

            override fun onReady() {
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                if (prefs?.bubbleShowText == true) {
                    val current = bubbleLabel?.text?.toString().orEmpty()
                    if (current.isBlank() ||
                        current.startsWith("Listening") ||
                        current == getString(R.string.flow_bubble_listening)
                    ) {
                        bubbleLabel?.text = BubbleLabelFormatter.listening(elapsed)
                    }
                } else {
                    setListenChrome(elapsed)
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (!listening) return
                val base = effectiveScale()
                val pulse = BubbleGeometry.rmsScaleY(rmsdB)
                bubbleView?.scaleX = base * pulse
                bubbleView?.scaleY = base * pulse
            }

            override fun onListeningChanged(isOn: Boolean) {
                // Continuous restarts do not emit false; false = user/engine full stop.
                if (!isOn) {
                    listening = false
                    stopPulse()
                    applyPrefsVisual()
                    renderIdle()
                }
            }
        })
        stt?.startContinuous(lang)
    }

    private fun stopListening(save: Boolean) {
        listening = false
        pushToTalk = false
        stopPulse()
        stt?.stop()
        val raw = sessionBuffer.toString().trim()
        if (save && raw.isNotBlank()) {
            val dur = SystemClock.elapsedRealtime() - listenStartedAt
            val lang = LanguagePolicy.LOCKED
            // One polish + one field write (no raw chunk dump)
            polishSession(raw) { finalText ->
                if (finalText.isNotBlank()) {
                    commitSessionToField(finalText)
                    val wordCount = finalText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            app.dictations.saveDictation(finalText, dur, lang)
                        }
                    }
                    DictationNotifier.notifyIfPermitted(this@FlowAccessibilityService, wordCount)
                }
            }
        }
        sessionBuffer = StringBuilder()
        fieldPrefix = ""
        applyModeLabel()
        setBubbleEmphasis(focusedEditable != null)
    }

    private fun captureFieldPrefix(): String {
        val root = rootInActiveWindow
        val node = resolveEditable(root, focusedEditable)
        try {
            root?.let {
                @Suppress("DEPRECATION")
                it.recycle()
            }
        } catch (_: Exception) {
        }
        if (node == null) return ""
        return try {
            node.text?.toString().orEmpty()
        } finally {
            @Suppress("DEPRECATION")
            node.recycle()
        }
    }

    private fun polishSession(text: String, onDone: (String) -> Unit) {
        scope.launch {
            val dict = app.dictations.dictionaryMap()
            val snip = app.dictations.snippetMap()
            var t = TextPostProcessor.expandSnippets(text, snip)
            // Snippet full-replace skips further polish
            if (t == text || snip.values.none { it == t }) {
                val level = FlowPrefs.normalizeCleanupLevel(prefs?.cleanupLevel ?: "medium")
                when (level) {
                    "none" -> { /* raw STT only */ }
                    "light" -> {
                        t = TextPostProcessor.polishSession(
                            t,
                            prefs?.style() ?: TextPostProcessor.Style.CASUAL,
                            courseCorrect = false
                        )
                    }
                    else -> {
                        // medium + high: full local polish (high uses same local rules for now)
                        t = TextPostProcessor.polishSession(
                            t,
                            prefs?.style() ?: TextPostProcessor.Style.CASUAL,
                            courseCorrect = true
                        )
                    }
                }
                t = TextPostProcessor.applyDictionary(t, dict)
            }
            mainHandler.post { onDone(t) }
        }
    }

    /** Single SET_TEXT of prefix + polished session. Clipboard only on insert fail. */
    private fun commitSessionToField(finalText: String) {
        if (finalText.isBlank()) return
        val root = rootInActiveWindow
        val node = resolveEditable(root, focusedEditable)
        try {
            root?.let {
                @Suppress("DEPRECATION")
                it.recycle()
            }
        } catch (_: Exception) {
        }
        if (node == null) {
            copyToClipboard(finalText)
            Toast.makeText(this, R.string.flow_bubble_copied_clipboard, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if (!isUsableEditable(node)) {
                copyToClipboard(finalText)
                return
            }
            val merged = FieldPolicy.mergeSession(fieldPrefix, finalText)
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    merged
                )
            }
            var ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (!ok) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
            if (!ok) {
                copyToClipboard(finalText)
                Toast.makeText(this, R.string.flow_bubble_copied_clipboard, Toast.LENGTH_SHORT).show()
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

    /** Prefer live FOCUS_INPUT; fall back to cached node. */
    private fun resolveEditable(
        root: AccessibilityNodeInfo?,
        cached: AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {
        if (root != null) {
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null) {
                if (isUsableEditable(focused)) return focused
                val nested = findEditableInSubtree(focused)
                @Suppress("DEPRECATION")
                focused.recycle()
                if (nested != null) return nested
            }
        }
        if (cached != null && isUsableEditable(cached)) {
            @Suppress("DEPRECATION")
            return AccessibilityNodeInfo.obtain(cached)
        }
        return null
    }

    private fun copyToClipboard(text: String) {
        try {
            val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("open-flow", text))
        } catch (_: Exception) {
        }
    }

    private fun renderIdle() {
        when (FlowPrefs.normalizeBubbleMode(prefs?.bubbleMode ?: "full")) {
            "dot" -> bubbleLabel?.text = "🎙"
            else -> bubbleLabel?.text = when (prefs?.bubbleShape) {
                "pill" -> "🎙"
                "square" -> "■"
                "dot" -> "●"
                else -> "🎙"
            }.let { icon ->
                if (prefs?.bubbleShowText == true) BubbleLabelFormatter.idle() else icon
            }
        }
    }

    /** Icon / wave status — no speech transcript when showText is off. */
    private fun setListenChrome(elapsedSec: Long) {
        bubbleLabel?.text = when {
            prefs?.bubbleShowText == true -> BubbleLabelFormatter.listening(elapsedSec)
            elapsedSec > 0 -> "● $elapsedSec"
            else -> "●"
        }
    }

    fun applyPrefsVisual() {
        val p = prefs ?: return
        val s = effectiveScale()
        bubbleView?.scaleX = s
        bubbleView?.scaleY = s
        bubbleParams?.alpha = p.bubbleOpacity
        bubbleView?.let { v ->
            bubbleParams?.let { params ->
                try {
                    windowManager?.updateViewLayout(v, params)
                } catch (_: Exception) {
                }
            }
        }
        if (!listening) applyModeLabel()
        refreshBubbleVisibility()
    }

    companion object {
        @Volatile
        var instance: FlowAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
