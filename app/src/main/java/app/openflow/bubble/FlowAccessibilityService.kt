package app.openflow.bubble

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.animation.ValueAnimator
import android.graphics.PixelFormat
import android.graphics.Rect
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
import android.view.accessibility.AccessibilityWindowInfo
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
import app.openflow.stt.SttTuning
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
    private var bubbleCancel: ImageView? = null
    private var bubbleDone: ImageView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var snapAnimator: ValueAnimator? = null

    private var stt: SttEngine? = null
    private var listening = false
    private var pushToTalk = false
    /** True while stopAndFlush waits for last final — blocks re-entrant stop/start. */
    private var stopInProgress = false
    private var focusedEditable: AccessibilityNodeInfo? = null
    private var searchFieldFocused = false
    private var listenStartedAt = 0L
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var prefs: FlowPrefs? = null
    /** Bumps when a listen session fully ends (after flush/commit). */
    private var listenGeneration = 0

    /** Raw STT final segments for this listen (not written to field until stop). */
    private var sessionBuffer = StringBuilder()
    /** Latest partial hypothesis — committed if stop races the engine final. */
    private var lastPartial: String = ""
    /** Field text snapshot at listen start — session rewrite base. */
    private var fieldPrefix: String = ""
    private var lastPackage: String? = null
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val shakeDetector = ShakeDetector()
    private var shakeRegistered = false
    private var injectReceiverRegistered = false
    private var copyReceiverRegistered = false
    private var draggingNow = false
    private var compactVisual = false
    private var lastInteractionAt = 0L
    private var lastCommitAt = -1L
    private var lastRms = 0f
    /** Soft keyboard present (Wispr: bubble lives with field + keyboard). */
    private var imeVisible: Boolean = false
    /** TYPE_INPUT_METHOD height in px. 0 = IME down / unknown. Not written to prefs. */
    private var imeHeightPx: Int = 0

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

    private val copyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_COPY_LAST) return
            copyLastToClipboard()
        }
    }

    private val pulseTick = object : Runnable {
        override fun run() {
            onPulseTick()
            mainHandler.postDelayed(this, 500L)
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
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        val tuning = prefs?.sttTuning() ?: SttTuning()
        stt = SttEngine(
            applicationContext,
            preferOnDevice = true,
            // Mute system STT ding on every continuous restart segment.
            softMuteBeeps = true,
            tuning = tuning
        )
        android.util.Log.i(
            "OpenFlow.Bubble",
            "stt profile=${prefs?.sttProfile} silence=${tuning.completeSilenceMs}ms " +
                "qualityFmt=${tuning.preferFormattingQuality}"
        )
        showBubble()
        instance = this
        lastInteractionAt = SystemClock.elapsedRealtime()
        registerInjectReceiver()
        registerCopyReceiver()
        mainHandler.removeCallbacks(pulseTick)
        mainHandler.post(pulseTick)
        android.util.Log.i("OpenFlow.Bubble", "onServiceConnected overlay=ready debugInject=${BuildConfig.DEBUG}")
        refreshBubbleVisibility()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            event.packageName?.toString()?.let { lastPackage = it }
            // Keyboard open/close → bubble show/hide (Wispr).
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
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                rootInActiveWindow?.let { root ->
                    try {
                        root.packageName?.toString()?.let { lastPackage = it }
                        val focused = findFocusedEditable(root)
                        if (focused != null) {
                            updateFocusFrom(focused)
                            @Suppress("DEPRECATION")
                            focused.recycle()
                        } else if (!listening) {
                            // Lost field — hide bubble (Wispr).
                            focusedEditable?.let {
                                @Suppress("DEPRECATION")
                                it.recycle()
                            }
                            focusedEditable = null
                            searchFieldFocused = false
                            refreshBubbleVisibility()
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
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(pulseTick)
        unregisterShake()
        unregisterInjectReceiver()
        unregisterCopyReceiver()
        DictationNotifier.notifyServiceStopped(this)
        stopListening(save = false)
        hideBubble()
        stt?.destroy()
        stt = null
        focusedEditable?.let {
            @Suppress("DEPRECATION")
            it.recycle()
        }
        focusedEditable = null
        searchFieldFocused = false
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
        searchFieldFocused = target != null && FieldPolicy.isSearch(
            inputType = target.inputType,
            className = target.className?.toString(),
            hintOrDesc = listOfNotNull(
                target.hintText?.toString(),
                target.text?.toString(),
                target.contentDescription?.toString()
            ).joinToString(" ")
        )
        setBubbleEmphasis(target != null)
        refreshBubbleVisibility()
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
        bubbleCancel = view.findViewById(R.id.bubble_cancel)
        bubbleDone = view.findViewById(R.id.bubble_done)

        // Wispr: Cancel discards, Done inserts.
        bubbleCancel?.setOnClickListener {
            if (listening || stopInProgress) stopListening(save = false)
        }
        bubbleDone?.setOnClickListener {
            if (listening || stopInProgress) {
                stopListening(save = true)
            } else if (copyChipVisible()) {
                copyLastToClipboard()
            }
        }

        if (prefs == null) prefs = FlowPrefs(this)
        val p = prefs!!
        val scale = effectiveScale()
        val opacity = p.bubbleOpacity
        refreshImeHeight()
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
            y = parkedY(p.bubbleY)
            alpha = opacity
        }
        bubbleParams = params
        applyOverlayWindowSize()
        view.scaleX = scale
        view.scaleY = scale
        setupTouch(view, params)
        try {
            windowManager?.addView(view, params)
            bubbleView = view
            updateBubbleVisuals()
            setBubbleEmphasis(focusedEditable != null)
            refreshBubbleVisibility()
        } catch (_: Exception) {
            bubbleView = null
        }
    }

    private fun hitVisible(target: android.view.View?, rawX: Float, rawY: Float): Boolean {
        if (target == null || target.visibility != android.view.View.VISIBLE) return false
        val loc = IntArray(2)
        target.getLocationOnScreen(loc)
        val l = loc[0]
        val t = loc[1]
        return rawX >= l && rawX < l + target.width && rawY >= t && rawY < t + target.height
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
            if (!listening && !stopInProgress) startListening()
            if (prefs?.bubbleHaptics != false) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
        }
        view.setOnTouchListener { v, event ->
            // Parent OnTouch returns true — Cancel/Done clicks never fire. Hit-test in ACTION_UP.
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = prefs?.bubbleY ?: params.y
                    dragged = false
                    longPressFired = false
                    // Smooth press-in (Wispr-like).
                    animatePress(v, pressed = true)
                    if (!listening) mainHandler.postDelayed(longPress, 420)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) + abs(dy) > 14) {
                        dragged = true
                        draggingNow = true
                        lastInteractionAt = SystemClock.elapsedRealtime()
                        if (compactVisual) {
                            compactVisual = false
                            applyVisualScale()
                        }
                        mainHandler.removeCallbacks(longPress)
                    }
                    val dm = resources.displayMetrics
                    val h = v.height.takeIf { it > 0 } ?: v.measuredHeight.takeIf { it > 0 } ?: 120
                    params.x = (startX - dx).coerceAtLeast(0)
                    val dragY = BubbleGeometry.clampVerticalOffset(
                        y = startY - dy,
                        screenHeightPx = dm.heightPixels,
                        bubbleHeightPx = h
                    )
                    params.y = BubbleGeometry.parkYAboveIme(dragY, imeHeightPx)
                    if (dragY < 40 && dragged) {
                        bubbleLabel?.visibility = View.VISIBLE
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
                    draggingNow = false
                    lastInteractionAt = SystemClock.elapsedRealtime()
                    animatePress(v, pressed = false)
                    val dm = resources.displayMetrics
                    val h = v.height.takeIf { it > 0 } ?: v.measuredHeight.takeIf { it > 0 } ?: 120
                    val upDy = (event.rawY - downRawY).toInt()
                    val savedY = BubbleGeometry.clampVerticalOffset(
                        y = startY - upDy,
                        screenHeightPx = dm.heightPixels,
                        bubbleHeightPx = h
                    )
                    if (dragged && savedY < 40) {
                        prefs?.snoozeMinutes(10)
                        Toast.makeText(this, R.string.flow_bubble_snoozed, Toast.LENGTH_SHORT).show()
                        refreshBubbleVisibility()
                        return@setOnTouchListener true
                    }
                    if (dragged && prefs?.bubbleEdgeSnap == true) {
                        snapBubbleToEdge(v, params)
                    }
                    if (dragged) {
                        prefs?.let {
                            it.bubbleX = params.x
                            it.bubbleY = savedY
                        }
                        params.y = BubbleGeometry.parkYAboveIme(savedY, imeHeightPx)
                        try {
                            windowManager?.updateViewLayout(v, params)
                        } catch (_: Exception) {
                        }
                    }
                    when (
                        BubbleTapPolicy.action(
                            listening = listening,
                            stopInProgress = stopInProgress,
                            dragged = dragged,
                            longPressFired = longPressFired,
                            hitCancel = hitVisible(bubbleCancel, event.rawX, event.rawY),
                            hitDone = hitVisible(bubbleDone, event.rawX, event.rawY)
                        )
                    ) {
                        BubbleTapPolicy.Action.START -> startListening()
                        BubbleTapPolicy.Action.STOP_SAVE -> stopListening(save = true)
                        BubbleTapPolicy.Action.STOP_DISCARD -> stopListening(save = false)
                        BubbleTapPolicy.Action.NONE -> { }
                    }
                    pushToTalk = false
                    true
                }
                else -> false
            }
        }
    }

    private fun animatePress(view: View, pressed: Boolean) {
        val base = effectiveScale()
        val target = if (pressed) base * 0.92f else base
        view.animate()
            .scaleX(target)
            .scaleY(target)
            .setDuration(110L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun snapBubbleToEdge(view: View, params: WindowManager.LayoutParams) {
        val dm = resources.displayMetrics
        val w = view.width.takeIf { it > 0 } ?: view.measuredWidth.takeIf { it > 0 } ?: 120
        val targetX = BubbleGeometry.snapOffsetFromEnd(
            x = params.x,
            screenWidthPx = dm.widthPixels,
            bubbleWidthPx = w
        )
        snapAnimator?.cancel()
        val startX = params.x
        if (startX == targetX) {
            if (prefs?.bubbleHaptics != false) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            }
            return
        }
        snapAnimator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 240L
            interpolator = OvershootInterpolator(0.9f)
            addUpdateListener { anim ->
                params.x = anim.animatedValue as Int
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (_: Exception) {
                }
            }
            start()
        }
        prefs?.bubbleX = targetX
        if (prefs?.bubbleHaptics != false) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }

    private fun hideBubble() {
        snapAnimator?.cancel()
        snapAnimator = null
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
        bubbleCancel = null
        bubbleDone = null
        bubbleParams = null
    }

    private fun detectImeVisible(): Boolean {
        return try {
            windows?.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } == true
        } catch (_: Exception) {
            false
        }
    }

    private fun detectImeHeight(): Int {
        return try {
            val screenH = resources.displayMetrics.heightPixels
            val rect = Rect()
            var h = 0
            windows?.forEach { w ->
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

    private fun refreshImeHeight() {
        imeHeightPx = detectImeHeight()
        imeVisible = imeHeightPx > 0 || detectImeVisible()
    }

    /** Display y from saved y. Parked value is never written back to prefs. */
    private fun parkedY(savedY: Int): Int {
        val dm = resources.displayMetrics
        val h = bubbleView?.let { v ->
            v.height.takeIf { it > 0 } ?: v.measuredHeight.takeIf { it > 0 }
        } ?: 120
        val clamped = BubbleGeometry.clampVerticalOffset(
            y = savedY,
            screenHeightPx = dm.heightPixels,
            bubbleHeightPx = h
        )
        return BubbleGeometry.parkYAboveIme(clamped, imeHeightPx)
    }

    private fun applyParkedOverlayY() {
        val params = bubbleParams ?: return
        val view = bubbleView ?: return
        params.y = parkedY(prefs?.bubbleY ?: params.y)
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun refreshBubbleVisibility() {
        refreshImeHeight()
        val snoozed = prefs?.isSnoozed() == true
        val bankHide = PackagePolicy.shouldHideBubble(lastPackage)
        val hasField = focusedEditable != null
        // Wispr: show on text field. Keep visible while listening even if focus blips.
        // Soft IME gate: if IME never reported, still show on field (OEM variance).
        val imeGate = if (imeVisible) true else hasField || listening
        val show = BubbleVisibility.shouldShow(
            snoozed = snoozed,
            bankHide = bankHide,
            hasEditable = hasField || listening,
            imeVisible = imeGate,
            alwaysShow = listening
        )
        bubbleView?.visibility = if (show) View.VISIBLE else View.GONE
        if (snoozed) registerShake() else unregisterShake()
        if (show) {
            applyParkedOverlayY()
            updateBubbleVisuals()
        }
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
        val mode = if (compactVisual && !listening) {
            "compact"
        } else {
            FlowPrefs.normalizeBubbleMode(p.bubbleMode)
        }
        val modeMul = when (mode) {
            "compact" -> 0.75f
            "dot" -> 0.55f
            else -> 1f
        }
        val searchMul = if (searchFieldFocused && !listening) 0.72f else 1f
        return p.bubbleScale * modeMul * searchMul
    }

    private fun applyVisualScale() {
        val s = effectiveScale()
        bubbleView?.scaleX = s
        bubbleView?.scaleY = s
    }

    private fun copyChipAgeMs(): Long {
        if (lastCommitAt < 0L) return -1L
        return SystemClock.elapsedRealtime() - lastCommitAt
    }

    private fun copyChipVisible(): Boolean =
        CopyChip.shouldShow(copyChipAgeMs(), listening)

    private fun refreshCopyChip() {
        if (listening) return
        val show = copyChipVisible()
        bubbleDone?.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            bubbleDone?.setColorFilter(BubbleChrome.DONE)
            bubbleLabel?.visibility = View.VISIBLE
            bubbleLabel?.text = "Copy"
            val density = resources.displayMetrics.density
            val padH = (10f * density).toInt()
            val padV = (8f * density).toInt()
            bubbleRoot?.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            bubbleRoot?.setPadding(padH, padV, padH, padV)
        }
        applyOverlayWindowSize()
    }

    private fun copyLastToClipboard() {
        val text = prefs?.lastSessionClean.orEmpty()
        if (text.isBlank()) {
            Toast.makeText(this, R.string.flow_bubble_saved_in_app, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Open Flow", text))
            Toast.makeText(this, R.string.flow_bubble_copied_clipboard, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }
    }

    private fun onPulseTick() {
        val now = SystemClock.elapsedRealtime()
        if (listening) {
            bubbleView?.keepScreenOn = true
            val elapsed = now - listenStartedAt
            when (SessionGuard.phase(elapsed)) {
                SessionPhase.STOP -> {
                    if (!stopInProgress) stopListening(save = true)
                    return
                }
                SessionPhase.WARN -> {
                    val sec = elapsed / 1000
                    if (prefs?.bubbleShowText == true) {
                        bubbleLabel?.text = "Wrap up ${sec}s"
                    } else {
                        setListenChrome(sec)
                    }
                }
                SessionPhase.NONE -> { }
            }
        }
        val wantCompact = IdleShrink.shouldCompact(
            idleMs = now - lastInteractionAt,
            listening = listening,
            dragging = draggingNow
        )
        if (wantCompact != compactVisual && !listening) {
            compactVisual = wantCompact
            applyVisualScale()
        }
        if (!listening) refreshCopyChip()
    }

    private fun setBubbleEmphasis(hasField: Boolean) {
        if (listening) return
        bubbleView?.alpha = if (hasField) {
            (prefs?.bubbleOpacity ?: 0.95f)
        } else {
            (prefs?.bubbleOpacity ?: 0.85f) * 0.8f
        }
        val base = effectiveScale()
        bubbleView?.scaleX = base
        bubbleView?.scaleY = base
    }

    private fun startListening() {
        if (stopInProgress) return
        if (listening) return
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
        compactVisual = false
        sessionBuffer = StringBuilder()
        lastPartial = ""
        fieldPrefix = captureFieldPrefix()
        listenStartedAt = SystemClock.elapsedRealtime()
        lastInteractionAt = listenStartedAt
        bubbleView?.keepScreenOn = true
        val gen = ++listenGeneration

        updateBubbleVisuals()
        setListenChrome(0)
        setBubbleEmphasis(true)

        val lang = LanguagePolicy.LOCKED
        stt?.setListener(object : SttEngine.Listener {
            /** Accept STT while this generation is active (including flush). */
            private fun live(): Boolean = gen == listenGeneration

            override fun onPartial(text: String) {
                if (!live()) return
                lastPartial = text.trim()
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                if (prefs?.bubbleShowText == true) {
                    val preview = SessionText.commitRaw(sessionBuffer.toString(), lastPartial)
                    bubbleLabel?.text = BubbleLabelFormatter.partial(preview, elapsed)
                    ensureLabelVisibleForText()
                } else {
                    setListenChrome(elapsed)
                }
            }

            override fun onFinal(text: String) {
                if (!live()) return
                if (text.isBlank()) return
                if (sessionBuffer.isNotEmpty()) sessionBuffer.append(' ')
                sessionBuffer.append(text.trim())
                // Final absorbed this hypothesis — clear partial to avoid double-commit.
                lastPartial = ""
                if (prefs?.bubbleShowText == true) {
                    bubbleLabel?.text = BubbleLabelFormatter.finalChunk(sessionBuffer.toString())
                    ensureLabelVisibleForText()
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
                        message.contains("Busy", true) ||
                        message.contains("No recognition", true) ||
                        message.contains("Retrying", true)
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
                if (fatal && !stopInProgress) {
                    // Engine dead — still commit any speech we have.
                    stopListening(save = true)
                }
            }

            override fun onNeedMicPermission() {
                if (!live()) return
                bubbleLabel?.text = BubbleLabelFormatter.needMic()
                if (!stopInProgress) stopListening(save = false)
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
                if (!live() || !listening) return
                lastRms = rmsdB
                if (prefs?.bubbleShowText != true) {
                    val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                    setListenChrome(elapsed)
                }
                if (prefs?.bubblePulse == false) return
                val base = effectiveScale()
                val pulse = BubbleGeometry.rmsScaleY(rmsdB)
                bubbleView?.scaleX = base * pulse
                bubbleView?.scaleY = base * pulse
            }

            override fun onListeningChanged(isOn: Boolean) {
                if (!live()) return
                // Engine ended continuous without user stop (fatal / max restarts).
                if (!isOn && listening && !stopInProgress) {
                    stopListening(save = true)
                }
            }
        })
        stt?.startContinuous(lang)
        android.util.Log.i("OpenFlow.Bubble", "listen start gen=$gen")
    }

    private fun stopListening(save: Boolean) {
        if (stopInProgress) return
        if (!listening && sessionBuffer.isEmpty() && lastPartial.isEmpty()) {
            // Nothing to flush.
            pushToTalk = false
            return
        }
        stopInProgress = true
        pushToTalk = false
        hapticSaveOrDiscard(save)
        // Keep listenGeneration stable so onFinal during flush is accepted.
        val gen = listenGeneration
        val prefix = fieldPrefix
        val dur = SystemClock.elapsedRealtime() - listenStartedAt
        bubbleLabel?.text = "…"

        fun finishCommit() {
            if (gen != listenGeneration) {
                stopInProgress = false
                return
            }
            val raw = SessionText.commitRaw(sessionBuffer.toString(), lastPartial)
            android.util.Log.i(
                "OpenFlow.Bubble",
                "stop save=$save rawLen=${raw.length} finalsLen=${sessionBuffer.length} " +
                    "partialLen=${lastPartial.length} gen=$gen"
            )
            listenGeneration++
            listening = false
            bubbleView?.keepScreenOn = false
            sessionBuffer = StringBuilder()
            lastPartial = ""
            fieldPrefix = ""
            stt?.setListener(null)
            stopInProgress = false

            if (save && raw.isNotBlank()) {
                val lang = LanguagePolicy.LOCKED
                polishSession(raw) { result ->
                    val finalText = result.clean
                    android.util.Log.i(
                        "OpenFlow.Bubble",
                        "commit cleanLen=${finalText.length} prefixLen=${prefix.length}"
                    )
                    if (finalText.isNotBlank()) {
                        commitSessionToField(finalText, prefix)
                        prefs?.setLastSession(raw = result.raw, clean = finalText)
                        lastCommitAt = SystemClock.elapsedRealtime()
                        lastInteractionAt = lastCommitAt
                        refreshCopyChip()
                        val wordCount = finalText.split(Regex("\\s+"))
                            .filter { it.isNotBlank() }.size
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
                        DictationNotifier.notifyIfPermitted(
                            this@FlowAccessibilityService,
                            wordCount
                        )
                    }
                }
            } else if (save && raw.isBlank()) {
                bubbleLabel?.text = BubbleLabelFormatter.idle()
            }
            updateBubbleVisuals()
            setBubbleEmphasis(focusedEditable != null)
        }

        // Prefer graceful drain so last final lands; timeout still commits partial.
        val engine = stt
        if (engine != null) {
            engine.stopAndFlush(SttEngine.DEFAULT_FLUSH_TIMEOUT_MS) {
                mainHandler.post { finishCommit() }
            }
        } else {
            finishCommit()
        }
    }

    /** Circle/square hide label at idle; force visible while showing live text. */
    private fun ensureLabelVisibleForText() {
        if (prefs?.bubbleShowText != true) return
        bubbleLabel?.visibility = View.VISIBLE
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
                lastCommitAt = SystemClock.elapsedRealtime()
                lastInteractionAt = lastCommitAt
                refreshCopyChip()
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

    private fun registerCopyReceiver() {
        if (copyReceiverRegistered) return
        val filter = IntentFilter(ACTION_COPY_LAST)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(copyReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(copyReceiver, filter)
            }
            copyReceiverRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun unregisterCopyReceiver() {
        if (!copyReceiverRegistered) return
        try {
            unregisterReceiver(copyReceiver)
        } catch (_: Exception) {
        }
        copyReceiverRegistered = false
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
            val style = AppStylePolicy.styleFor(
                lastPackage,
                prefs?.style() ?: WritingStyle.CASUAL
            )
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
            android.util.Log.i(
                "OpenFlow.Bubble",
                "setText ok=$ok mergedLen=${merged.length} class=${node.className}"
            )
            if (!ok) {
                // Wispr-style fallback: put on clipboard so user can paste.
                try {
                    val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(
                        android.content.ClipData.newPlainText("Open Flow", merged)
                    )
                    Toast.makeText(this, R.string.flow_bubble_copied_clipboard, Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.flow_bubble_saved_in_app, Toast.LENGTH_SHORT).show()
                }
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
     * Minimal brutal chrome:
     * - Idle: hard charcoal tile + cream stroke + mic
     * - Listening: hard bar Cancel | status | Done (no purple)
     * Shape pref: circle/dot/pill/square; default square (product brutal).
     */
    private fun updateBubbleVisuals() {
        val p = prefs ?: return
        val root = bubbleRoot ?: return
        val icon = bubbleIcon ?: return
        val label = bubbleLabel ?: return
        val pulseRing = bubblePulseRing ?: return
        val cancel = bubbleCancel
        val done = bubbleDone
        val density = resources.displayMetrics.density
        val shape = FlowPrefs.normalizeBubbleShape(p.bubbleShape)

        val mode = FlowPrefs.normalizeBubbleMode(p.bubbleMode)
        val orbDp = when (mode) {
            "dot" -> 40f
            "compact" -> 48f
            else -> 52f
        }
        val stroke = BubbleChrome.strokePx(density)

        if (listening) {
            val bg = GradientDrawable().apply {
                this.shape = GradientDrawable.RECTANGLE
                cornerRadius = BubbleChrome.cornerPx("listen", density)
                setColor(BubbleChrome.LISTEN_FILL)
                setStroke(stroke, BubbleChrome.LISTEN_STROKE)
            }
            root.background = bg
            root.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            val padH = (10f * density).toInt()
            val padV = (8f * density).toInt()
            root.setPadding(padH, padV, padH, padV)

            cancel?.visibility = View.VISIBLE
            cancel?.setColorFilter(BubbleChrome.CANCEL)
            done?.visibility = View.VISIBLE
            done?.setColorFilter(BubbleChrome.DONE)
            icon.visibility = View.VISIBLE
            icon.setColorFilter(BubbleChrome.ICON)
            icon.layoutParams = LinearLayout.LayoutParams(
                (20f * density).toInt(),
                (20f * density).toInt()
            ).apply { gravity = Gravity.CENTER }
            label.visibility = View.VISIBLE
            label.setTextColor(BubbleChrome.LABEL)
            if (label.text.isNullOrBlank()) {
                label.text = BubbleLabelFormatter.listening(0)
            }

            // Pulse WRAP_CONTENT was the grey veil. Idle-only; never on listen.
            pulseRing.visibility = View.GONE
        } else {
            cancel?.visibility = View.GONE
            done?.visibility = View.GONE
            label.visibility = View.GONE
            pulseRing.visibility = View.GONE

            val size = (orbDp * density).toInt()
            root.layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            root.setPadding(0, 0, 0, 0)
            val useOval = shape == "circle" || shape == "dot"
            root.background = GradientDrawable().apply {
                this.shape = if (useOval) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
                if (!useOval) {
                    cornerRadius = BubbleChrome.cornerPx(shape, density)
                }
                setColor(BubbleChrome.IDLE_FILL)
                setStroke(stroke, BubbleChrome.IDLE_STROKE)
            }
            icon.visibility = View.VISIBLE
            icon.setColorFilter(BubbleChrome.ICON)
            val iconSz = (orbDp * 0.42f * density).toInt()
            icon.layoutParams = LinearLayout.LayoutParams(iconSz, iconSz).apply {
                gravity = Gravity.CENTER
            }
        }
        applyOverlayWindowSize()
    }

    /** Pin overlay window. WRAP_CONTENT measures against the screen. */
    private fun applyOverlayWindowSize() {
        val params = bubbleParams ?: return
        val density = resources.displayMetrics.density
        val shape = FlowPrefs.normalizeBubbleShape(prefs?.bubbleShape.orEmpty())
        val wide = listening || copyChipVisible()
        val (w, h) = BubbleGeometry.overlaySizePx(wide, density, shape)
        params.width = if (w > 0) w else WindowManager.LayoutParams.WRAP_CONTENT
        params.height = if (h > 0) h else WindowManager.LayoutParams.WRAP_CONTENT
        val view = bubbleView ?: return
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun hapticSaveOrDiscard(save: Boolean) {
        if (prefs?.bubbleHaptics == false) return
        val view = bubbleView ?: return
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (save) {
                android.view.HapticFeedbackConstants.CONFIRM
            } else {
                android.view.HapticFeedbackConstants.REJECT
            }
        } else {
            android.view.HapticFeedbackConstants.CONTEXT_CLICK
        }
        view.performHapticFeedback(constant)
    }

    private fun setListenChrome(elapsedSec: Long) {
        val p = prefs ?: return
        bubbleLabel?.visibility = View.VISIBLE
        if (p.bubbleShowText) {
            bubbleLabel?.text = BubbleLabelFormatter.listening(elapsedSec)
        } else {
            val bars = WaveformBars.fromRms(lastRms)
            val warn = SessionGuard.phase(elapsedSec * 1000L) == SessionPhase.WARN
            val suffix = if (warn) " wrap" else if (elapsedSec > 0) "  ${elapsedSec}s" else ""
            bubbleLabel?.text = bars + suffix
        }
    }

    fun applyPrefsVisual() {
        val p = prefs ?: return
        val s = effectiveScale()
        bubbleView?.scaleX = s
        bubbleView?.scaleY = s
        bubbleParams?.alpha = p.bubbleOpacity
        refreshImeHeight()
        applyParkedOverlayY()
        updateBubbleVisuals()
        refreshBubbleVisibility()
    }

    companion object {
        /** Debug broadcast action (any build id; handler no-ops if not DEBUG). */
        const val ACTION_INJECT = "app.openflow.INJECT_DICTATION"
        const val ACTION_COPY_LAST = "app.openflow.COPY_LAST"
        const val EXTRA_TEXT = "text"

        @Volatile
        var instance: FlowAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
