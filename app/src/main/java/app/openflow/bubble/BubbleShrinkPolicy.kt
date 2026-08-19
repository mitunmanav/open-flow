package app.openflow.bubble

object BubbleShrinkPolicy {
    fun idleMode(masterOn: Boolean, toDot: Boolean): String =
        when {
            !masterOn -> "full"
            toDot -> "dot"
            else -> "compact"
        }

    fun searchMul(
        masterOn: Boolean,
        shrinkSearch: Boolean,
        searchFocused: Boolean,
        listening: Boolean,
    ): Float =
        if (masterOn && shrinkSearch && searchFocused && !listening) 0.72f else 1f
}
