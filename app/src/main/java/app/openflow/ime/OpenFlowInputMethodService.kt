package app.openflow.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import app.openflow.R
import app.openflow.stt.SttEngine

/**
 * Voice IME: mic → on-device STT → commit text into focused field.
 */
class OpenFlowInputMethodService : InputMethodService() {

    private var statusView: TextView? = null
    private var partialView: TextView? = null
    private var stt: SttEngine? = null
    private var listening = false

    override fun onCreate() {
        super.onCreate()
        stt = SttEngine(applicationContext, preferOnDevice = true)
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.ime_voice, null)
        statusView = view.findViewById(R.id.ime_status)
        partialView = view.findViewById(R.id.ime_partial)

        view.findViewById<Button>(R.id.ime_mic).setOnClickListener {
            if (listening) stopListening() else startListening()
        }
        view.findViewById<Button>(R.id.ime_backspace).setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        view.findViewById<Button>(R.id.ime_space).setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }
        view.findViewById<Button>(R.id.ime_done).setOnClickListener {
            stopListening()
            requestHideSelf(0)
        }
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        statusView?.text = getString(R.string.listening)
        partialView?.text = ""
    }

    override fun onDestroy() {
        stopListening()
        stt?.destroy()
        stt = null
        super.onDestroy()
    }

    private fun startListening() {
        listening = true
        statusView?.text = getString(R.string.listening)
        stt?.setListener(object : SttEngine.Listener {
            override fun onPartial(text: String) {
                partialView?.text = text
            }

            override fun onFinal(text: String) {
                currentInputConnection?.commitText(text, 1)
                currentInputConnection?.commitText(" ", 1)
                partialView?.text = ""
                statusView?.text = "Committed"
                // Continuous dictation: restart
                if (listening) {
                    stt?.start()
                }
            }

            override fun onError(message: String) {
                statusView?.text = message
                listening = false
            }

            override fun onReady() {
                statusView?.text = getString(R.string.listening)
            }

            override fun onEnd() {}
        })
        stt?.start()
    }

    private fun stopListening() {
        listening = false
        stt?.stop()
        statusView?.text = "Stopped"
    }
}
