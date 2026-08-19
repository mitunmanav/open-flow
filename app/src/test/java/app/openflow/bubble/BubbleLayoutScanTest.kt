package app.openflow.bubble

import app.openflow.ui.qa.UiSourceScan
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class BubbleLayoutScanTest {
    private fun root() = File(UiSourceScan.projectRoot(), "app/src/main/res/layout")

    @Test
    fun service_aborts_listen_on_bank_hide() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/bubble/FlowAccessibilityService.kt",
        ).readText()
        assertThat(src).contains("BubbleVisibility.shouldAbortListen")
        assertThat(src).contains("stopListening(false)")
    }

    @Test
    fun listen_chrome_is_three_includes() {
        val main = File(root(), "flow_bubble.xml").readText()
        assertThat(main).contains("@layout/bubble_close")
        assertThat(main).contains("@layout/bubble_wave")
        assertThat(main).contains("@layout/bubble_ok")
        assertThat(File(root(), "bubble_close.xml").readText()).contains("bubble_cancel")
        assertThat(File(root(), "bubble_ok.xml").readText()).contains("bubble_done")
        val wave = File(root(), "bubble_wave.xml").readText()
        assertThat(wave).contains("bubble_wave")
        assertThat(wave).contains("bubble_wave_0")
        assertThat(wave).contains("bubble_wave_3")
        listOf("bubble_close.xml", "bubble_ok.xml").forEach { name ->
            val xml = File(root(), name).readText()
            assertThat(xml).contains("48dp")
            assertThat(xml).doesNotContain("36dp")
        }
    }

    @Test
    fun listen_chrome_is_three_square_discs_with_gap() {
        listOf("bubble_close.xml", "bubble_wave.xml", "bubble_ok.xml").forEach { name ->
            val xml = File(root(), name).readText()
            assertThat(xml).contains("android:layout_width=\"48dp\"")
            assertThat(xml).contains("android:layout_height=\"48dp\"")
        }
        val close = File(root(), "bubble_close.xml").readText()
        val wave = File(root(), "bubble_wave.xml").readText()
        assertThat(close).contains("android:layout_marginEnd=\"8dp\"")
        assertThat(wave).contains("android:layout_marginEnd=\"8dp\"")
    }

    @Test
    fun listen_paints_three_discs_not_one_bar() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/bubble/FlowAccessibilityService.kt",
        ).readText()
        assertThat(src).contains("paintListenDisc")
        assertThat(src).contains("root.background = null")
        assertThat(src).doesNotContain("label.visibility = View.VISIBLE")
    }

    @Test
    fun setListenChrome_keeps_label_gone() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/bubble/FlowAccessibilityService.kt",
        ).readText()
        val chrome = src.substringAfter("fun setListenChrome").substringBefore("fun applyPrefsVisual")
        assertThat(chrome).contains("View.GONE")
        assertThat(chrome).doesNotContain("View.VISIBLE")
    }

    @Test
    fun listen_rms_does_not_scale_the_row() {
        val src = File(
            UiSourceScan.projectRoot(),
            "app/src/main/java/app/openflow/bubble/FlowAccessibilityService.kt",
        ).readText()
        val pulse = src.substringAfter("fun applyRmsPulse").substringBefore("fun paintListenDisc")
        assertThat(pulse).contains("BubbleListenMotion.overlayScale")
        assertThat(pulse).doesNotContain("BubblePulsePolicy.scale")
    }
}
