package app.openflow.bubble

/** Cache bubble width/height for a drag gesture so MOVE does not remeasure each frame. */
class BubbleDragCache {
    var widthPx: Int = 0
        private set
    var heightPx: Int = 0
        private set

    fun begin(widthPx: Int, heightPx: Int) {
        this.widthPx = widthPx.coerceAtLeast(0)
        this.heightPx = heightPx.coerceAtLeast(0)
    }

    fun clear() {
        widthPx = 0
        heightPx = 0
    }

    fun sizeOr(fallbackW: Int, fallbackH: Int): Pair<Int, Int> =
        if (widthPx > 0 && heightPx > 0) widthPx to heightPx else fallbackW to fallbackH
}
