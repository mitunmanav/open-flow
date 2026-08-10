package app.openflow.stt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * SpeechRecognizer wrapper with continuous-dictation restart loop.
 * Prefer on-device. Unlimited speak time = auto-restart while [continuous] active.
 */
class SttEngine(
    private val context: Context,
    private val preferOnDevice: Boolean = true,
    private val policy: ContinuousPolicy = ContinuousPolicy(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String, fatal: Boolean)
        fun onReady()
        fun onListeningChanged(listening: Boolean)
        fun onSessionTick(sessionIndex: Int)
    }

    private var recognizer: SpeechRecognizer? = null
    private var listener: Listener? = null
    private val continuous = AtomicBoolean(false)
    private val sessionCount = AtomicInteger(0)
    private var languageTag: String = Locale.getDefault().toLanguageTag()
    private var restartPosted = false

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun setListener(l: Listener?) {
        listener = l
    }

    /** Start continuous dictation (auto-restart until [stop]). */
    fun startContinuous(languageTag: String = Locale.getDefault().toLanguageTag()) {
        this.languageTag = languageTag
        continuous.set(true)
        listener?.onListeningChanged(true)
        mainHandler.post { beginSession(forceRecreate = true) }
    }

    /** One-shot listen (no auto restart). */
    fun startOnce(languageTag: String = Locale.getDefault().toLanguageTag()) {
        this.languageTag = languageTag
        continuous.set(false)
        listener?.onListeningChanged(true)
        mainHandler.post { beginSession(forceRecreate = true) }
    }

    fun stop() {
        continuous.set(false)
        restartPosted = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (_: Exception) {
            }
            destroyInternal()
            listener?.onListeningChanged(false)
        }
    }

    fun destroy() {
        continuous.set(false)
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            destroyInternal()
            listener = null
        }
    }

    private fun beginSession(forceRecreate: Boolean) {
        if (!continuous.get() && sessionCount.get() > 0 && !forceRecreate) {
            // one-shot already ran
        }
        val n = sessionCount.incrementAndGet()
        listener?.onSessionTick(n)
        val needNew = forceRecreate ||
            recognizer == null ||
            policy.shouldRecreateRecognizer(n)
        if (needNew) {
            destroyInternal()
            recognizer = createRecognizer()
        }
        val r = recognizer
        if (r == null) {
            listener?.onError("Speech recognition not available", fatal = true)
            continuous.set(false)
            listener?.onListeningChanged(false)
            return
        }
        r.setRecognitionListener(buildListener())
        try {
            r.startListening(buildIntent(languageTag))
        } catch (e: Exception) {
            listener?.onError(e.message ?: "start failed", fatal = false)
            scheduleRestart(ContinuousPolicy.ERROR_CLIENT)
        }
    }

    private fun buildListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listener?.onReady()
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val fatal = error == ContinuousPolicy.ERROR_INSUFFICIENT_PERMISSIONS
            listener?.onError("STT $error", fatal = fatal)
            if (fatal) {
                continuous.set(false)
                listener?.onListeningChanged(false)
                return
            }
            if (policy.shouldRestart(continuous.get(), error, hadResult = false)) {
                scheduleRestart(error)
            } else if (!continuous.get()) {
                listener?.onListeningChanged(false)
            }
        }

        override fun onResults(results: Bundle?) {
            val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = texts?.firstOrNull().orEmpty()
            if (best.isNotBlank()) listener?.onFinal(best)
            if (policy.shouldRestart(continuous.get(), null, hadResult = true)) {
                scheduleRestart(null)
            } else {
                listener?.onListeningChanged(false)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val texts = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = texts?.firstOrNull().orEmpty()
            if (best.isNotBlank()) listener?.onPartial(best)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun scheduleRestart(errorCode: Int?) {
        if (!continuous.get() || restartPosted) return
        restartPosted = true
        val delay = policy.restartDelayMs(errorCode)
        mainHandler.postDelayed({
            restartPosted = false
            if (continuous.get()) beginSession(forceRecreate = false)
        }, delay)
    }

    private fun destroyInternal() {
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    private fun createRecognizer(): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null
        return if (preferOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    private fun buildIntent(languageTag: String): Intent {
        val cfg = SttConfig(preferOnDevice = preferOnDevice)
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, cfg.partialResults)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, cfg.maxResults)
            if (cfg.preferOnDevice) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            // Stretch silence windows where the engine honors them (varies by OEM)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2_000L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                2_500L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2_000L
            )
        }
    }
}
