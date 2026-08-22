package app.openflow.stt.providers.ondevice

object OnPhoneModelUi {
    const val TINY_EN_URL =
        "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en-q5_1.bin"
    const val DONE_NOT_LIVE = "Text shows after Done. Not live like Gboard."

    fun line(ready: Boolean, busy: Boolean): String = when {
        busy -> "tiny.en · downloading"
        ready -> "tiny.en · ready"
        else -> "tiny.en · not on this phone"
    }
}
