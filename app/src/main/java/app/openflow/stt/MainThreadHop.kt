package app.openflow.stt

/** Hop off-thread SpeechEngine callbacks onto the UI thread. */
object MainThreadHop {
    fun run(isMain: Boolean, post: (() -> Unit) -> Unit, block: () -> Unit) {
        if (isMain) block() else post(block)
    }
}
