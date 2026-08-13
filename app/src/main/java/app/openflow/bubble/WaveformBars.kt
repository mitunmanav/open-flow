package app.openflow.bubble

/** 4-cell RMS meter. Not a clock. */
object WaveformBars {
    fun fromRms(rmsdB: Float): String {
        val t = rmsdB.coerceIn(0f, 10f)
        val filled = when {
            t <= 0.01f -> 0
            t < 2.5f -> 1
            t < 5f -> 2
            t < 7.5f -> 3
            else -> 4
        }
        return buildString {
            repeat(4) { i -> append(if (i < filled) '▮' else '▯') }
        }
    }
}
