package app.openflow.bubble

import android.annotation.SuppressLint
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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import app.openflow.BuildConfig
import app.openflow.OpenFlowApp
import app.openflow.R
import app.openflow.ai.NoAI
import app.openflow.audio.AudioFileManager
import app.openflow.audio.SessionAudioCapture
import app.openflow.data.ProcessStatus
import app.openflow.notify.DictationNotifier
import app.openflow.metrics.SessionLatency
import java.util.UUID
import app.openflow.orchestrate.ProviderHealth
import app.openflow.orchestrate.RouteSignals
import app.openflow.orchestrate.SttRouter
import app.openflow.prefs.FlowPrefs
import app.openflow.ui.HapticFeel
import app.openflow.ui.HapticPick
import app.openflow.runtime.TrimPolicy
import app.openflow.stt.providers.cloud.CloudEar
import app.openflow.stt.AndroidSpeechEngine
import app.openflow.stt.CloudFallbackNotice
import app.openflow.stt.LanguagePolicy
import app.openflow.stt.MainThreadHop
import app.openflow.stt.EarMicPolicy
import app.openflow.stt.SpeechEngine
import app.openflow.stt.SttBias
import app.openflow.stt.SttEngine
import app.openflow.stt.providers.ondevice.OnDeviceEar
import app.openflow.text.CleanupBudget
import app.openflow.text.CleanupResult
import app.openflow.text.CommandMode
import app.openflow.text.CustomStyleConfig
import app.openflow.text.InsertPolish
import app.openflow.text.PressEnterPolicy
import app.openflow.text.LearnEngine
import app.openflow.text.TextPostProcessor
import app.openflow.text.VoiceCommands
import app.openflow.text.StyleResolvePolicy
import app.openflow.text.WritingStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

    private val win by lazy {
        BubbleWindowController(
            context = this,
            windowsProvider = { windows },
            hapticTap = { hapticEvent(HapticFeel.Event.TAP) },
        )
    }
    private val painter by lazy {
        BubbleVisualPainter(win, object : BubbleVisualPainter.Env {
            override val prefs: FlowPrefs? get() = this@FlowAccessibilityService.prefs
            override val listening: Boolean get() = this@FlowAccessibilityService.listening
            override fun postStopActive(): Boolean = this@FlowAccessibilityService.postStopActive()
            override fun density(): Float = resources.displayMetrics.density
            override val filesDir: java.io.File get() = this@FlowAccessibilityService.filesDir
            override val contentResolver: android.content.ContentResolver
                get() = this@FlowAccessibilityService.contentResolver
        })
    }

    /** Live listen ear from [OpenFlowApp.currentEar]. Not a raw [SttEngine]. */
    private var ear: SpeechEngine? = null
    private var sessionEarId: String = "system"
    private val providerHealth = ProviderHealth()
    private var listening = false
    private var pushToTalk = false
    /** True while stopAndFlush waits for last final — blocks re-entrant stop/start. */
    private var stopInProgress = false
    private var focusedEditable: AccessibilityNodeInfo? = null
    private var searchFieldFocused = false
    private var listenStartedAt = 0L
    /** Wall-clock listen start — stored as dictation createdAt. */
    private var listenStartedWallMs = 0L
    private var sessionId: String = ""
    private var retrySessionId: String? = null
    private var overlayAttempts = 0
    private val overlayRetry = Runnable { showOverlayOrRecover() }
    private val sessionAudio = SessionAudioCapture()
    private val audioFiles by lazy { AudioFileManager(this) }
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
    @Volatile private var lastKeepCap: Set<String> = emptySet()
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val shakeDetector = ShakeDetector()
    private var shakeRegistered = false
    private var injectReceiverRegistered = false
    private var copyReceiverRegistered = false
    private var draggingNow = false
    private var compactVisual = false
    private var lastInteractionAt = 0L
    private var lastRms = 0f
    private var listenAwake = false
    private val dragCache = BubbleDragCache()
    private var lastVisibilityRefreshAt = 0L
    private var lastRmsBars: String = ""
    private var lastRmsLabelAt = 0L
    private var lastWarnSec = -1L
    private var latencyTrace: SessionLatency? = null

    /** Last successful field write — watch for user fix (auto-learn). */
    private var lastInserted: String = ""
    private var lastInsertAt: Long = 0L
    private var lastInsertPkg: String? = null
    private var undoSnap: UndoInsert.Snapshot? = null
    private var postStopAt: Long = 0L
    private var lastInsertOk: Boolean = false

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
            val now = SystemClock.elapsedRealtime()
            mainHandler.postDelayed(
                this,
                PulseSchedule.nextDelay(
                    listening = listening,
                    chipsVisible = !listening && postStopAt > 0L &&
                        PostStopChips.visible(now - postStopAt),
                    dragging = draggingNow,
                    idleMs = now - lastInteractionAt,
                ),
            )
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
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        if (BuildConfig.DEBUG) {
            android.util.Log.i(
                "OpenFlow.Bubble",
                "ear from registry pick=${app.enginePrefs.earId}"
            )
        }
        if (prefs == null) prefs = FlowPrefs(this)
        val p = prefs ?: run {
            // Prefs init failed — overlay cannot function. Log + bail, no crash.
            if (BuildConfig.DEBUG) {
                android.util.Log.e("OpenFlow.Bubble", "FlowPrefs init failed — overlay disabled")
            }
            return
        }
        win.prefs = p
        overlayAttempts = 0
        showOverlayOrRecover()
        instance = this
        lastInteractionAt = SystemClock.elapsedRealtime()
        registerInjectReceiver()
        registerCopyReceiver()
        mainHandler.removeCallbacks(pulseTick)
        mainHandler.post(pulseTick)
        if (BuildConfig.DEBUG) {
            android.util.Log.i("OpenFlow.Bubble", "onServiceConnected overlay=ready debugInject=${BuildConfig.DEBUG}")
        }
        refreshBubbleVisibility()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // Every event can carry the foreground package — do not wait for
        // WINDOW_STATE_CHANGED alone (task restore / am start can skip it).
        lastPackage = ActivePackageTracker.remember(lastPackage, event.packageName?.toString())
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            // Keyboard open/close → bubble show/hide (Wispr).
            refreshBubbleVisibility(force = false)
        }
        if (prefs?.isSnoozed() == true) return
        if (ActivePackageTracker.shouldIgnoreFocus(lastPackage)) {
            refreshBubbleVisibility(force = false)
            return
        }
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
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> maybeLearnFromTextChanged(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                rootInActiveWindow?.let { root ->
                    try {
                        lastPackage = ActivePackageTracker.remember(
                            lastPackage,
                            root.packageName?.toString(),
                        )
                        val focused = FieldFocusResolver.findFocusedEditable(root)
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
                            refreshBubbleVisibility(force = false)
                        }
                    } finally {
                        @Suppress("DEPRECATION")
                        root.recycle()
                    }
                }
            }
        }
    }

    private fun maybeLearnFromTextChanged(event: AccessibilityEvent) {
        val p = prefs ?: return
        if (!p.autoLearn) return
        if (listening || stopInProgress) return
        if (!LearnEngine.shouldWatch(SystemClock.elapsedRealtime(), lastInsertAt)) return
        val pkg = event.packageName?.toString()
        if (lastInsertPkg != null && pkg != null && pkg != lastInsertPkg) return
        val source = event.source
        try {
            if (source != null) {
                if (source.isPassword) return
                if (FieldPolicy.isSensitive(
                        isPassword = source.isPassword,
                        inputType = source.inputType,
                        className = source.className?.toString(),
                        hintOrDesc = listOfNotNull(
                            source.hintText?.toString(),
                            source.contentDescription?.toString()
                        ).joinToString(" ")
                    )
                ) {
                    return
                }
            }
            val newText = source?.text?.toString()
                ?: event.text?.joinToString("").orEmpty()
            if (LearnEngine.isOwnSet(newText, lastInserted)) return
            val from = lastInserted
            lastInserted = newText
            val deltaMs = SystemClock.elapsedRealtime() - lastInsertAt
            scope.launch(Dispatchers.IO) {
                runCatching { app.dictations.learnFromEdit(from, newText, deltaMs) }
            }
        } finally {
            if (source != null) {
                try {
                    @Suppress("DEPRECATION")
                    source.recycle()
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onInterrupt() {
        stopListening(save = false)
        win.hide()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(pulseTick)
        mainHandler.removeCallbacks(overlayRetry)
        unregisterShake()
        unregisterInjectReceiver()
        unregisterCopyReceiver()
        DictationNotifier.notifyServiceStopped(this)
        stopListening(save = false)
        win.hide()
        ear?.setListener(null)
        ear?.stop()
        ear = null
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

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        dropIdleSttIfNeeded(level)
    }

    /** App sets [OpenFlowApp.dropIdleStt]; service actually stops idle ear. */
    private fun dropIdleSttIfNeeded(level: Int = app.lastTrimLevel) {
        val lvl = if (app.dropIdleStt) {
            maxOf(level, TrimPolicy.TRIM_MEMORY_BACKGROUND)
        } else {
            level
        }
        if (!TrimPolicy.dropIdleEngine(lvl, listening, stopInProgress)) return
        ear?.stop()
        ear = null
        lastRms = 0f
    }

    private fun updateFocusFrom(node: AccessibilityNodeInfo) {
        val target = when {
            FieldFocusResolver.isUsableEditable(node) -> {
                @Suppress("DEPRECATION")
                AccessibilityNodeInfo.obtain(node)
            }
            else -> FieldFocusResolver.findEditableInSubtree(node)
        }
        focusedEditable?.let {
            @Suppress("DEPRECATION")
            it.recycle()
        }
        focusedEditable = target
        searchFieldFocused = target != null && FieldPolicy.isSearch(
            inputType = target.inputType,
            className = target.className?.toString(),
            hintOrDesc = FieldPolicy.skipHints(
                target.hintText?.toString(),
                target.contentDescription?.toString()
            )
        )
        setBubbleEmphasis(target != null)
        refreshBubbleVisibility()
    }

    /**
     * Overlay addView can fail transiently (boot token races, OEM overlay
     * permission). Retry with backoff; after exhaustion post an honest nudge.
     */
    private fun showOverlayOrRecover() {
        val p = prefs ?: return
        val added = win.show(
            scale = effectiveScale(),
            opacity = p.bubbleOpacity,
            savedX = p.bubbleX,
            savedY = p.bubbleY,
            onCancel = { if (listening || stopInProgress) stopListening(save = false) },
            onDone = { if (listening || stopInProgress) stopListening(save = true) },
            setupTouch = { v, lp -> setupTouch(v, lp) },
            onAdded = {
                painter.paint()
                setBubbleEmphasis(focusedEditable != null)
                refreshBubbleVisibility()
            },
        )
        if (added || win.view != null) {
            overlayAttempts = 0
            return
        }
        if (BuildConfig.DEBUG) {
            android.util.Log.w("OpenFlow.Bubble", "overlay addView failed attempt=$overlayAttempts")
        }
        if (OverlayRecoveryPolicy.shouldRetry(overlayAttempts)) {
            mainHandler.removeCallbacks(overlayRetry)
            mainHandler.postDelayed(overlayRetry, OverlayRecoveryPolicy.delayFor(overlayAttempts))
        } else {
            DictationNotifier.notifyOverlayFailed(this)
        }
        overlayAttempts++
    }

    private fun setupTouch(view: View, params: WindowManager.LayoutParams) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        var longPressFired = false
        var lastVx = 0f
        var velocityTracker: VelocityTracker? = null
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val longPress = Runnable {
            longPressFired = true
            pushToTalk = true
            if (!listening && !stopInProgress) startListening()
            hapticEvent(HapticFeel.Event.TAP)
        }
        view.setOnTouchListener { v, event ->
            // Parent OnTouch returns true — Cancel/Done clicks never fire. Hit-test in ACTION_UP.
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    win.snapAnimator?.cancel()
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                    lastVx = 0f
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = prefs?.bubbleY ?: params.y
                    dragged = false
                    longPressFired = false
                    val downW = v.width.takeIf { it > 0 } ?: v.measuredWidth.takeIf { it > 0 } ?: 96
                    val downH = v.height.takeIf { it > 0 } ?: v.measuredHeight.takeIf { it > 0 } ?: 120
                    dragCache.begin(downW, downH)
                    animatePress(v, pressed = true)
                    if (!listening) mainHandler.postDelayed(longPress, 420)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addMovement(event)
                    velocityTracker?.computeCurrentVelocity(1000)
                    lastVx = velocityTracker?.xVelocity ?: 0f
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (!dragged && BubbleMotion.passedSlop(dx, dy, slop)) {
                        dragged = true
                        draggingNow = true
                        lastInteractionAt = SystemClock.elapsedRealtime()
                        if (compactVisual) {
                            compactVisual = false
                            applyVisualScale()
                        }
                        mainHandler.removeCallbacks(longPress)
                    }
                    if (!BubbleMotion.shouldUpdateLayout(dragged)) return@setOnTouchListener true
                    val dm = resources.displayMetrics
                    val h = dragCache.sizeOr(
                        v.width.takeIf { it > 0 } ?: v.measuredWidth.takeIf { it > 0 } ?: 96,
                        v.height.takeIf { it > 0 } ?: v.measuredHeight.takeIf { it > 0 } ?: 120,
                    ).second
                    params.x = (startX - dx).coerceAtLeast(0)
                    val dragY = BubbleGeometry.clampVerticalOffset(
                        y = startY - dy,
                        screenHeightPx = dm.heightPixels,
                        bubbleHeightPx = h
                    )
                    params.y = BubbleGeometry.parkYAboveIme(dragY, win.imeHeightPx)
                    if (dragY < 40) {
                        win.bubbleLabel?.visibility = View.VISIBLE
                        win.bubbleLabel?.text = getString(R.string.flow_bubble_snooze_hint)
                    }
                    try {
                        win.updateLayout(v, params)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mainHandler.removeCallbacks(longPress)
                    draggingNow = false
                    lastInteractionAt = SystemClock.elapsedRealtime()
                    animatePress(v, pressed = false)
                    velocityTracker?.recycle()
                    velocityTracker = null
                    val dm = resources.displayMetrics
                    val h = dragCache.sizeOr(
                        v.width.takeIf { it > 0 } ?: v.measuredWidth.takeIf { it > 0 } ?: 96,
                        v.height.takeIf { it > 0 } ?: v.measuredHeight.takeIf { it > 0 } ?: 120,
                    ).second
                    dragCache.clear()
                    val upDy = (event.rawY - downRawY).toInt()
                    val savedY = BubbleGeometry.clampVerticalOffset(
                        y = startY - upDy,
                        screenHeightPx = dm.heightPixels,
                        bubbleHeightPx = h
                    )
                    if (dragged && savedY < 40) {
                        if (BubbleSnoozePolicy.canSnooze(
                                imeVisible = win.imeVisible,
                                listening = listening,
                                repairShowing = micRepairShowing(),
                            )
                        ) {
                            prefs?.snoozeMinutes(10)
                            Toast.makeText(this, R.string.flow_bubble_snoozed, Toast.LENGTH_SHORT).show()
                            refreshBubbleVisibility()
                            return@setOnTouchListener true
                        }
                    }
                    val snapping = dragged && prefs?.bubbleEdgeSnap == true
                    params.y = BubbleGeometry.parkYAboveIme(savedY, win.imeHeightPx)
                    if (snapping) {
                        win.snapToEdge(v, params, lastVx)
                    } else if (dragged) {
                        prefs?.let {
                            it.bubbleX = params.x
                            it.bubbleY = savedY
                        }
                        try {
                            win.updateLayout(v, params)
                        } catch (_: Exception) {
                        }
                    }
                    if (dragged && snapping) {
                        prefs?.bubbleY = savedY
                    }
                    when (
                        BubbleTapPolicy.action(
                            listening = listening,
                            stopInProgress = stopInProgress,
                            dragged = dragged,
                            longPressFired = longPressFired,
                            hitCancel = win.hitVisible(win.bubbleCancel, event.rawX, event.rawY),
                            hitDone = win.hitVisible(win.bubbleDone, event.rawX, event.rawY),
                            hitCopy = win.hitVisible(win.bubbleChipCopy, event.rawX, event.rawY),
                            hitUndo = win.hitVisible(win.bubbleChipUndo, event.rawX, event.rawY),
                            hitPaste = win.hitVisible(win.bubbleChipPaste, event.rawX, event.rawY),
                            hitLang = win.hitVisible(win.bubbleChipLang, event.rawX, event.rawY),
                        )
                    ) {
                        BubbleTapPolicy.Action.START -> startListening()
                        BubbleTapPolicy.Action.STOP_SAVE -> stopListening(save = true)
                        BubbleTapPolicy.Action.STOP_DISCARD -> stopListening(save = false)
                        BubbleTapPolicy.Action.COPY -> copyLastToClipboard()
                        BubbleTapPolicy.Action.UNDO -> undoLastInsert()
                        BubbleTapPolicy.Action.PASTE -> pasteClipboardIntoField()
                        BubbleTapPolicy.Action.LANG_CYCLE -> cycleLanguage()
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
        win.animatePress(view, pressed, effectiveScale())
    }



    private fun refreshBubbleVisibility(force: Boolean = true) {
        val now = SystemClock.elapsedRealtime()
        if (!BubbleRedrawPolicy.shouldRefreshVisibility(lastVisibilityRefreshAt, now, force)) {
            return
        }
        lastVisibilityRefreshAt = now
        win.refreshImeHeight()
        val snoozed = prefs?.isSnoozed() == true
        val activePkg = try {
            rootInActiveWindow?.packageName?.toString()
        } catch (_: Exception) {
            null
        }
        lastPackage = ActivePackageTracker.remember(lastPackage, activePkg)
        val bankHide = PackagePolicy.shouldHideBubble(lastPackage)
        if (BubbleVisibility.shouldAbortListen(bankHide, listening)) {
            stopListening(false)
        }
        val hasField = focusedEditable != null
        val imeGate = if (win.imeVisible) true else hasField || listening
        val micRepair = micRepairShowing()
        val tileHidden = prefs?.bubbleHidden == true && !listening
        val show = BubbleVisibility.effectiveVisible(
            show = BubbleVisibility.shouldShow(
                snoozed = snoozed,
                bankHide = bankHide,
                hasEditable = hasField || listening,
                imeVisible = imeGate,
                alwaysShow = listening,
                listening = listening,
                insideOwnApp = PackagePolicy.isOwnApp(lastPackage),
                mustStay = micRepair,
            ),
            tileHidden = tileHidden,
        )
        win.view?.visibility = if (show) View.VISIBLE else View.GONE
        if (snoozed) registerShake() else unregisterShake()
        refreshLangChip(show && !listening)
        if (show) {
            win.applyParkedOverlayY()
            painter.paint()
        }
    }

    private fun refreshLangChip(idle: Boolean) {
        val chip = win.bubbleChipLang ?: return
        if (!idle) {
            chip.visibility = View.GONE
            return
        }
        val tag = prefs?.languageTag
        chip.text = LanguageCyclePolicy.badge(tag)
        chip.visibility = View.VISIBLE
    }

    private fun cycleLanguage() {
        val prefs = prefs ?: return
        val tags = LanguagePolicy.SUPPORTED_LANGUAGES.map { it.tag }
        val next = LanguageCyclePolicy.next(prefs.languageTag, tags)
        prefs.languageTag = next
        val name = LanguagePolicy.SUPPORTED_LANGUAGES
            .firstOrNull { it.tag == next }?.displayName ?: next
        Toast.makeText(this, getString(R.string.flow_bubble_lang_set, name), Toast.LENGTH_SHORT)
            .show()
        hapticEvent(HapticFeel.Event.TAP)
        refreshLangChip(idle = true)
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
        return BubbleVisualDriver.effectiveScale(
            scale = p.bubbleScale,
            shrinkIdle = p.bubbleShrinkIdle,
            shrinkDot = p.bubbleShrinkDot,
            shrinkSearch = p.bubbleShrinkSearch,
            listening = listening,
            searchFieldFocused = searchFieldFocused,
        )
    }

    private fun micRepairShowing(): Boolean {
        val t = win.bubbleLabel?.text?.toString().orEmpty()
        return t == BubbleLabelFormatter.needMic() || t == getString(R.string.flow_bubble_need_mic)
    }

    private fun applyVisualScale() {
        val s = effectiveScale()
        win.view?.scaleX = s
        win.view?.scaleY = s
    }

    private fun copyLastToClipboard() {
        val text = prefs?.lastSessionClean.orEmpty()
        if (text.isBlank()) {
            Toast.makeText(this, R.string.flow_bubble_saved_in_app, Toast.LENGTH_SHORT).show()
            return
        }
        copyTextToClipboard(text)
        Toast.makeText(this, R.string.flow_bubble_copied_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun copyTextToClipboard(text: String) {
        if (text.isBlank()) return
        try {
            val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Open Flow", text))
        } catch (_: Exception) {
        }
    }

    private fun postStopActive(): Boolean {
        if (listening || postStopAt <= 0L) return false
        val text = prefs?.lastSessionClean.orEmpty()
        val s = PostStopChips.state(
            elapsedMs = SystemClock.elapsedRealtime() - postStopAt,
            hasSessionText = text.isNotBlank(),
            insertOk = lastInsertOk,
            canUndo = UndoInsert.canUndo(undoSnap),
        )
        return s.any
    }

    private fun armPostStopChips(insertOk: Boolean) {
        lastInsertOk = insertOk
        postStopAt = SystemClock.elapsedRealtime()
        refreshPostStopChips()
    }

    private fun clearPostStopChips() {
        postStopAt = 0L
        win.bubbleChipCopy?.visibility = View.GONE
        win.bubbleChipUndo?.visibility = View.GONE
        win.bubbleChipPaste?.visibility = View.GONE
        painter.paint()
    }

    private fun refreshPostStopChips() {
        if (listening) return
        if (postStopAt > 0L && !PostStopChips.visible(SystemClock.elapsedRealtime() - postStopAt)) {
            clearPostStopChips()
            return
        }
        val text = prefs?.lastSessionClean.orEmpty()
        val s = PostStopChips.state(
            elapsedMs = if (postStopAt <= 0L) Long.MAX_VALUE else SystemClock.elapsedRealtime() - postStopAt,
            hasSessionText = text.isNotBlank(),
            insertOk = lastInsertOk,
            canUndo = UndoInsert.canUndo(undoSnap),
        )
        win.bubbleChipCopy?.visibility = if (s.copy) View.VISIBLE else View.GONE
        win.bubbleChipUndo?.visibility = if (s.undo) View.VISIBLE else View.GONE
        win.bubbleChipPaste?.visibility = if (s.paste) View.VISIBLE else View.GONE
        if (s.any) painter.paint()
    }

    private fun undoLastInsert() {
        val snap = undoSnap ?: return
        if (!UndoInsert.canUndo(snap)) return
        val restored = UndoInsert.restoredField(snap)
        if (writeFieldText(restored)) {
            undoSnap = null
            lastInserted = restored
            clearPostStopChips()
            Toast.makeText(this, R.string.flow_bubble_undone, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pasteClipboardIntoField() {
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
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val ok = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            if (!ok) {
                Toast.makeText(this, R.string.flow_bubble_copied_clipboard, Toast.LENGTH_SHORT).show()
            } else {
                clearPostStopChips()
            }
        } finally {
            @Suppress("DEPRECATION")
            node.recycle()
        }
    }

    fun useRawFromHistory(raw: String) {
        val said = raw.trim()
        if (said.isBlank()) return
        val prefix = captureFieldPrefix()
        val merged = UndoInsert.useRawMerged(prefix, said)
        if (writeFieldText(merged)) {
            prefs?.setLastSession(raw = said, clean = said)
            Toast.makeText(this, R.string.restore_raw, Toast.LENGTH_SHORT).show()
        } else {
            copyTextToClipboard(said)
            Toast.makeText(this, R.string.flow_bubble_copied_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeFieldText(text: String): Boolean {
        val root = rootInActiveWindow
        val node = resolveEditable(root, focusedEditable)
        try {
            root?.let {
                @Suppress("DEPRECATION")
                it.recycle()
            }
        } catch (_: Exception) {
        }
        if (node == null) return false
        return try {
            setNodeText(node, text)
        } finally {
            @Suppress("DEPRECATION")
            node.recycle()
        }
    }

    private fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        var ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!ok) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return ok
    }

    private fun onPulseTick() {
        dropIdleSttIfNeeded()
        val now = SystemClock.elapsedRealtime()
        if (listening) {
            setListeningAwake(true)
            applyRmsPulse()
            val elapsed = now - listenStartedAt
            when (SessionGuard.phase(elapsed)) {
                SessionPhase.STOP -> {
                    if (!stopInProgress) {
                        stopListening(SessionStopPolicy.save(SessionStopPolicy.onLimit(sessionEarId)))
                    }
                    return
                }
                SessionPhase.WARN -> {
                    val sec = elapsed / 1000
                    if (sec != lastWarnSec) {
                        lastWarnSec = sec
                        if (prefs?.bubbleShowText == true) {
                            win.bubbleLabel?.text = "Wrap up ${sec}s"
                        } else {
                            setListenChrome(sec)
                        }
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
        refreshPostStopChips()
    }

    private fun setBubbleEmphasis(hasField: Boolean) {
        if (listening) return
        win.view?.alpha = BubbleVisualDriver.emphasisAlpha(prefs?.bubbleOpacity, hasField)
        val base = effectiveScale()
        win.view?.scaleX = base
        win.view?.scaleY = base
    }

    private fun startListening() {
        if (stopInProgress) return
        if (listening) return
        clearPostStopChips()
        if (prefs?.isSnoozed() == true) {
            prefs?.clearSnooze()
            refreshBubbleVisibility()
        }
        val micOk = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!micOk) {
            win.bubbleLabel?.text = getString(R.string.flow_bubble_need_mic)
            listening = false
            refreshBubbleVisibility()
            return
        }
        listening = true
        compactVisual = false
        latencyTrace = SessionLatency(SystemClock::elapsedRealtime)
            .also { it.mark("listen") }
        hapticEvent(HapticFeel.Event.LISTEN)
        sessionBuffer = StringBuilder()
        lastPartial = ""
        fieldPrefix = captureFieldPrefix()
        listenStartedAt = SystemClock.elapsedRealtime()
        listenStartedWallMs = System.currentTimeMillis()
        lastWarnSec = -1L
        sessionId = retrySessionId ?: UUID.randomUUID().toString()
        lastInteractionAt = listenStartedAt
        setListeningAwake(true)
        val gen = ++listenGeneration

        painter.paint()
        setListenChrome(0)
        setBubbleEmphasis(true)

        val lang = InsertPolish.language(prefs?.languageTag)
        val earPick = SttRouter.pick(
            auto = false,
            manualEarId = app.enginePrefs.earId,
            signals = routeSignals(),
            health = providerHealth,
        )
        sessionEarId = earPick.providerId
        val ear = app.currentEar()
        this.ear = ear
        if (retrySessionId == null && EarMicPolicy.bubbleCapturesWav(sessionEarId)) {
            sessionAudio.start()
        }
        ear.setListener(object : SpeechEngine.Listener {
            /** Accept STT while this generation is active (including flush). */
            private fun live(): Boolean = gen == listenGeneration

            /** Cloud WS / PCM threads must not touch Views or Toast. */
            private fun ui(block: () -> Unit) {
                MainThreadHop.run(
                    isMain = Looper.myLooper() == Looper.getMainLooper(),
                    post = { mainHandler.post(it) },
                    block = block,
                )
            }

            /** Hop to main only if this generation is still live. */
            private fun liveUi(block: () -> Unit) {
                ui { if (live()) block() }
            }

            override fun onPartial(text: String) = liveUi {
                latencyTrace?.mark("first_partial")
                lastPartial = text.trim()
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                if (prefs?.bubbleShowText == true) {
                    val preview = SessionText.commitRaw(sessionBuffer.toString(), lastPartial)
                    win.bubbleLabel?.text = BubbleLabelFormatter.partial(preview, elapsed)
                    ensureLabelVisibleForText()
                } else {
                    setListenChrome(elapsed)
                }
            }

            override fun onFinal(text: String) = liveUi {
                if (text.isBlank()) return@liveUi
                if (sessionBuffer.isNotEmpty()) sessionBuffer.append(' ')
                sessionBuffer.append(text.trim())
                lastPartial = ""
                if (prefs?.bubbleShowText == true) {
                    win.bubbleLabel?.text = BubbleLabelFormatter.finalChunk(sessionBuffer.toString())
                    ensureLabelVisibleForText()
                } else {
                    setListenChrome(
                        (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                    )
                }
            }

            override fun onError(message: String, fatal: Boolean) = ui {
                if (!live() && !fatal) return@ui
                if (fatal) providerHealth.recordFailure(sessionEarId)
                if (BuildConfig.DEBUG) {
                    android.util.Log.w("OpenFlow.Bubble", "ear error fatal=$fatal msg=$message")
                }
                val kind = EarErrorPolicy.classify(message, fatal)
                val mic = kind == EarErrorPolicy.Kind.MIC
                val soft = kind == EarErrorPolicy.Kind.SOFT
                if (soft) {
                    setListenChrome((SystemClock.elapsedRealtime() - listenStartedAt) / 1000)
                    return@ui
                }
                val shown = when {
                    mic -> BubbleLabelFormatter.needMic()
                    fatal -> CloudFallbackNotice.forFatal(sessionEarId)
                        ?: BubbleLabelFormatter.earError(message)
                    else -> BubbleLabelFormatter.earError(message)
                }
                win.bubbleLabel?.text = shown
                if (!mic && !soft) {
                    hapticEvent(HapticFeel.Event.ERROR)
                    Toast.makeText(
                        this@FlowAccessibilityService,
                        shown,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (fatal && !stopInProgress) {
                    stopListening(save = true)
                }
            }

            override fun onNeedMicPermission() = liveUi {
                win.bubbleLabel?.text = BubbleLabelFormatter.needMic()
                if (!stopInProgress) stopListening(save = false)
            }

            override fun onReady() = liveUi {
                latencyTrace?.mark("ear_ready")
                providerHealth.recordSuccess(sessionEarId)
                val elapsed = (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                if (prefs?.bubbleShowText == true) {
                    val current = win.bubbleLabel?.text?.toString().orEmpty()
                    if (current.isBlank() ||
                        current.startsWith("Listening") ||
                        current == getString(R.string.flow_bubble_listening)
                    ) {
                        win.bubbleLabel?.text = BubbleLabelFormatter.listening(elapsed)
                    }
                } else {
                    setListenChrome(elapsed)
                }
            }

            override fun onListeningChanged(isOn: Boolean) = liveUi {
                if (!isOn && listening && !stopInProgress) {
                    // System STT ends a segment this way. Cloud ears end via onError / user stop only.
                    if (ear is CloudEar) return@liveUi
                    stopListening(save = true)
                }
            }

            override fun onRmsChanged(rmsdB: Float) = liveUi {
                lastRms = BubbleRms.capture(rmsdB)
                applyRmsPulse()
                if (prefs?.bubbleShowText != true) {
                    setListenChrome(
                        (SystemClock.elapsedRealtime() - listenStartedAt) / 1000
                    )
                }
            }
        })
        scope.launch(Dispatchers.IO) {
            val dict = runCatching { app.dictations.dictionaryMap() }.getOrDefault(emptyMap())
            val bias = SttBias.strings(dict, SttBias.fieldTokens(fieldPrefix))
            mainHandler.post {
                if (gen != listenGeneration || !listening) return@post
                ear.setBiasing(bias)
                ear.setPickDictionary(bias)
                ear.startContinuous(lang)
                if (BuildConfig.DEBUG) {
                    android.util.Log.i(
                        "OpenFlow.Bubble",
                        "listen start gen=$gen ear=${ear.javaClass.simpleName} bias=${bias.size}"
                    )
                }
            }
        }
    }

    private fun stopListening(save: Boolean) {
        latencyTrace?.mark("stop")
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
        val routedEarId = sessionEarId
        val prefix = fieldPrefix
        val dur = SystemClock.elapsedRealtime() - listenStartedAt
        win.bubbleLabel?.text = "…"

        fun finishCommit() {
            if (gen != listenGeneration) {
                stopInProgress = false
                return
            }
            val snap = ListenSnapshot(
                generation = gen,
                finals = sessionBuffer.toString(),
                lastPartial = lastPartial,
                prefix = prefix,
                earId = routedEarId,
                sessionId = sessionId,
                startedAtElapsed = listenStartedAt,
                startedWallMs = listenStartedWallMs,
                retrySessionId = retrySessionId,
            )
            val raw = snap.raw
            if (BuildConfig.DEBUG) {
                android.util.Log.i(
                    "OpenFlow.Bubble",
                    "stop save=$save rawLen=${raw.length} finalsLen=${sessionBuffer.length} " +
                        "partialLen=${lastPartial.length} gen=$gen"
                )
            }
            listenGeneration++
            listening = false
            setListeningAwake(false)
            lastRms = 0f
            applyRmsPulse()
            sessionBuffer = StringBuilder()
            lastPartial = ""
            fieldPrefix = ""
            ear?.setListener(null)
            stopInProgress = false

            when (StopCommitPolicy.decide(save, raw)) {
                StopCommitPolicy.Action.POLISH_INSERT -> {
                    val lang = InsertPolish.language(prefs?.languageTag)
                    polishSession(raw, prefix, routedEarId) { result ->
                        finishPolishedInsert(
                            result = result,
                            prefix = prefix,
                            dur = dur,
                            lang = lang,
                            logTag = "OpenFlow.Bubble",
                        )
                    }
                }
                StopCommitPolicy.Action.PERSIST_FAIL -> {
                    persistFailedSession(dur)
                    win.bubbleLabel?.text = "Fail · tap to retry"
                }
                StopCommitPolicy.Action.DISCARD -> {
                    sessionAudio.stopAndDiscard()
                    win.bubbleLabel?.text = BubbleLabelFormatter.idle()
                }
            }
            painter.paint()
            setBubbleEmphasis(focusedEditable != null)
        }

        val current = ear
        if (current == null) {
            finishCommit()
        } else {
            val timeout = if (current is OnDeviceEar) {
                OnDeviceEar.FLUSH_TIMEOUT_MS
            } else {
                SttEngine.DEFAULT_FLUSH_TIMEOUT_MS
            }
            try {
                current.stopAndFlush(timeout) {
                    mainHandler.post { finishCommit() }
                }
            } catch (e: Exception) {
                // Engine threw mid-flush: still run the guarded commit so
                // stopInProgress/listening reset and captured text survives.
                if (BuildConfig.DEBUG) {
                    android.util.Log.e("OpenFlow.Bubble", "stopAndFlush failed", e)
                }
                mainHandler.post { finishCommit() }
            }
        }
    }

    /** Circle/square hide label at idle; force visible while showing live text. */
    private fun ensureLabelVisibleForText() {
        if (listening) {
            win.bubbleLabel?.visibility = View.GONE
            return
        }
        if (prefs?.bubbleShowText != true) return
        win.bubbleLabel?.visibility = View.VISIBLE
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
            if (BuildConfig.DEBUG) {
                android.util.Log.w("OpenFlow.Inject", "blank")
            }
            return
        }
        val prefix = captureFieldPrefix()
        val dur = 0L
        polishSession(text, prefix, sessionEarId) { result ->
            finishPolishedInsert(
                result = result,
                prefix = prefix,
                dur = dur,
                lang = InsertPolish.language(prefs?.languageTag),
                logTag = "OpenFlow.Inject",
            )
        }
    }

    private fun registerInjectReceiver() {
        if (!ReceiverExportPolicy.injectAllowed(BuildConfig.DEBUG) || injectReceiverRegistered) return
        val filter = IntentFilter(ACTION_INJECT)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(injectReceiver, filter, ReceiverExportPolicy.injectFlags())
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(injectReceiver, filter)
            }
            injectReceiverRegistered = true
            android.util.Log.i("OpenFlow.Inject", "receiver registered action=$ACTION_INJECT")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("OpenFlow.Inject", "register failed", e)
            }
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
                registerReceiver(copyReceiver, filter, ReceiverExportPolicy.copyFlags())
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

    private fun routeSignals(): RouteSignals {
        val online = runCatching {
            val manager = getSystemService(ConnectivityManager::class.java)
            val capabilities = manager?.getNetworkCapabilities(manager.activeNetwork)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.getOrDefault(false)
        fun keyed(ids: Set<String>): Set<String> = ids.filterTo(mutableSetOf()) { id ->
            runCatching { app.secrets.get(id).orEmpty().isNotBlank() }.getOrDefault(false)
        }
        return RouteSignals(
            online = online,
            keyedEars = keyed(CLOUD_EAR_IDS),
            keyedBrains = keyed(CLOUD_BRAIN_IDS),
            preferOnDevice = prefs?.preferOnDevice == true,
        )
    }

    private fun looksLikeCommand(text: String): Boolean =
        CommandMode.applyLocal(text) != text || VoiceCommands.apply(text) != text

    /**
     * Same path for stopListening + debug inject:
     * dict → snippets → CleanupPipeline(pref level) → optional brain (if picked) → dict again.
     */
    private fun persistFailedSession(dur: Long) {
        val sid = sessionId.ifBlank { UUID.randomUUID().toString() }
        val wall = if (listenStartedWallMs > 0L) listenStartedWallMs else System.currentTimeMillis()
        val lang = InsertPolish.language(prefs?.languageTag)
        val retention = prefs?.retentionPolicy ?: "keep"
        val req = PersistAsk.afterFail(
            sessionId = sid,
            durationMs = dur,
            languageTag = lang,
            retentionPolicy = retention,
            packageName = lastPackage.orEmpty(),
            createdAtEpochMs = wall,
        )
        scope.launch(Dispatchers.IO) {
            runCatching {
                sessionAudio.stopAndWrite(audioFiles.getAudioFile(req.id))
                applyPersist(req)
                retrySessionId = req.id
                DictationNotifier.notifyProcessFailed(this@FlowAccessibilityService)
            }
        }
    }

    private suspend fun applyPersist(req: PersistAsk.Request) {
        when (req.kind) {
            PersistAsk.Kind.SKIP -> return
            PersistAsk.Kind.SAVE_OK -> app.dictations.saveDictation(
                rawText = req.rawText,
                cleanText = req.cleanText,
                durationMs = req.durationMs,
                languageTag = req.languageTag,
                retentionPolicy = req.retentionPolicy,
                packageName = req.packageName,
                createdAtEpochMs = req.createdAtEpochMs,
                processStatus = ProcessStatus.OK,
                id = req.id,
            )
            PersistAsk.Kind.MARK_OK -> app.dictations.markDictationOk(
                id = req.id,
                rawText = req.rawText,
                cleanText = req.cleanText,
            )
            PersistAsk.Kind.SAVE_FAILED -> app.dictations.saveDictation(
                rawText = req.rawText,
                cleanText = req.cleanText,
                durationMs = req.durationMs,
                languageTag = req.languageTag,
                retentionPolicy = req.retentionPolicy,
                packageName = req.packageName,
                createdAtEpochMs = req.createdAtEpochMs,
                processStatus = ProcessStatus.FAILED,
                id = req.id,
            )
        }
    }

    private fun polishSession(
        text: String,
        surroundingField: String,
        routedEarId: String,
        onDone: (CleanupResult) -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            val dict = app.dictations.dictionaryMap()
            val snip = app.dictations.snippetMap()
            lastKeepCap = dict.keys
            val detected = AppContextEngine.detect(lastPackage)
            val messaging = AppContextEngine.casualChat(detected.category)
            val prefLevel = prefs?.cleanupLevel ?: "medium"
            val level = InsertPolish.level(prefLevel)
            val p = prefs
            val style = if (p != null) {
                StyleResolvePolicy.resolve(
                    lastPackage,
                    p.getStyleAppAssignments(),
                    p.hubStylesMap(),
                )
            } else {
                WritingStyle.CASUAL
            }
            val custom = p?.customStyleConfig() ?: CustomStyleConfig()
            val mode = app.enginePrefs.routeMode
            val rewrite = InsertPolish.brainRewriteOnInsert(app.enginePrefs.brainId)
            val surrounding = FieldContext.surrounding(
                FieldContext.on(rewrite),
                surroundingField,
            )
            val brain = if (rewrite) {
                FieldContext.wrapBrain(app.registry.brain(app.enginePrefs.brainId), surrounding)
            } else {
                NoAI
            }
            var polishTimedOut = false
            val polish = async {
                TextPostProcessor.polishRouted(
                    raw = text,
                    style = style,
                    level = level,
                    custom = custom,
                    dictionary = dict,
                    snippets = snip,
                    brain = brain,
                    earId = routedEarId,
                    brainId = app.enginePrefs.brainId,
                    promptHint = detected.category.promptGuideline,
                    messaging = messaging,
                    mode = mode,
                    aiWhen = app.enginePrefs.aiWhen,
                    signals = routeSignals(),
                    looksLikeCommand = looksLikeCommand(text),
                    onBrainOutcome = { id, ok ->
                        if (ok) providerHealth.recordSuccess(id) else providerHealth.recordFailure(id)
                    },
                )
            }
            val result = withTimeoutOrNull(CleanupBudget.POLISH_MS) { polish.await() } ?: run {
                polishTimedOut = true
                polish.cancel()
                if (BuildConfig.DEBUG) {
                    android.util.Log.w(
                        "OpenFlow.Cleanup",
                        "polish timed out after ${CleanupBudget.POLISH_MS}ms — honest raw fallback",
                    )
                }
                CleanupBudget.fallback(text)
            }
            if (result == null) return@launch
            val brainId = app.enginePrefs.brainId
            android.util.Log.i(
                "OpenFlow.Cleanup",
                "level=$level pref=$prefLevel style=$style lang=${InsertPolish.language(prefs?.languageTag)} " +
                    "brain=$brainId mode=$mode field=${surroundingField.isNotEmpty()} " +
                    "rawLen=${text.length} cleanLen=${result.clean.length} " +
                    "corr=${result.corrections.size} " +
                    "changed=${text.trim() != result.clean.trim()}"
            )
            mainHandler.post {
                if (polishTimedOut) {
                    Toast.makeText(
                        this@FlowAccessibilityService,
                        R.string.flow_cleanup_fallback,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                onDone(result)
            }
        }
    }

    private fun finishPolishedInsert(
        result: CleanupResult,
        prefix: String,
        dur: Long,
        lang: String,
        logTag: String,
    ) {
        val pe = PressEnterPolicy.apply(result.clean)
        val finalText = pe.text
        android.util.Log.i(
            "OpenFlow.Bubble",
            "commit cleanLen=${finalText.length} prefixLen=${prefix.length} " +
                "submit=${pe.submit} level=${result.level}"
        )
        if (finalText.isNotBlank()) {
            val ok = commitSessionToField(finalText, prefix, result.raw)
            if (ok && pe.submit) submitAfterInsert()
            latencyTrace?.mark("inserted")
            latencyTrace?.let { t ->
                android.util.Log.i(
                    "OpenFlow.Latency",
                    "ok=$ok words=${finalText.split(WORD_SPLIT).count { it.isNotBlank() }} ${t.summary()}"
                )
            }
            latencyTrace = null
            prefs?.setLastSession(raw = result.raw, clean = finalText)
            lastInteractionAt = SystemClock.elapsedRealtime()
            armPostStopChips(ok)
            val wordCount = finalText.split(WORD_SPLIT)
                .filter { it.isNotBlank() }.size
            val retention = prefs?.retentionPolicy ?: "keep"
            val req = PersistAsk.afterPolish(
                sessionId = sessionId,
                wasRetry = retrySessionId != null,
                raw = result.raw,
                clean = finalText,
                durationMs = dur,
                languageTag = lang,
                retentionPolicy = retention,
                packageName = lastPackage.orEmpty(),
                createdAtEpochMs = listenStartedWallMs,
            )
            scope.launch(Dispatchers.IO) {
                runCatching {
                    if (req.kind != PersistAsk.Kind.MARK_OK && req.kind != PersistAsk.Kind.SKIP) {
                        sessionAudio.stopAndWrite(audioFiles.getAudioFile(req.id))
                    }
                    applyPersist(req)
                    retrySessionId = null
                }
            }
            DictationNotifier.notifyIfPermitted(this@FlowAccessibilityService, wordCount)
        } else if (pe.submit) {
            submitAfterInsert()
        }
    }

    private fun submitAfterInsert() {
        val how = InsertSubmitPolicy.how(submit = true, api = Build.VERSION.SDK_INT)
        if (how == InsertSubmitPolicy.How.NONE) return
        val root = rootInActiveWindow
        val node = resolveEditable(root, focusedEditable)
        try {
            try {
                root?.let {
                    @Suppress("DEPRECATION")
                    it.recycle()
                }
            } catch (_: Exception) {
            }
            if (node == null) return
            val action = InsertSubmitPolicy.actionId(how) ?: return
            node.performAction(action)
        } finally {
            try {
                @Suppress("DEPRECATION")
                node?.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun commitSessionToField(finalText: String, prefix: String, raw: String = ""): Boolean {
        if (finalText.isBlank()) return false
        val merged = FieldContext.afterPolish(prefix, finalText, lastKeepCap)
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
            return false
        }
        try {
            if (!FieldFocusResolver.isUsableEditable(node)) {
                Toast.makeText(this, R.string.flow_bubble_saved_in_app, Toast.LENGTH_SHORT).show()
                return false
            }
            val ok = setNodeText(node, merged)
            android.util.Log.i(
                "OpenFlow.Bubble",
                "setText ok=$ok mergedLen=${merged.length} class=${node.className}"
            )
            if (ok) {
                lastInserted = merged
                lastInsertAt = SystemClock.elapsedRealtime()
                lastInsertPkg = node.packageName?.toString() ?: lastPackage
                undoSnap = UndoInsert.afterInsert(
                    previousField = prefix,
                    merged = merged,
                    raw = raw,
                    clean = finalText,
                )
            }
            if (!ok) {
                copyTextToClipboard(merged)
                Toast.makeText(this, R.string.flow_bubble_copied_clipboard, Toast.LENGTH_SHORT).show()
            }
            focusedEditable?.let {
                @Suppress("DEPRECATION")
                it.recycle()
            }
            @Suppress("DEPRECATION")
            focusedEditable = AccessibilityNodeInfo.obtain(node)
            return ok
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
                if (FieldFocusResolver.isUsableEditable(focused)) return focused
                val nested = FieldFocusResolver.findEditableInSubtree(focused)
                @Suppress("DEPRECATION")
                focused.recycle()
                if (nested != null) return nested
            }
        }
        if (cached != null) {
            // Cached node may have been recycled/detached since store
            // (minSdk < 33: recycle() is real). Treat as miss, not crash.
            return try {
                if (FieldFocusResolver.isUsableEditable(cached)) {
                    @Suppress("DEPRECATION")
                    AccessibilityNodeInfo.obtain(cached)
                } else {
                    null
                }
            } catch (_: IllegalStateException) {
                null
            }
        }
        return null
    }

    private fun applyRmsPulse() {
        val view = win.view ?: return
        if (!listening) {
            applyVisualScale()
            return
        }
        val s = BubbleListenMotion.overlayScale(base = effectiveScale(), rms = lastRms)
        view.scaleX = s
        view.scaleY = s
        painter.applyWaveHeights(WaveformBars.filledCount(lastRms))
    }

    private fun hapticEvent(event: HapticFeel.Event) {
        val pick = prefs?.hapticPick(event) ?: HapticPick.CLICK
        val constant = HapticPick.constant(pick) ?: return
        win.view?.performHapticFeedback(constant)
    }

    private fun hapticSaveOrDiscard(save: Boolean) {
        hapticEvent(if (save) HapticFeel.Event.SAVE else HapticFeel.Event.CANCEL)
    }

    private fun brainCanCommand(): Boolean =
        CommandChrome.visible(BrainPick.command(app.enginePrefs.brainId))

    /** Keep the screen on while the bubble is listening. */
    private fun setListeningAwake(on: Boolean) {
        win.view?.keepScreenOn = on
        if (listenAwake == on) return
        listenAwake = on
        val params = win.params ?: return
        val view = win.view ?: return
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        params.flags = if (on) params.flags or flag else params.flags and flag.inv()
        try {
            win.updateLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun setListenChrome(@Suppress("UNUSED_PARAMETER") elapsedSec: Long) {
        win.bubbleLabel?.visibility = View.GONE
    }

    fun applyPrefsVisual() {
        val p = prefs ?: return
        val s = effectiveScale()
        win.view?.scaleX = s
        win.view?.scaleY = s
        win.params?.alpha = p.bubbleOpacity
        win.refreshImeHeight()
        win.applyParkedOverlayY()
        painter.paint()
        refreshBubbleVisibility()
    }

    companion object {
        private val WORD_SPLIT = Regex("\\s+")
        private val CLOUD_EAR_IDS = setOf("openai", "deepgram", "assemblyai", "sarvam")
        private val CLOUD_BRAIN_IDS = setOf("openai", "anthropic", "grok", "gemini")

        /** Debug broadcast action (any build id; handler no-ops if not DEBUG). */
        const val ACTION_INJECT = "app.openflow.INJECT_DICTATION"
        const val ACTION_COPY_LAST = "app.openflow.COPY_LAST"
        const val EXTRA_TEXT = "text"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        var instance: FlowAccessibilityService? = null
            private set

        fun isRunning(): Boolean = instance != null

        /**
         * True when Settings lists our service — even if [instance] is briefly null
         * after install / process restart (race before onServiceConnected).
         */
        fun isEnabled(context: android.content.Context): Boolean {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
            val want = android.content.ComponentName(
                context,
                FlowAccessibilityService::class.java,
            ).flattenToString()
            return enabledInSecureList(flat, want)
        }

        /** Pure parse of ENABLED_ACCESSIBILITY_SERVICES colon list. */
        fun enabledInSecureList(flat: String?, componentFlattened: String): Boolean {
            if (flat.isNullOrBlank() || componentFlattened.isBlank()) return false
            return flat.split(':').any { it.equals(componentFlattened, ignoreCase = true) }
        }
    }
}
