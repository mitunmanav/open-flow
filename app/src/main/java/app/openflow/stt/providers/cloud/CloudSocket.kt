package app.openflow.stt.providers.cloud

fun interface CloudSocket {
    fun connect(
        url: String,
        headers: Map<String, String>,
        onText: (String) -> Unit,
    ): CloudSession
}

interface CloudSession {
    fun send(bytes: ByteArray)
    fun sendText(text: String)
    fun close()
}
