package app.openflow.bubble

import kotlin.math.sqrt

/**
 * Pure shake detector from raw accelerometer m/s².
 * Threshold ~2.7G (common Android practice / Medium guides).
 */
class ShakeDetector(
    private val thresholdG: Float = 2.7f,
    private val minIntervalMs: Long = 800L
) {
    private var lastShakeMs = 0L

    fun onAccel(ax: Float, ay: Float, az: Float, nowMs: Long): Boolean {
        val gX = ax / 9.80665f
        val gY = ay / 9.80665f
        val gZ = az / 9.80665f
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
        if (gForce < thresholdG) return false
        if (nowMs - lastShakeMs < minIntervalMs) return false
        lastShakeMs = nowMs
        return true
    }
}
