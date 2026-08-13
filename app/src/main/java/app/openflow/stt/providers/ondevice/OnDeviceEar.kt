package app.openflow.stt.providers.ondevice

import app.openflow.stt.SpeechEngine
import java.io.File

/** On-phone ear stub. No JNI this slice — start fails until a model file + runtime exist. */
class OnDeviceEar(
    private val modelFile: File? = null,
    private val micGranted: Boolean = false,
) : SpeechEngine {

    private var listener: SpeechEngine.Listener? = null

    override val isAvailable: Boolean
        get() = modelFile?.isFile == true

    override fun hasMicPermission(): Boolean = micGranted

    override fun setListener(listener: SpeechEngine.Listener?) {
        this.listener = listener
    }

    override fun startContinuous(languageTag: String) {
        rejectIfMissing()
    }

    override fun startOnce(languageTag: String) {
        rejectIfMissing()
    }

    override fun stop() {}

    override fun destroy() {
        listener = null
    }

    private fun rejectIfMissing() {
        if (modelFile == null || !modelFile.isFile) {
            listener?.onError("model not installed", fatal = true)
        }
    }
}
