package app.openflow.bubble

/** Bubble scale + opacity math. Pure — JVM testable. */
object BubbleVisualDriver {

    /** Master scale x idle-shrink mode x search-shrink. Listening pins full size. */
    fun effectiveScale(
        scale: Float,
        shrinkIdle: Boolean,
        shrinkDot: Boolean,
        shrinkSearch: Boolean,
        listening: Boolean,
        searchFieldFocused: Boolean,
    ): Float {
        if (listening) return scale
        val mode = BubbleShrinkPolicy.idleMode(shrinkIdle, shrinkDot)
        val modeMul = when (mode) {
            "compact" -> 0.75f
            "dot" -> 0.55f
            else -> 1f
        }
        val searchMul = BubbleShrinkPolicy.searchMul(
            masterOn = shrinkIdle,
            shrinkSearch = shrinkSearch,
            searchFocused = searchFieldFocused,
            listening = listening,
        )
        return scale * modeMul * searchMul
    }

    /** Emphasis alpha: full when a field holds focus, dimmed otherwise. */
    fun emphasisAlpha(opacity: Float?, hasField: Boolean): Float =
        if (hasField) (opacity ?: 0.95f) else (opacity ?: 0.85f) * 0.8f
}
