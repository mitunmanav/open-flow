package app.openflow.bubble

object BubbleScaleSteps {
    val STEPS: List<Float> = listOf(0.70f, 0.85f, 1.00f, 1.15f)
    const val DEFAULT = 0.85f

    fun nearest(v: Float): Float =
        STEPS.minBy { kotlin.math.abs(it - v) }
}
