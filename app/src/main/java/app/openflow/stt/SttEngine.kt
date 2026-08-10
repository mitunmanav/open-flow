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

/**
 * Thin wrapper around Android SpeechRecognizer.
 * Prefers on-device when available (API 33+ createOnDeviceSpeechRecognizer).
 */
class SttEngine(
    private val context: Context,
    private val preferOnDevice: Boolean = true,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
        fun onReady()
        fun onEnd()
    }

    private var recognizer: SpeechRecognizer? = null
    private var listener: Listener? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun setListener(l: Listener?) {
        listener = l
    }

    fun start(languageTag: String = Locale.getDefault().toLanguageTag()) {
        mainHandler.post {
            destroyInternal()
            val r = createRecognizer()
            if (r == null) {
                listener?.onError("Speech recognition not available on this device")
                return@post
            }
            recognizer = r
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listener?.onReady()
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    listener?.onEnd()
                }

                override fun onError(error: Int) {
                    listener?.onError("STT error code $error")
                }

                override fun onResults(results: Bundle?) {
                    val texts = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = texts?.firstOrNull().orEmpty()
                    if (best.isNotBlank()) listener?.onFinal(best)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val texts = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = texts?.firstOrNull().orEmpty()
                    if (best.isNotBlank()) listener?.onPartial(best)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            r.startListening(buildIntent(languageTag))
        }
    }

    fun stop() {
        mainHandler.post {
            try {
                recognizer?.stopListening()
            } catch (_: Exception) {
            }
        }
    }

    fun destroy() {
        mainHandler.post { destroyInternal() }
    }

    private fun destroyInternal() {
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
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (preferOnDevice) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
    }
}
