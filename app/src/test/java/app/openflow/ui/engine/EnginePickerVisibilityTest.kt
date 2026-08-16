package app.openflow.ui.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnginePickerVisibilityTest {

    @Test
    fun unfinishedEar_hiddenWhenEarGateOff() {
        assertThat(EnginePickerVisibility.showEar("on_phone")).isFalse()
        assertThat(EnginePickerVisibility.showEar("laptop")).isFalse()
        assertThat(EnginePickerVisibility.showEar("custom_stt")).isFalse()
        assertThat(EnginePickerVisibility.showEar("system")).isTrue()
        assertThat(EnginePickerVisibility.showEar("openai")).isTrue()
    }

    @Test
    fun onDeviceBrain_alwaysHidden() {
        assertThat(EnginePickerVisibility.showBrain("on_phone", url = "")).isFalse()
        assertThat(EnginePickerVisibility.showBrain("on_phone", url = "https://example.com/v1")).isFalse()
    }

    @Test
    fun laptopAndCustomBrain_hiddenUntilValidUrl() {
        assertThat(EnginePickerVisibility.showBrain("laptop", url = "")).isFalse()
        assertThat(EnginePickerVisibility.showBrain("custom", url = "not-a-url")).isFalse()
        assertThat(EnginePickerVisibility.showBrain("laptop", url = "https://example.com/v1")).isTrue()
        assertThat(EnginePickerVisibility.showBrain("custom", url = "http://192.168.1.1:11434/v1")).isTrue()
        assertThat(EnginePickerVisibility.showBrain("none", url = "")).isTrue()
        assertThat(EnginePickerVisibility.showBrain("openai", url = "")).isTrue()
    }

    @Test
    fun visibleEars_omitComingLaterStubs() {
        val ids = EnginePickerVisibility.visibleEars().map { it.id }
        assertThat(ids).containsAtLeast("system", "openai", "deepgram", "assemblyai", "sarvam")
        assertThat(ids).containsNoneOf("on_phone", "laptop", "custom_stt")
    }

    @Test
    fun visibleBrains_omitOnPhone_andUrlGatedUntilReady() {
        val emptyUrl = EnginePickerVisibility.visibleBrains(url = "").map { it.id }
        assertThat(emptyUrl).doesNotContain("on_phone")
        assertThat(emptyUrl).doesNotContain("laptop")
        assertThat(emptyUrl).doesNotContain("custom")
        assertThat(emptyUrl).contains("none")
        assertThat(emptyUrl).contains("openai")

        val withUrl = EnginePickerVisibility.visibleBrains(url = "https://example.com/v1").map { it.id }
        assertThat(withUrl).doesNotContain("on_phone")
        assertThat(withUrl).contains("laptop")
        assertThat(withUrl).contains("custom")
    }

    @Test
    fun visibleEarSections_dropEmptyComingLater() {
        val secs = EnginePickerVisibility.visibleEarSections()
        assertThat(secs.map { it.id }).containsExactly("local", "cloud").inOrder()
        assertThat(secs.none { it.id == "later" }).isTrue()
    }

    @Test
    fun visibleBrainSections_dropEmptyLaterUntilUrl() {
        val empty = EnginePickerVisibility.visibleBrainSections(url = "")
        assertThat(empty.map { it.id }).containsExactly("rules", "cloud").inOrder()

        val ready = EnginePickerVisibility.visibleBrainSections(url = "https://example.com/v1")
        assertThat(ready.map { it.id }).contains("later")
        assertThat(ready.last().items.map { it.id }).containsExactly("laptop", "custom").inOrder()
    }
}
