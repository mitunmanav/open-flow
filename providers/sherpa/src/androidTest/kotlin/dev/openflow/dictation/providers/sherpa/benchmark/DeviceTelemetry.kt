package dev.openflow.dictation.providers.sherpa.benchmark

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import java.io.File

/**
 * What the device was doing while a number was measured.
 *
 * `docs/providers/model-benchmark-results.md`'s first rule is *"name the device"*, and
 * `model-selection.md`'s *Method* says *"thermal state noted"*. Both are read from the device
 * rather than remembered, and every reading is nullable — a device that will not answer is
 * recorded as unknown rather than as a plausible default, because `0` for "peak RSS" or `0 %` for
 * "battery drained" is a number nobody measured.
 *
 * **Peak RSS and peak PSS are both reported, and they answer different questions.** RSS is
 * every page this process holds, counted whole; PSS divides shared pages — the ONNX runtime's,
 * the Java runtime's — between the processes sharing them. Two models with identical RSS can
 * show different PSS, and the plan's bound is written against RSS, so RSS is the figure a
 * verdict uses and PSS is carried beside it for context.
 */
internal class DeviceTelemetry(private val context: Context) {

    /**
     * The device's core count, as the JVM sees it.
     *
     * Recorded beside the thread widths the manifest asked for, so `threads = 2, 4` on a two-core
     * phone reads as what happened rather than as what was requested. Android exposes no "big
     * little performance core count" to an app, so this is the number `numThreads` competes for,
     * and saying so is better than implying a precision the platform does not offer.
     */
    val cpuCount: Int = Runtime.getRuntime().availableProcessors()

    private val powerManager: PowerManager? = context.getSystemService(PowerManager::class.java)

    /**
     * The page size `/proc/self/statm` counts in.
     *
     * Android has no public accessor for it — `android.system.Os.sysconf` is hidden API — and it
     * is 4096 on every ABI this project supports. Stated rather than hidden: if a future device
     * disagrees, RSS here is off by exactly the ratio, and `peakPssKb` is the figure that does
     * not depend on it.
     */
    private val pageSizeBytes = 4096L

    // ---- thermal ---------------------------------------------------------------------

    /** The current thermal status as `name(code)`, or `"unknown (…)"`. */
    fun thermalStatus(): String {
        if (Build.VERSION.SDK_INT < 29) return "unknown (API ${Build.VERSION.SDK_INT} < 29)"
        val status = powerManager?.currentThermalStatus ?: return "unknown (no PowerManager)"
        return renderThermal(status)
    }

    /**
     * A thermal-status listener covering the duration of one cell.
     *
     * The worst status seen during the cell is recorded, not only the status at each end. A phone
     * that reached `severe` and cooled back to `none` between two samples would otherwise read as
     * never having throttled — which is the specific misreading the plan asks for the thermal
     * state to prevent, since a throttled iteration is what makes a median over 20 the right
     * statistic rather than a mean.
     */
    inner class ThermalWatch {
        val before: String = thermalStatus()
        private var worst: Int = statusCodeOf(before)
        private var listener: PowerManager.OnThermalStatusChangedListener? = null

        init {
            if (Build.VERSION.SDK_INT >= 29) {
                val pm = powerManager
                if (pm != null) {
                    val created = PowerManager.OnThermalStatusChangedListener { status ->
                        if (status > worst) worst = status
                    }
                    listener = created
                    // A listener registration that the platform refuses must not lose the run;
                    // the cell then reports only its endpoint samples, which [before] says so.
                    runCatching { pm.addThermalStatusListener(context.mainExecutor, created) }
                }
            }
        }

        /** The worst status seen during the cell, and the status now. */
        fun finish(): Pair<String, String> {
            val after = thermalStatus()
            if (statusCodeOf(after) > worst) worst = statusCodeOf(after)
            return renderThermal(worst) to after
        }

        fun release() {
            if (Build.VERSION.SDK_INT >= 29) {
                val pm = powerManager ?: return
                val registered = listener ?: return
                runCatching { pm.removeThermalStatusListener(registered) }
            }
        }
    }

    fun startThermalWatch(): ThermalWatch = ThermalWatch()

    private fun statusCodeOf(label: String): Int =
        Regex("\\((\\d+)\\)$").find(label)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MIN_VALUE

    /**
     * `PowerManager.THERMAL_STATUS_*`, written as literals.
     *
     * Those constants are `@RequiresApi(29)` and this function is called from paths the compiler
     * cannot prove are guarded, so naming them would make lint fail the build. The values are the
     * API's, and the names are here so a reader does not have to look them up.
     */
    private fun renderThermal(status: Int): String {
        if (status == Int.MIN_VALUE) return "unknown"
        val name = when (status) {
            0 -> "none"          // THERMAL_STATUS_NONE
            1 -> "light"         // THERMAL_STATUS_LIGHT
            2 -> "moderate"      // THERMAL_STATUS_MODERATE
            3 -> "severe"        // THERMAL_STATUS_SEVERE
            4 -> "critical"      // THERMAL_STATUS_CRITICAL
            5 -> "emergency"     // THERMAL_STATUS_EMERGENCY
            6 -> "shutdown"      // THERMAL_STATUS_SHUTDOWN
            else -> "unrecognised"
        }
        return "$name($status)"
    }

    // ---- memory ----------------------------------------------------------------------

    /**
     * Resident set size of this process in bytes, or `null` if `/proc` would not say.
     *
     * Field 2 of `/proc/self/statm` is the resident page count. Inside a `runCatching` because a
     * locked-down `/proc` would otherwise turn a memory metric into a failed run, and a missing
     * metric is better than a wrong one.
     */
    fun residentSetSizeBytes(): Long? = runCatching {
        File("/proc/self/statm").readText().trim().split(Regex("\\s+"))[1].toLong() * pageSizeBytes
    }.getOrNull()

    /** Total PSS in KB, the number Android itself reports for this process. */
    fun totalPssKb(): Long? = runCatching {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        info.totalPss.toLong()
    }.getOrNull()

    // ---- battery ---------------------------------------------------------------------

    /** One battery reading. `plugged` is a `BatteryManager.BATTERY_PLUGGED_*` value. */
    data class BatteryReading(val percent: Double, val level: Int, val scale: Int, val plugged: Int) {
        val charging: Boolean get() = plugged != 0

        /** One line, for a cell's notes. */
        fun render(): String {
            val shown = if (percent % 1.0 == 0.0) percent.toInt().toString() else "%.1f".format(percent)
            return "battery: $shown% level=$level/$scale plugged=${pluggedName()}"
        }

        private fun pluggedName(): String = when (plugged) {
            0 -> "none"
            1 -> "ac"        // BATTERY_PLUGGED_AC
            2 -> "usb"       // BATTERY_PLUGGED_USB
            4 -> "wireless"  // BATTERY_PLUGGED_WIRELESS
            8 -> "dock"      // BATTERY_PLUGGED_DOCK, API 33
            else -> "other($plugged)"
        }
    }

    /**
     * The sticky battery broadcast, which is the only way to read the level without a listener.
     *
     * `percent` is `level / scale` rather than `BatteryManager.BATTERY_PROPERTY_CAPACITY` because
     * the sticky broadcast's `EXTRA_LEVEL` is what the phone's own battery widget shows, and a
     * battery figure a reader cannot reconcile with the device in their hand is not evidence.
     */
    fun battery(): BatteryReading? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        if (level < 0 || scale <= 0) return null
        return BatteryReading(100.0 * level / scale, level, scale, plugged)
    }

    /** The device's model and OEM strings, for a report whose first rule is "name the device". */
    fun describe(): String = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DISPLAY})".trim()

    companion object {
        /**
         * The sherpa-onnx build the numbers came from, as the artifact reports it.
         *
         * A JNI call, so it is inside a `runCatching` with the pinned coordinate as the fallback:
         * a report that names the version it could not read is worse than one that names the
         * version it was built against, and the coordinate is in `gradle/libs.versions.toml`.
         */
        fun sherpaVersion(): String = runCatching { com.k2fsa.sherpa.onnx.VersionInfo.version }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "v1.13.8 (pinned; VersionInfo unreadable)"
    }
}
