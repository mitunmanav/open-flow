package app.openflow.stt.providers.cloud

import java.util.ArrayDeque

/** Buffer WS frames until the socket is actually open (OkHttp send-before-onOpen drops). */
class HoldUntilOpenSession(
    private val inner: CloudSession,
) : CloudSession {
    private val lock = Any()
    private var open = false
    private val pending = ArrayDeque<Any>()

    fun markOpen() {
        val flush: List<Any>
        synchronized(lock) {
            open = true
            flush = pending.toList()
            pending.clear()
        }
        for (item in flush) {
            when (item) {
                is ByteArray -> inner.send(item)
                is String -> inner.sendText(item)
            }
        }
    }

    override fun send(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        enqueueOrRun(bytes) { inner.send(it) }
    }

    override fun sendText(text: String) {
        enqueueOrRun(text) { inner.sendText(it) }
    }

    override fun close() {
        synchronized(lock) {
            pending.clear()
            open = true
        }
        inner.close()
    }

    private fun <T : Any> enqueueOrRun(item: T, send: (T) -> Unit) {
        val runNow: Boolean
        synchronized(lock) {
            if (open) {
                runNow = true
            } else {
                pending.addLast(item)
                runNow = false
            }
        }
        if (runNow) send(item)
    }
}
