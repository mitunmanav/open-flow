package app.openflow.engine

import app.openflow.prefs.MemoryPrefsStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EnginePrefsTest {

    @Test
    fun defaults_are_system_ear_and_none_brain() {
        val prefs = EnginePrefs(MemoryPrefsStore())
        assertThat(prefs.earId).isEqualTo("system")
        assertThat(prefs.brainId).isEqualTo("none")
        assertThat(prefs.brainModel).isEmpty()
        assertThat(prefs.earModel).isEmpty()
        assertThat(prefs.customBaseUrl).isEmpty()
        assertThat(prefs.sarvamMode).isEqualTo("transcribe")
        assertThat(ProviderId.parseEar(prefs.earId)).isEqualTo(EarId.SYSTEM)
        assertThat(ProviderId.parseBrain(prefs.brainId)).isEqualTo(BrainId.NONE)
    }

    @Test
    fun fields_round_trip() {
        val store = MemoryPrefsStore()
        val prefs = EnginePrefs(store)
        prefs.earId = "sarvam"
        prefs.brainId = "grok"
        prefs.brainModel = "grok-3"
        prefs.earModel = "saaras:v3"
        prefs.customBaseUrl = "http://192.168.1.10:11434/v1"
        prefs.sarvamMode = "translate"
        assertThat(prefs.earId).isEqualTo("sarvam")
        assertThat(prefs.brainId).isEqualTo("grok")
        assertThat(prefs.brainModel).isEqualTo("grok-3")
        assertThat(prefs.earModel).isEqualTo("saaras:v3")
        assertThat(prefs.customBaseUrl).isEqualTo("http://192.168.1.10:11434/v1")
        assertThat(prefs.sarvamMode).isEqualTo("translate")
    }

    @Test
    fun blank_ids_fall_back_to_defaults() {
        val prefs = EnginePrefs(MemoryPrefsStore())
        prefs.earId = ""
        prefs.brainId = "   "
        assertThat(prefs.earId).isEqualTo("system")
        assertThat(prefs.brainId).isEqualTo("none")
    }

    @Test
    fun unknown_sarvam_mode_is_transcribe() {
        val prefs = EnginePrefs(MemoryPrefsStore())
        prefs.sarvamMode = "nope"
        assertThat(prefs.sarvamMode).isEqualTo("transcribe")
    }

    @Test
    fun uses_own_store_not_flow_prefs_keys() {
        val store = MemoryPrefsStore()
        val prefs = EnginePrefs(store)
        prefs.earId = "laptop"
        prefs.brainId = "openai"
        assertThat(store.getString("ear_id", "")).isEqualTo("laptop")
        assertThat(store.getString("brain_id", "")).isEqualTo("openai")
        assertThat(store.getString("auto_learn", "")).isEmpty()
    }
}
