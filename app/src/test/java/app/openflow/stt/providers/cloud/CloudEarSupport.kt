package app.openflow.stt.providers.cloud

import app.openflow.stt.SpeechEngine

internal class RecListener : SpeechEngine.Listener {
    val errors = mutableListOf<String>()
    val fatal = mutableListOf<Boolean>()
    val partials = mutableListOf<String>()
    val finals = mutableListOf<String>()
    var ready = 0
    val listening = mutableListOf<Boolean>()
    var needMic = 0

    override fun onPartial(text: String) {
        partials += text
    }

    override fun onFinal(text: String) {
        finals += text
    }

    override fun onError(message: String, fatal: Boolean) {
        errors += message
        this.fatal += fatal
    }

    override fun onReady() {
        ready++
    }

    override fun onListeningChanged(listening: Boolean) {
        this.listening += listening
    }

    override fun onNeedMicPermission() {
        needMic++
    }
}

internal class FakeSocket : CloudSocket {
    var url: String? = null
    var headers: Map<String, String> = emptyMap()
    var onText: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var closed = false
    val sentText = mutableListOf<String>()
    val sentBytes = mutableListOf<ByteArray>()

    override fun connect(
        url: String,
        headers: Map<String, String>,
        onError: (String) -> Unit,
        onText: (String) -> Unit,
    ): CloudSession {
        this.url = url
        this.headers = headers
        this.onText = onText
        this.onError = onError
        closed = false
        return object : CloudSession {
            override fun send(bytes: ByteArray) {
                sentBytes += bytes
            }

            override fun sendText(text: String) {
                sentText += text
            }

            override fun close() {
                closed = true
            }
        }
    }

    fun push(msg: String) {
        onText?.invoke(msg)
    }

    fun fail(msg: String) {
        onError?.invoke(msg)
    }
}
