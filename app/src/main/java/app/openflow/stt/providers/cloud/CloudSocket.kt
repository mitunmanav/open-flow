package app.openflow.stt.providers.cloud

interface CloudSocket {
    fun connect(
        url: String,
        headers: Map<String, String>,
        onError: (String) -> Unit = {},
        onText: (String) -> Unit,
    ): CloudSession
}

interface CloudSession {
    fun send(bytes: ByteArray)
    fun sendText(text: String)
    fun close()
}
