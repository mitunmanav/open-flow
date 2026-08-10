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
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * SpeechRecognizer wrapper with continuous-dictation restart loop.
 * Prefer on-device. Unlimited speak = auto-restart while continuous.
 */
class SttEngine(
    private val context: Context,
    private val preferOnDevice: Boolean = true,
    private val policy: ContinuousPolicy = ContinuousPolicy(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val softMuteBeeps: Boolean = true
) {
    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String, fatal: Boolean)
        fun onReady()
        fun onListeningChanged(listening: Boolean)
        fun onNeedMicPermission() {}
    }

    private var recognizer: SpeechRecognizer? = null
    private var listener: Listener? = null
    private val continuous = AtomicBoolean(false)
    private val starting = AtomicBoolean(false)
    private val sessionCount = AtomicInteger(0)
    private var languageTag: String = Locale.getDefault().toLanguageTag()
    private var restartPosted = false
    private var savedMusicVolume: Int? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun setListener(l: Listener?) {
        listener = l
    }

    fun startContinuous(languageTag: String = Locale.getDefault().toLanguageTag()) {
        this.languageTag = languageTag
        if (!hasMicPermission()) {
            listener?.onNeedMicPermission()
            listener?.onError("Microphone permission required", fatal = true)
            listener?.onListeningChanged(false)
            return
        }
        if (!isAvailable) {
            listener?.onError("Speech recognition not available", fatal = true)
            listener?.onListeningChanged(false)
            return
        }
        continuous.set(true)
        listener?.onListeningChanged(true)
        mainHandler.post { beginSession(forceRecreate = true) }
    }

    fun startOnce(languageTag: String = Locale.getDefault().toLanguageTag()) {
        this.languageTag = languageTag
        if (!hasMicPermission()) {
            listener?.onNeedMicPermission()
            listener?.onError("Microphone permission required", fatal = true)
            return
        }
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
            restoreVolume()
            starting.set(false)
            listener?.onListeningChanged(false)
        }
    }

    fun destroy() {
        continuous.set(false)
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            destroyInternal()
            restoreVolume()
            starting.set(false)
            listener = null
        }
    }

    private fun beginSession(forceRecreate: Boolean) {
        if (!continuous.get() && !forceRecreate && sessionCount.get() > 0) {
            return
        }
        if (!hasMicPermission()) {
            listener?.onNeedMicPermission()
            continuous.set(false)
            listener?.onListeningChanged(false)
            return
        }
        if (!starting.compareAndSet(false, true)) {
            // serialize starts
            scheduleRestart(ContinuousPolicy.ERROR_RECOGNIZER_BUSY)
            return
        }
        try {
            val n = sessionCount.incrementAndGet()
            val needNew = forceRecreate ||
                recognizer == null ||
                policy.shouldRecreateRecognizer(n)
            if (needNew) {
                destroyInternal()
                recognizer = createRecognizer()
            } else {
                try {
                    recognizer?.cancel()
                } catch (_: Exception) {
                }
            }
            val r = recognizer
            if (r == null) {
                listener?.onError("Speech recognition not available", fatal = true)
                continuous.set(false)
                listener?.onListeningChanged(false)
                return
            }
            r.setRecognitionListener(buildListener())
            softMute()
            try {
                r.startListening(buildIntent(languageTag))
            } catch (e: Exception) {
                listener?.onError(e.message ?: "start failed", fatal = false)
                scheduleRestart(ContinuousPolicy.ERROR_CLIENT)
            }
        } finally {
            // unlock after short delay so engine can claim mic
            mainHandler.postDelayed({ starting.set(false) }, 80)
        }
    }

    private fun buildListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            restoreVolume()
            listener?.onReady()
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            restoreVolume()
            starting.set(false)
            val fatal = error == ContinuousPolicy.ERROR_INSUFFICIENT_PERMISSIONS
            val msg = when (error) {
                ContinuousPolicy.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow microphone"
                ContinuousPolicy.ERROR_SPEECH_TIMEOUT -> "Silence — continuing"
                ContinuousPolicy.ERROR_NO_MATCH -> "No match — continuing"
                ContinuousPolicy.ERROR_RECOGNIZER_BUSY -> "Busy — retry"
                else -> "STT $error"
            }
            listener?.onError(msg, fatal = fatal)
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
            restoreVolume()
            starting.set(false)
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
            if (continuous.get()) {
                // busy → force new recognizer
                val force = errorCode == ContinuousPolicy.ERROR_RECOGNIZER_BUSY
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
        } catch (_: Exception) {
        }
    }

    private fun restoreVolume() {
        val saved = savedMusicVolume ?: return
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_MUSIC, saved, 0)
        } catch (_: Exception) {
        }
        savedMusicVolume = null
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
        val lang = languageTag.ifBlank { SttTuning.DEFAULT_LANGUAGE }
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            // Bias engine toward this locale when supported
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, SttTuning.MAX_RESULTS)
            if (preferOnDevice) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            // Snappy endpointer (was 2s / 2.8s / 2.2s — felt laggy)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                SttTuning.MIN_SPEECH_MS
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                SttTuning.COMPLETE_SILENCE_MS
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                SttTuning.POSSIBLY_COMPLETE_SILENCE_MS
            )
            // API 33+: auto punct + latency-first formatting when engine supports it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(
                    RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                    RecognizerIntent.FORMATTING_OPTIMIZE_LATENCY
                )
                putExtra(RecognizerIntent.EXTRA_HIDE_PARTIAL_TRAILING_PUNCTUATION, true)
            }
        }
    }
}

