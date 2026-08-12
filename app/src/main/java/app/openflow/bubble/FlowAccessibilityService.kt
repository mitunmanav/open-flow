package app.openflow.bubble

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import app.openflow.BuildConfig
import app.openflow.OpenFlowApp
import app.openflow.R
import app.openflow.notify.DictationNotifier
import app.openflow.prefs.FlowPrefs
import app.openflow.stt.LanguagePolicy
import app.openflow.stt.SttEngine
import app.openflow.text.CleanupLevel
import app.openflow.text.CleanupResult
import app.openflow.text.CustomStyleConfig
import app.openflow.text.TextPostProcessor
import app.openflow.text.WritingStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Wispr-style Flow Bubble + continuous on-device STT.
 *
 * Floating overlay features:
 * - Dynamic shapes: Pill, Circle/Orb, Squircle, Dot.
 * - Glassmorphism surface with electric indigo / emerald recording glow.
 * - Live volume pulsing (RMS) and magnetic edge-snapping.
 * - Single-commit text insertion into focused accessible field on stop.
 */
class FlowAccessibilityService : AccessibilityService(), SensorEventListener {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var bubbleRoot: LinearLayout? = null
    private var bubbleIcon: ImageView? = null
    private var bubbleLabel: TextView? = null
    private var bubblePulseRing: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var stt: SttEngine? = null
    private var listening = false
    private var pushToTalk = false
    private var focusedEditable: AccessibilityNodeInfo? = null
    private var listenStartedAt = 0L
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var prefs: FlowPrefs? = null
    /** Bumps on each start/stop so late STT events are ignored. */
    private var listenGeneration = 0

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
    private var injectReceiverRegistered = false

    private val pulseRunnable = object : Runnable {
        override fun run() {
            if (!listening) return
            val base = prefs?.bubbleOpacity ?: 0.85f
            bubbleView?.alpha = if (pulseUp) base else (base * 0.65f)
            pulseUp = !pulseUp
            mainHandler.postDelayed(this, 350L)
        }
    }

    /** Debug-only: adb inject without mic (see companion ACTION_INJECT). */
    private val injectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!BuildConfig.DEBUG) return
            if (intent?.action != ACTION_INJECT) return
            val raw = intent.getStringExtra(EXTRA_TEXT).orEmpty()
            android.util.Log.i("OpenFlow.Inject", "recv rawLen=${raw.length}")
            injectDictation(raw)
        }
    }

    private val app: OpenFlowApp get() = application as OpenFlowApp

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = FlowPrefs(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
            // Skip TYPE_WINDOW_CONTENT_CHANGED — spam while typing; focus + window enough.
            eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        stt = SttEngine(
            applicationContext,
            preferOnDevice = true,
            softMuteBeeps = false
        )
        showBubble()
        instance = this
        registerInjectReceiver()
        android.util.Log.i("OpenFlow.Bubble", "onServiceConnected overlay=ready debugInject=${BuildConfig.DEBUG}")
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
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
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
        unregisterInjectReceiver()
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
        serviceJob.cancel()
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
        bubbleRoot = view.findViewById(R.id.bubble_root)
        bubbleIcon = view.findViewById(R.id.bubble_icon)
        bubbleLabel = view.findViewById(R.id.bubble_label)
        bubblePulseRing = view.findViewById(R.id.bubble_pulse_ring)

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
            x = p.bubbleX
            y = p.bubbleY
            alpha = opacity
        }
        bubbleParams = params
        view.scaleX = scale
        view.scaleY = scale
        setupTouch(view, params)
        try {
            windowManager?.addView(view, params)
            bubbleView = view
            updateBubbleVisuals()
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
                    prefs?.let {
                        it.bubbleX = params.x
                        it.bubbleY = params.y
                    }
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
        bubbleRoot = null
        bubbleIcon = null
        bubbleLabel = null
        bubblePulseRing = null
        bubbleParams = null
    }

    private fun refreshBubbleVisibility() {
        val snoozed = prefs?.isSnoozed() == true
        val bankHide = PackagePolicy.shouldHideBubble(lastPackage)
        val hide = snoozed || bankHide
        bubbleView?.visibility = if (hide) View.GONE else View.VISIBLE
        if (snoozed) registerShake() else unregisterShake()
        if (!hide) updateBubbleVisuals()
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
        val p = prefs ?: return 0.9f
        val modeMul = when (FlowPrefs.normalizeBubbleMode(p.bubbleMode)) {
            "compact" -> 0.75f
            "dot" -> 0.55f
            else -> 1f
        }
        return p.bubbleScale * modeMul
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
        if (listening) return
        bubbleView?.alpha = if (hasField) {
            (prefs?.bubbleOpacity ?: 0.95f)
        } else {
            (prefs?.bubbleOpacity ?: 0.85f) * 0.8f
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
        val gen = ++listenGeneration

        if (prefs?.bubblePulse != false) startPulse()
        updateBubbleVisuals()
        setListenChrome(0)
        setBubbleEmphasis(true)

        val lang = LanguagePolicy.LOCKED
        stt?.setListener(object : SttEngine.Listener {
            private fun live(): Boolean = listening && gen == listenGeneration

            override fun onPartial(text: String) {
                if (!live()) return
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                if (prefs?.bubbleShowText == true) {
                    val preview = if (sessionBuffer.isEmpty()) text
                    else "${sessionBuffer} $text"
                    bubbleLabel?.text = BubbleLabelFormatter.partial(preview, elapsed)
                } else {
                    setListenChrome(elapsed)
                }
            }

            override fun onFinal(text: String) {
                if (!live()) return
                if (text.isBlank()) return
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
                if (!live() && !fatal) return
                val mic = message.contains("Microphone", true) ||
                    message.contains("Allow mic", true)
                val soft = !fatal && !mic && (
                    message.contains("Silence", true) ||
                        message.contains("No match", true) ||
                        message.contains("Busy", true)
                    )
                if (soft) {
                    // Continuous restart noise — keep listening chrome, no error spam.
                    setListenChrome((SystemClock.elapsedRealtime() - listenStartedAt) / 1000)
                    return
                }
                bubbleLabel?.text = if (mic) {
                    BubbleLabelFormatter.needMic()
                } else {
                    message.take(48)
                }
                if (fatal) {
                    listening = false
                    stopPulse()
                    updateBubbleVisuals()
                    setBubbleEmphasis(focusedEditable != null)
                }
            }

            override fun onNeedMicPermission() {
                if (!live()) return
                bubbleLabel?.text = BubbleLabelFormatter.needMic()
                listening = false
            }

            override fun onReady() {
                if (!live()) return
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
                if (!live()) return
                val base = effectiveScale()
                val pulse = BubbleGeometry.rmsScaleY(rmsdB)
                bubbleView?.scaleX = base * pulse
                bubbleView?.scaleY = base * pulse
            }

            override fun onListeningChanged(isOn: Boolean) {
                if (!live()) return
                if (!isOn) {
                    listening = false
                    stopPulse()
                    updateBubbleVisuals()
                }
            }
        })
        stt?.startContinuous(lang)
    }

    private fun stopListening(save: Boolean) {
        listening = false
        pushToTalk = false
        stopPulse()
        listenGeneration++
        stt?.setListener(null)
        stt?.stop()
        val raw = sessionBuffer.toString().trim()
        val prefix = fieldPrefix
        val dur = SystemClock.elapsedRealtime() - listenStartedAt
        sessionBuffer = StringBuilder()
        fieldPrefix = ""
        if (save && raw.isNotBlank()) {
            val lang = LanguagePolicy.LOCKED
            polishSession(raw) { result ->
                val finalText = result.clean
                if (finalText.isNotBlank()) {
                    commitSessionToField(finalText, prefix)
                    prefs?.setLastSession(raw = result.raw, clean = finalText)
                    val wordCount = finalText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                    val retention = prefs?.retentionPolicy ?: "keep"
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            app.dictations.saveDictation(
                                rawText = result.raw,
                                cleanText = finalText,
                                durationMs = dur,
                                languageTag = lang,
                                retentionPolicy = retention
                            )
                        }
                    }
                    DictationNotifier.notifyIfPermitted(this@FlowAccessibilityService, wordCount)
                }
            }
        }
        updateBubbleVisuals()
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

    /**
     * Debug inject: same polish + field commit path as stopListening, no STT.
     * adb: am broadcast -a app.openflow.INJECT_DICTATION -p <pkg> --es text "…"
     */
    fun injectDictation(raw: String) {
        if (!BuildConfig.DEBUG) return
        val text = raw.trim()
        if (text.isBlank()) {
            android.util.Log.w("OpenFlow.Inject", "blank")
            return
        }
        val prefix = captureFieldPrefix()
        val dur = 0L
        polishSession(text) { result ->
            val finalText = result.clean
            android.util.Log.i(
                "OpenFlow.Inject",
                "done cleanLen=${finalText.length} prefixLen=${prefix.length} " +
                    "level=${result.level} fieldOk=${finalText.isNotBlank()}"
            )
            if (finalText.isNotBlank()) {
                commitSessionToField(finalText, prefix)
                prefs?.setLastSession(raw = result.raw, clean = finalText)
                val wordCount = finalText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                val retention = prefs?.retentionPolicy ?: "keep"
                val lang = LanguagePolicy.LOCKED
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        app.dictations.saveDictation(
                            rawText = result.raw,
                            cleanText = finalText,
                            durationMs = dur,
                            languageTag = lang,
                            retentionPolicy = retention
                        )
                    }
                }
                DictationNotifier.notifyIfPermitted(this@FlowAccessibilityService, wordCount)
            }
        }
    }

    private fun registerInjectReceiver() {
        if (!BuildConfig.DEBUG || injectReceiverRegistered) return
        val filter = IntentFilter(ACTION_INJECT)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(injectReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(injectReceiver, filter)
            }
            injectReceiverRegistered = true
            android.util.Log.i("OpenFlow.Inject", "receiver registered action=$ACTION_INJECT")
        } catch (e: Exception) {
            android.util.Log.e("OpenFlow.Inject", "register failed", e)
        }
    }

    private fun unregisterInjectReceiver() {
        if (!injectReceiverRegistered) return
        try {
            unregisterReceiver(injectReceiver)
        } catch (_: Exception) {
        }
        injectReceiverRegistered = false
    }

    /**
     * Same path for stopListening + debug inject:
     * dict → snippets → CleanupPipeline(level, style, custom) via [TextPostProcessor.polishSessionResult].
     */
    private fun polishSession(text: String, onDone: (CleanupResult) -> Unit) {
        scope.launch(Dispatchers.Default) {
            val dict = app.dictations.dictionaryMap()
            val snip = app.dictations.snippetMap()
            val prefLevel = prefs?.cleanupLevel ?: "medium"
            val level = CleanupLevel.fromPref(prefLevel)
            val style = prefs?.style() ?: WritingStyle.CASUAL
            val custom = prefs?.customStyleConfig() ?: CustomStyleConfig()
            val result = TextPostProcessor.polishSessionResult(
                raw = text,
                style = style,
                level = level,
                custom = custom,
                dictionary = dict,
                snippets = snip
            )
            android.util.Log.i(
                "OpenFlow.Cleanup",
                "level=$level pref=$prefLevel style=$style lang=${LanguagePolicy.LOCKED} " +
                    "rawLen=${text.length} cleanLen=${result.clean.length} " +
                    "corr=${result.corrections.size} " +
                    "changed=${text.trim() != result.clean.trim()}"
            )
            mainHandler.post { onDone(result) }
        }
    }

    private fun commitSessionToField(finalText: String, prefix: String) {
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
            Toast.makeText(this, R.string.flow_bubble_saved_in_app, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if (!isUsableEditable(node)) {
                Toast.makeText(this, R.string.flow_bubble_saved_in_app, Toast.LENGTH_SHORT).show()
                return
            }
            val merged = FieldPolicy.mergeSession(prefix, finalText)
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
                Toast.makeText(this, R.string.flow_bubble_saved_in_app, Toast.LENGTH_SHORT).show()
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

    /**
     * Dynamic Shape and Visual Styler:
     * - Pill: wrap width with label + icon, rounded corners
     * - Circle: 56dp x 56dp orb with centered icon
     * - Square / Squircle: 52dp x 52dp rounded superellipse
     * - Dot: 38dp x 38dp compact sphere
     */
    private fun updateBubbleVisuals() {
        val p = prefs ?: return
        val root = bubbleRoot ?: return
        val icon = bubbleIcon ?: return
        val label = bubbleLabel ?: return
        val pulseRing = bubblePulseRing ?: return
        val dm = resources.displayMetrics
        val density = dm.density

        val shape = FlowPrefs.normalizeBubbleShape(p.bubbleShape)
        val mode = FlowPrefs.normalizeBubbleMode(p.bubbleMode)
        val isDot = mode == "dot" || shape == "dot"
        val isCircle = shape == "circle" && !isDot
        val isSquare = shape == "square" && !isDot

        val (bgColor, strokeColor, iconTint) = if (listening) {
            Triple(0xF04F46E5.toInt(), 0xFF818CF8.toInt(), 0xFFFFFFFF.toInt())
        } else {
            Triple(0xF018181B.toInt(), 0x5052525B.toInt(), 0xFFF4F4F5.toInt())
        }

        icon.setColorFilter(iconTint)

        val bgDrawable = GradientDrawable().apply {
            setColor(bgColor)
            setStroke((1.5f * density).toInt().coerceAtLeast(1), strokeColor)
            when {
                isDot -> {
                    this.shape = GradientDrawable.OVAL
                }
                isCircle -> {
                    this.shape = GradientDrawable.OVAL
                }
                isSquare -> {
                    this.shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14f * density
                }
                else -> { // Pill
                    this.shape = GradientDrawable.RECTANGLE
                    cornerRadius = 24f * density
                }
            }
        }
        root.background = bgDrawable

        when {
            isDot -> {
                val size = (38f * density).toInt()
                root.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                root.setPadding(0, 0, 0, 0)
                label.visibility = View.GONE
                icon.visibility = View.VISIBLE
                icon.layoutParams = LinearLayout.LayoutParams((18f * density).toInt(), (18f * density).toInt())
            }
            isCircle -> {
                val size = (56f * density).toInt()
                root.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                root.setPadding(0, 0, 0, 0)
                label.visibility = if (listening && p.bubbleShowText && sessionBuffer.isNotEmpty()) View.VISIBLE else View.GONE
                icon.visibility = View.VISIBLE
                icon.layoutParams = LinearLayout.LayoutParams((24f * density).toInt(), (24f * density).toInt())
            }
            isSquare -> {
                val size = (52f * density).toInt()
                root.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
                root.setPadding(0, 0, 0, 0)
                label.visibility = if (listening && p.bubbleShowText && sessionBuffer.isNotEmpty()) View.VISIBLE else View.GONE
                icon.visibility = View.VISIBLE
                icon.layoutParams = LinearLayout.LayoutParams((22f * density).toInt(), (22f * density).toInt())
            }
            else -> { // Pill
                val padH = (14f * density).toInt()
                val padV = (10f * density).toInt()
                root.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                root.setPadding(padH, padV, padH, padV)
                icon.visibility = View.VISIBLE
                icon.layoutParams = LinearLayout.LayoutParams((22f * density).toInt(), (22f * density).toInt())
                label.visibility = View.VISIBLE
                if (!listening) {
                    label.text = BubbleLabelFormatter.idle()
                }
            }
        }

        if (listening && p.bubblePulse) {
            pulseRing.visibility = View.VISIBLE
            val ringDrawable = GradientDrawable().apply {
                setColor(0x336366F1.toInt())
                if (isDot || isCircle) {
                    this.shape = GradientDrawable.OVAL
                } else {
                    this.shape = GradientDrawable.RECTANGLE
                    cornerRadius = if (isSquare) 16f * density else 26f * density
                }
            }
            pulseRing.background = ringDrawable
        } else {
            pulseRing.visibility = View.GONE
        }
    }

    private fun setListenChrome(elapsedSec: Long) {
        val p = prefs ?: return
        if (p.bubbleShowText) {
            bubbleLabel?.text = BubbleLabelFormatter.listening(elapsedSec)
        } else {
            bubbleLabel?.text = if (elapsedSec > 0) "$elapsedSec s" else "●"
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
        updateBubbleVisuals()
        refreshBubbleVisibility()
    }

    companion object {
        /** Debug broadcast action (any build id; handler no-ops if not DEBUG). */
        const val ACTION_INJECT = "app.openflow.INJECT_DICTATION"
        const val EXTRA_TEXT = "text"

        @Volatile
        var instance: FlowAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
