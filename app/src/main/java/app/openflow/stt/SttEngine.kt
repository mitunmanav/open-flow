package app.openflow.stt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionPart
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import app.openflow.prefs.FlowPrefs
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * SpeechRecognizer wrapper with continuous restart.
 *
 * Reliability (Android 11+ / docs):
 * - Manifest must query RecognitionService (package visibility).
 * - Prefer on-device when available; never hard-fail offline-only when pack missing.
 * - On offline/language/client errors → one network-capable retry.
 * - Must run start/stop on main thread.
 * - After stopListening wait for onResults/onError before startListening again.
 * - Reuse recognizer; destroy only when done or BUSY / SERVER_DISCONNECTED.
 * - Failures set [lastError]; never swallow and pretend OK.
 */
class SttEngine(
    private val context: Context,
    private val preferOnDevice: Boolean = true,
    private val policy: ContinuousPolicy = ContinuousPolicy(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val softMuteBeeps: Boolean = false,
    private var tuning: SttTuning = SttTuning(),
) {
    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String, fatal: Boolean)
        fun onReady()
        fun onListeningChanged(listening: Boolean)
        fun onNeedMicPermission() {}
        fun onRmsChanged(rmsdB: Float) {}
    }

    private var recognizer: SpeechRecognizer? = null
    private var listener: Listener? = null
    private val continuous = AtomicBoolean(false)
    /** True from startListening until onResults/onError (Android wait contract). */
    private val inFlight = AtomicBoolean(false)
    private val restartQueued = AtomicBoolean(false)
    private val sessionCount = AtomicInteger(0)
    private var languageTag: String = LanguagePolicy.LOCKED
    private var restartPosted = false
    private var restartCount = 0
    private val maxRestartsPerSession = 200
    private var savedMusicVolume: Int? = null

    /** Last swallow / engine fail. Never empty-pretend OK. */
    @Volatile
    var lastError: String? = null
        private set

    /** When true, force EXTRA_PREFER_OFFLINE. Flips false after offline-related errors. */
    private var forceOfflineOnly: Boolean = preferOnDevice
    private var usedOnDeviceFactory: Boolean = false
    private var offlineFallbackUsed: Boolean = false

    /** Non-null while [stopAndFlush] waits for onResults/onError. */
    private var flushCallback: (() -> Unit)? = null
    private val flushDone = AtomicBoolean(false)
    private val flushTimeout = Runnable { completeFlush() }

    /** API 33+ SpeechRecognizer bias strings (dict + focused field). */
    private var biasing: List<String> = emptyList()

    fun setBiasing(words: List<String>) {
        biasing = words
    }

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun setListener(l: Listener?) {
        listener = l
    }

    /** Push Fast/Balanced/Accurate without toggling a11y. Next start uses this. */
    fun applyTuning(next: SttTuning) {
        tuning = next
    }

    fun startContinuous(languageTag: String = LanguagePolicy.LOCKED) {
        this.languageTag = LanguagePolicy.force(languageTag)
        if (!hasMicPermission()) {
            listener?.onNeedMicPermission()
            rememberError("Microphone permission required", notify = true, fatal = true)
            listener?.onListeningChanged(false)
            return
        }
        if (!isAvailable) {
            rememberError(
                "No speech service — install Google app / offline pack",
                notify = true,
                fatal = true
            )
            listener?.onListeningChanged(false)
            return
        }
        continuous.set(true)
        restartCount = 0
        listener?.onListeningChanged(true)
        mainHandler.post { beginSession(forceRecreate = true) }
    }

    fun startOnce(languageTag: String = LanguagePolicy.LOCKED) {
        this.languageTag = LanguagePolicy.force(languageTag)
        if (!hasMicPermission()) {
            listener?.onNeedMicPermission()
            rememberError("Microphone permission required", notify = true, fatal = true)
            return
        }
        if (!isAvailable) {
            rememberError("No speech service on this device", notify = true, fatal = true)
            return
        }
        continuous.set(false)
        listener?.onListeningChanged(true)
        mainHandler.post { beginSession(forceRecreate = true) }
    }

    /**
     * Immediate stop: cancels engine, drops listener, no wait for final.
     * Prefer [stopAndFlush] when the last utterance must reach [Listener.onFinal].
     */
    fun stop() {
        continuous.set(false)
        restartCount = 0
        restartQueued.set(false)
        inFlight.set(false)
        clearScheduledWork()
        abandonFlush(invokeCallback = true)
        mainHandler.post {
            try {
                recognizer?.cancel()
            } catch (e: Exception) {
                rememberError(
                    "cancel failed: ${e.message ?: e.javaClass.simpleName}",
                    notify = true
                )
            }
            destroyInternal()
            restoreVolume()
            val l = listener
            listener = null
            l?.onListeningChanged(false)
        }
    }

    /**
     * Stop continuous listen and **wait** for onResults/onError (Android contract)
     * so the last final can still fire. Times out after [timeoutMs], then destroys.
     *
     * Listener is **kept** until [onFlushed] runs — caller should null it after commit.
     * Does **not** call [Listener.onListeningChanged](false) (caller owns session end UI).
     */
    fun stopAndFlush(timeoutMs: Long = DEFAULT_FLUSH_TIMEOUT_MS, onFlushed: () -> Unit) {
        continuous.set(false)
        restartCount = 0
        clearScheduledWork()
        // Replace any prior flush waiter (should not stack).
        abandonFlush(invokeCallback = true)
        flushDone.set(false)
        flushCallback = onFlushed
        mainHandler.post {
            if (recognizer == null) {
                completeFlush()
                return@post
            }
            try {
                recognizer?.stopListening()
            } catch (e: Exception) {
                rememberError(
                    "stopListening failed: ${e.message ?: e.javaClass.simpleName}",
                    notify = true
                )
                completeFlush()
                return@post
            }
            mainHandler.postDelayed(flushTimeout, timeoutMs.coerceIn(100L, 2_000L))
        }
    }

    fun destroy() {
        continuous.set(false)
        restartQueued.set(false)
        inFlight.set(false)
        clearScheduledWork()
        abandonFlush(invokeCallback = true)
        mainHandler.post {
            destroyInternal()
            restoreVolume()
            listener = null
        }
    }

    private fun clearScheduledWork() {
        restartPosted = false
        mainHandler.removeCallbacks(flushTimeout)
        mainHandler.removeCallbacksAndMessages(null)
    }

    /** Drop in-flight flush; optionally notify caller so UI can commit what it has. */
    private fun abandonFlush(invokeCallback: Boolean) {
        val pending = flushCallback
        flushCallback = null
        if (invokeCallback && pending != null && flushDone.compareAndSet(false, true)) {
            mainHandler.post { pending.invoke() }
            return
        }
        flushDone.set(true)
    }

    private fun completeFlush() {
        if (!flushDone.compareAndSet(false, true)) return
        mainHandler.removeCallbacks(flushTimeout)
        mainHandler.post {
            try {
                recognizer?.cancel()
            } catch (e: Exception) {
                rememberError("flush cancel failed: ${e.message ?: e.javaClass.simpleName}")
            }
            destroyInternal()
            restoreVolume()
            inFlight.set(false)
            restartQueued.set(false)
            val cb = flushCallback
            flushCallback = null
            cb?.invoke()
        }
    }

    /** Call from recognition end when continuous is off (user stop / flush). */
    private fun signalFlushIfNeeded() {
        if (flushCallback != null) {
            completeFlush()
        }
    }

    private fun beginSession(forceRecreate: Boolean) {
        if (!continuous.get() && !forceRecreate && sessionCount.get() > 0) {
            return
        }
        if (!hasMicPermission()) {
            listener?.onNeedMicPermission()
            rememberError("Microphone permission required", notify = true, fatal = true)
            continuous.set(false)
            listener?.onListeningChanged(false)
            return
        }
        // After stopListening, wait for onResults/onError before startListening again.
        if (inFlight.get()) {
            if (!forceRecreate) {
                restartQueued.set(true)
                return
            }
            destroyInternal()
            inFlight.set(false)
        }
        val n = sessionCount.incrementAndGet()
        val needNew = forceRecreate ||
            recognizer == null ||
            policy.shouldRecreateRecognizer(n)
        if (needNew) {
            destroyInternal()
            recognizer = createRecognizer()
        }
        val r = recognizer
        if (r == null) {
            rememberError("Speech recognition not available", notify = true, fatal = true)
            continuous.set(false)
            listener?.onListeningChanged(false)
            return
        }
        r.setRecognitionListener(buildListener())
        softMute()
        inFlight.set(true)
        try {
            r.startListening(buildIntent(languageTag, refreshTuning()))
        } catch (e: Exception) {
            inFlight.set(false)
            rememberError(e.message ?: "start failed", notify = true, fatal = false)
            forceOfflineOnly = false
            usedOnDeviceFactory = false
            scheduleRestart(ContinuousPolicy.ERROR_CLIENT)
        }
    }

    private fun buildListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            restoreVolume()
            listener?.onReady()
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            listener?.onRmsChanged(rmsdB)
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            restoreVolume()
        }

        override fun onError(error: Int) {
            restoreVolume()
            inFlight.set(false)

            // Offline / language / client → one soft fallback to non-offline default engine
            val offlineRelated = error == SpeechRecognizer.ERROR_CLIENT ||
                error == SpeechRecognizer.ERROR_SERVER ||
                error == SpeechRecognizer.ERROR_NETWORK ||
                error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                        error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))

            if (offlineRelated && !offlineFallbackUsed && continuous.get()) {
                offlineFallbackUsed = true
                forceOfflineOnly = false
                usedOnDeviceFactory = false
                rememberError("Retrying speech engine…", notify = true, fatal = false)
                mainHandler.postDelayed({
                    if (continuous.get()) beginSession(forceRecreate = true)
                }, 350)
                return
            }

            val fatal = error == ContinuousPolicy.ERROR_INSUFFICIENT_PERMISSIONS ||
                error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
            val msg = humanError(error)
            rememberError(msg, notify = true, fatal = fatal)
            if (fatal) {
                continuous.set(false)
                restartQueued.set(false)
                listener?.onListeningChanged(false)
                signalFlushIfNeeded()
                return
            }
            val queued = restartQueued.getAndSet(false)
            if (policy.shouldRestart(continuous.get(), error, hadResult = false) || queued) {
                scheduleRestart(error)
            } else if (!continuous.get()) {
                listener?.onListeningChanged(false)
                signalFlushIfNeeded()
            }
        }

        override fun onResults(results: Bundle?) {
            restoreVolume()
            inFlight.set(false)
            restartCount = 0
            val best = extractBestText(results)
            if (best.isNotBlank()) {
                listener?.onFinal(best)
            } else {
                rememberError("No recognition result", notify = true, fatal = false)
            }
            val queued = restartQueued.getAndSet(false)
            if (policy.shouldRestart(continuous.get(), null, hadResult = true) || queued) {
                scheduleRestart(null)
            } else {
                listener?.onListeningChanged(false)
                signalFlushIfNeeded()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            restartCount = 0
            val best = extractBestText(partialResults)
            if (best.isNotBlank()) listener?.onPartial(best)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun humanError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Silence — listen again"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match — try again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy — retry"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Network speech failed (system STT)"
        SpeechRecognizer.ERROR_SERVER -> "Speech service error"
        SpeechRecognizer.ERROR_CLIENT -> "Speech client error"
        SpeechRecognizer.ERROR_AUDIO -> "Mic audio error"
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (error) {
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                        "Language pack missing — install offline speech"
                    SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                        "Speech service disconnected — retry"
                    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many speech requests"
                    else -> "STT error $error"
                }
            } else {
                "STT error $error"
            }
        }
    }

    private fun scheduleRestart(errorCode: Int?) {
        if (!continuous.get() || restartPosted) return
        restartPosted = true
        restartCount++
        if (restartCount > maxRestartsPerSession) {
            restartPosted = false
            continuous.set(false)
            rememberError("Speech engine unstable — please try again", notify = true, fatal = true)
            listener?.onListeningChanged(false)
            signalFlushIfNeeded()
            return
        }
        val delay = policy.restartDelayMs(errorCode)
        mainHandler.postDelayed({
            restartPosted = false
            if (continuous.get()) {
                val force = policy.shouldRecreateOnError(errorCode) ||
                    errorCode == ContinuousPolicy.ERROR_CLIENT
                beginSession(forceRecreate = force)
            }
        }, delay)
    }

    private fun softMute() {
        if (!softMuteBeeps) return
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (savedMusicVolume == null) {
                savedMusicVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
            am.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (e: Exception) {
            rememberError("mute failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun restoreVolume() {
        val saved = savedMusicVolume ?: return
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC, saved, 0)
        } catch (e: Exception) {
            rememberError("restore volume failed: ${e.message ?: e.javaClass.simpleName}")
        }
        savedMusicVolume = null
    }

    private fun destroyInternal() {
        try {
            recognizer?.cancel()
        } catch (e: Exception) {
            rememberError("destroy cancel failed: ${e.message ?: e.javaClass.simpleName}")
        }
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            rememberError("destroy failed: ${e.message ?: e.javaClass.simpleName}")
        }
        recognizer = null
    }

    private fun createRecognizer(): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null
        // Prefer on-device factory when available (API 31+) and still trying offline path
        if (preferOnDevice && forceOfflineOnly && !offlineFallbackUsed &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            try {
                if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    usedOnDeviceFactory = true
                    return SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                }
            } catch (e: Exception) {
                usedOnDeviceFactory = false
                rememberError(
                    "on-device factory failed: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
        usedOnDeviceFactory = false
        return try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            rememberError(
                "create SpeechRecognizer failed: ${e.message ?: e.javaClass.simpleName}"
            )
            null
        }
    }

    /**
     * Live Fast/Balanced/Accurate from prefs on every listen.
     * Constructor / [applyTuning] is fallback if prefs read fails.
     * No a11y toggle needed — extras rebuild on this start.
     */
    private fun refreshTuning(): SttTuning {
        val live = runCatching { FlowPrefs(context).sttTuning() }.getOrElse { e ->
            rememberError("stt tuning prefs: ${e.message ?: e.javaClass.simpleName}")
            null
        }
        if (live != null) tuning = live
        return tuning
    }

    private fun buildIntent(languageTag: String, t: SttTuning = tuning): Intent {
        val lang = LanguagePolicy.force(languageTag)
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, t.maxResults)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

            // Only prefer offline when we still believe packs exist.
            // Hard offline-only breaks many devices without downloaded language packs.
            if (forceOfflineOnly && usedOnDeviceFactory) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                t.minSpeechMs
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                t.completeSilenceMs
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                t.possiblyCompleteSilenceMs
            )

            // API 33+: auto punct / capitalization.
            // Quality = better punct, more latency; latency = snappier, weaker punct.
            // Default quality (see SttTuning.preferFormattingQuality).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val mode = if (t.preferFormattingQuality) {
                    RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY
                } else {
                    RecognizerIntent.FORMATTING_OPTIMIZE_LATENCY
                }
                putExtra(RecognizerIntent.EXTRA_ENABLE_FORMATTING, mode)
                putExtra(RecognizerIntent.EXTRA_HIDE_PARTIAL_TRAILING_PUNCTUATION, true)
                putStringArrayListExtra(
                    RecognizerIntent.EXTRA_BIASING_STRINGS,
                    ArrayList(biasing)
                )
                putExtra(RecognizerIntent.EXTRA_ENABLE_BIASING_DEVICE_CONTEXT, true)
                putExtra(RecognizerIntent.EXTRA_MASK_OFFENSIVE_WORDS, false)
            }
        }
    }

    /**
     * Best engine transcript only — never invent text.
     * Prefer RESULTS_RECOGNITION + CONFIDENCE_SCORES; API 34+ RECOGNITION_PARTS fallback.
     */
    private fun extractBestText(bundle: Bundle?): String {
        val hyps = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val scores = bundle?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        val picked = HypothesisPick.best(hyps, scores)
        if (picked.isNotEmpty()) return picked

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || bundle == null) {
            return ""
        }
        val parts = try {
            bundle.getParcelableArrayList(
                SpeechRecognizer.RECOGNITION_PARTS,
                RecognitionPart::class.java
            )
        } catch (e: Exception) {
            rememberError("recognition parts: ${e.message ?: e.javaClass.simpleName}")
            null
        }
        if (parts.isNullOrEmpty()) return ""

        val texts = parts.map { part ->
            val formatted = part.formattedText
            if (!formatted.isNullOrBlank()) formatted.trim() else part.rawText.trim()
        }
        return HypothesisPick.joinParts(texts)
    }

    private fun rememberError(
        message: String,
        notify: Boolean = false,
        fatal: Boolean = false,
    ) {
        lastError = message
        if (notify) listener?.onError(message, fatal)
    }

    companion object {
        const val DEFAULT_FLUSH_TIMEOUT_MS = 550L
    }
}
