package app.openflow.stt.providers.cloud

/** Connect never throws. Dead session: send/close are no-ops. */
class FailSoftSocket : CloudSocket {
    override fun connect(
        url: String,
        headers: Map<String, String>,
        onError: (String) -> Unit,
        onText: (String) -> Unit,
    ): CloudSession = Dead

    private object Dead : CloudSession {
        override fun send(bytes: ByteArray) = Unit
        override fun sendText(text: String) = Unit
        override fun close() = Unit
    }
}
