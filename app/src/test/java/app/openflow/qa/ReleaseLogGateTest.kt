package app.openflow.qa

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Release-log hygiene (LAUNCH_CHECKLIST #10):
 * Log.w / Log.e in release builds must be skipped via `if (BuildConfig.DEBUG)`.
 * Idiom source: FlowAccessibilityService (`if (BuildConfig.DEBUG) { Log.x(...) }`).
 * Read-only source scan. No writes.
 */
class ReleaseLogGateTest {

    /** The 7 files owned by this hygiene pass. Add future owners here. */
    private val ownedFiles = listOf(
        "app/src/main/java/app/openflow/OpenFlowApp.kt",
        "app/src/main/java/app/openflow/display/DisplayRefreshController.kt",
        "app/src/main/java/app/openflow/notify/DictationNotifier.kt",
        "app/src/main/java/app/openflow/ai/providers/cloud/OpenAiCompatBrain.kt",
        "app/src/main/java/app/openflow/ai/providers/cloud/AnthropicBrain.kt",
        "app/src/main/java/app/openflow/ai/providers/host/LaptopBrain.kt",
        "app/src/main/java/app/openflow/bubble/FlowAccessibilityService.kt",
    )

    private val logCall = Regex("""Log\.[we]\(""")
    private val debugGate = Regex("""if\s*\(\s*BuildConfig\.DEBUG\s*\)""")

    /**
     * APPROXIMATION (pragmatic): a Log.w/Log.e call counts as gated when an
     * `if (BuildConfig.DEBUG)` opener sits within [LOOKBACK] lines above it
     * (or earlier on the same line). The gated idiom puts the opener directly
     * above the call, so 3 lines cover the wrapped block + a multi-line call.
     * Not a Kotlin parser — a whole-function debug guard far above will read
     * as ungated; that is accepted (we gate each call directly, like
     * FlowAccessibilityService does).
     */
    private fun ungatedCalls(file: File): List<String> {
        val lines = file.readLines()
        val hits = mutableListOf<String>()
        lines.forEachIndexed { i, line ->
            if (!logCall.containsMatchIn(line)) return@forEachIndexed
            val windowStart = (i - LOOKBACK).coerceAtLeast(0)
            val gated = (windowStart..i).any { debugGate.containsMatchIn(lines[it]) }
            if (!gated) {
                hits += "${file.name}:${i + 1}: ${line.trim()}"
            }
        }
        return hits
    }

    private fun projectRoot(): File {
        var dir = File(System.getProperty("user.dir")).canonicalFile
        repeat(8) {
            if (File(dir, "app/src/main/java/app/openflow/OpenFlowApp.kt").isFile) return dir
            if (File(dir, "src/main/java/app/openflow/OpenFlowApp.kt").isFile) return dir.parentFile
            dir = dir.parentFile ?: return@repeat
        }
        error("open-flow root not found from ${System.getProperty("user.dir")}")
    }

    @Test
    fun warn_and_error_logs_are_debug_gated() {
        val ungated = ownedFiles.flatMap { rel ->
            val f = File(projectRoot(), rel)
            assertThat(f.isFile).isTrue()
            ungatedCalls(f)
        }
        assertThat(ungated).isEmpty()
    }

    private companion object {
        const val LOOKBACK = 3
    }
}
