package app.openflow.orchestrate

enum class AiWhen {
    EVERY,
    MISS_ONLY,
    ;

    val pref: String
        get() = if (this == MISS_ONLY) "miss_only" else "every"

    companion object {
        fun fromPref(raw: String): AiWhen =
            if (raw.trim().lowercase() == "miss_only") MISS_ONLY else EVERY
    }
}
