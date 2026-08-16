package app.openflow.bubble

import kotlin.math.abs

/** Overlay drag: slop, fling-to-edge, snap timing. Gravity.END x. */
object BubbleMotion {
    const val DEFAULT_SLOP_PX = 16
    const val FLING_PX_PER_SEC = 800f

    fun passedSlop(dx: Int, dy: Int, slopPx: Int = DEFAULT_SLOP_PX): Boolean =
        abs(dx) > slopPx || abs(dy) > slopPx

    fun shouldUpdateLayout(dragged: Boolean): Boolean = dragged

    /**
     * Snap X from current overlay offset.
     * Positive [vxPxPerSec] = finger right → Gravity.END (small x).
     */
    fun snapX(
        x: Int,
        vxPxPerSec: Float,
        screenWidthPx: Int,
        bubbleWidthPx: Int,
        flingPxPerSec: Float = FLING_PX_PER_SEC,
        marginPx: Int = BubbleGeometry.DEFAULT_MARGIN_PX,
    ): Int {
        val right = marginPx
        val left = (screenWidthPx - bubbleWidthPx - marginPx).coerceAtLeast(marginPx)
        return when {
            vxPxPerSec > flingPxPerSec -> right
            vxPxPerSec < -flingPxPerSec -> left
            else -> BubbleGeometry.snapOffsetFromEnd(x, screenWidthPx, bubbleWidthPx, marginPx)
        }
    }

    fun snapDurationMs(distancePx: Int, vxPxPerSec: Float): Long {
        val speed = abs(vxPxPerSec).coerceAtLeast(700f)
        val ms = (abs(distancePx) / speed * 1000f).toLong()
        return ms.coerceIn(140L, 280L)
    }

    fun skipImmediateXWrite(snapping: Boolean): Boolean = snapping
}
