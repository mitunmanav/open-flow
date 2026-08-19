package app.openflow.bubble

object EarErrorPolicy {
    enum class Kind { SOFT, MIC, SHOW }

    fun classify(message: String, fatal: Boolean): Kind {
        val mic = message.contains("Microphone", ignoreCase = true) ||
            message.contains("Allow mic", ignoreCase = true)
        if (mic) return Kind.MIC
        if (!fatal && (
                message.contains("Silence", ignoreCase = true) ||
                    message.contains("No match", ignoreCase = true) ||
                    message.contains("Busy", ignoreCase = true) ||
                    message.contains("No recognition", ignoreCase = true) ||
                    message.contains("Retrying", ignoreCase = true)
                )
        ) return Kind.SOFT
        return Kind.SHOW
    }
}
