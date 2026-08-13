package app.openflow.display

import kotlin.math.abs

/**
 * Adaptive refresh targets: 60 / 90 / 120 / 144 Hz.
 * Pure pick logic — apply via Window preferredDisplayModeId / setFrameRate.
 */
object DisplayRefreshPolicy {

    val TARGETS_HZ = listOf(60, 90, 120, 144)

    fun normalizePreference(hz: Int): Int =
        TARGETS_HZ.minByOrNull { abs(it - hz) } ?: 60

    data class ModeInfo(val modeId: Int, val refreshRateHz: Float)

    /**
     * Pick best mode for [preferredHz].
     * Prefer exact match within 2 Hz; else closest available.
     */
    fun pickMode(modes: List<ModeInfo>, preferredHz: Int): ModeInfo? {
        if (modes.isEmpty()) return null
        val pref = normalizePreference(preferredHz).toFloat()
        val exact = modes.filter { abs(it.refreshRateHz - pref) <= 2f }
        if (exact.isNotEmpty()) {
            return exact.minByOrNull { abs(it.refreshRateHz - pref) }
        }
        return modes.minByOrNull { abs(it.refreshRateHz - pref) }
    }

    /** Available target chips given device modes (for Settings UI). */
    fun availableTargets(modes: List<ModeInfo>): List<Int> {
        if (modes.isEmpty()) return listOf(60)
        return TARGETS_HZ.filter { target ->
            modes.any { abs(it.refreshRateHz - target) <= 3f }
        }.ifEmpty { listOf(60) }
    }

    /** Skip Window work when already on the preferred rate (within 2 Hz). */
    fun needsApply(currentHz: Float?, preferredHz: Int): Boolean {
        if (currentHz == null) return true
        val pref = normalizePreference(preferredHz).toFloat()
        return abs(currentHz - pref) > 2f
    }

    /** Skip Window work when no pick, or already on that mode. */
    fun needsApply(currentModeId: Int, pick: ModeInfo?): Boolean {
        if (pick == null) return false
        return currentModeId != pick.modeId
    }
}
