package app.openflow.runtime

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrimPolicyTest {

    @Test
    fun android_levels_match_component_callbacks2() {
        // ComponentCallbacks2: UI_HIDDEN=20, BACKGROUND=40
        assertThat(TrimPolicy.TRIM_MEMORY_UI_HIDDEN).isEqualTo(20)
        assertThat(TrimPolicy.TRIM_MEMORY_BACKGROUND).isEqualTo(40)
    }

    @Test
    fun drop_idle_stt_at_background() {
        assertThat(TrimPolicy.shouldDropIdleStt(40)).isTrue()
        assertThat(TrimPolicy.action(40)).isEqualTo(TrimPolicy.Action.DROP_IDLE_STT)
    }

    @Test
    fun drop_idle_stt_above_background() {
        assertThat(TrimPolicy.shouldDropIdleStt(80)).isTrue()
    }

    @Test
    fun ui_hidden_does_not_drop_idle_stt() {
        assertThat(TrimPolicy.shouldDropIdleStt(20)).isFalse()
        assertThat(TrimPolicy.shouldDropIdleStt(39)).isFalse()
        assertThat(TrimPolicy.action(20)).isEqualTo(TrimPolicy.Action.RELEASE_UI)
    }

    @Test
    fun ui_hidden_releases_ui_caches() {
        assertThat(TrimPolicy.shouldReleaseUiCaches(20)).isTrue()
        assertThat(TrimPolicy.shouldReleaseUiCaches(40)).isTrue()
        assertThat(TrimPolicy.shouldReleaseUiCaches(19)).isFalse()
    }

    @Test
    fun keep_idle_stt_below_background() {
        assertThat(TrimPolicy.shouldDropIdleStt(39)).isFalse()
        assertThat(TrimPolicy.shouldDropIdleStt(20)).isFalse()
        assertThat(TrimPolicy.shouldDropIdleStt(0)).isFalse()
        assertThat(TrimPolicy.action(0)).isEqualTo(TrimPolicy.Action.KEEP)
    }

    @Test
    fun drop_idle_engine_when_background_and_idle() {
        assertThat(
            TrimPolicy.dropIdleEngine(level = 40, listening = false, stopInProgress = false)
        ).isTrue()
    }

    @Test
    fun keep_engine_while_listening() {
        assertThat(
            TrimPolicy.dropIdleEngine(level = 40, listening = true, stopInProgress = false)
        ).isFalse()
    }

    @Test
    fun keep_engine_while_flush() {
        assertThat(
            TrimPolicy.dropIdleEngine(level = 40, listening = false, stopInProgress = true)
        ).isFalse()
    }

    @Test
    fun keep_engine_on_ui_hidden() {
        assertThat(
            TrimPolicy.dropIdleEngine(level = 20, listening = false, stopInProgress = false)
        ).isFalse()
    }
}
