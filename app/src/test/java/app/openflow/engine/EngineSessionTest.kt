package app.openflow.engine

import app.openflow.prefs.MemoryPrefsStore
import app.openflow.secrets.MemorySecretStore
import app.openflow.text.Feature
import app.openflow.text.FeatureAuto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EngineSessionTest {

    @Test
    fun pick_writes_ear_and_brain() {
        val prefs = EnginePrefs(MemoryPrefsStore())
        val session = EngineSession(prefs, MemorySecretStore())
        session.pick("sarvam", "grok")
        assertThat(prefs.earId).isEqualTo("sarvam")
        assertThat(prefs.brainId).isEqualTo("grok")
    }

    @Test
    fun save_key_writes_every_id_for_current_pick() {
        val secrets = MemorySecretStore()
        val prefs = EnginePrefs(MemoryPrefsStore())
        val session = EngineSession(prefs, secrets)
        session.pick("openai", "grok")
        session.saveKey("sk-abc12345")
        assertThat(secrets.get("openai")).isEqualTo("sk-abc12345")
        assertThat(secrets.get("grok")).isEqualTo("sk-abc12345")
        assertThat(secrets.get("deepgram")).isNull()
    }

    @Test
    fun save_key_same_id_once() {
        val secrets = MemorySecretStore()
        val session = EngineSession(EnginePrefs(MemoryPrefsStore()), secrets)
        session.pick("openai", "openai")
        session.saveKey("sk-same")
        assertThat(secrets.get("openai")).isEqualTo("sk-same")
    }

    @Test
    fun save_key_skips_system_none() {
        val secrets = MemorySecretStore()
        val session = EngineSession(EnginePrefs(MemoryPrefsStore()), secrets)
        session.pick("system", "none")
        session.saveKey("should-not-store")
        assertThat(secrets.get("system")).isNull()
        assertThat(secrets.get("none")).isNull()
        assertThat(secrets.get("openai")).isNull()
    }

    @Test
    fun save_key_writes_custom_stt_and_custom() {
        val secrets = MemorySecretStore()
        val session = EngineSession(EnginePrefs(MemoryPrefsStore()), secrets)
        session.pick("custom_stt", "custom")
        session.saveKey("tok-custom")
        assertThat(secrets.get("custom_stt")).isEqualTo("tok-custom")
        assertThat(secrets.get("custom")).isEqualTo("tok-custom")
    }

    @Test
    fun save_key_writes_laptop_for_laptop_brain() {
        val secrets = MemorySecretStore()
        val session = EngineSession(EnginePrefs(MemoryPrefsStore()), secrets)
        session.pick("system", "laptop")
        session.saveKey("host-key")
        assertThat(secrets.get("laptop")).isEqualTo("host-key")
    }

    @Test
    fun save_url_and_sarvam() {
        val prefs = EnginePrefs(MemoryPrefsStore())
        val session = EngineSession(prefs, MemorySecretStore())
        session.saveUrl("http://192.168.1.10:11434/v1")
        session.saveSarvam("translate")
        assertThat(prefs.customBaseUrl).isEqualTo("http://192.168.1.10:11434/v1")
        assertThat(prefs.sarvamMode).isEqualTo("translate")
    }

    @Test
    fun key_mask_empty_or_last4() {
        val secrets = MemorySecretStore()
        val session = EngineSession(EnginePrefs(MemoryPrefsStore()), secrets)
        assertThat(session.keyMask()).isEqualTo("")
        session.pick("deepgram", "none")
        assertThat(session.keyMask()).isEqualTo("")
        session.saveKey("sk-zz99")
        assertThat(session.keyMask()).isEqualTo("••••zz99")
        session.saveKey("ab")
        assertThat(session.keyMask()).isEqualTo("••••")
    }

    @Test
    fun features_match_feature_auto() {
        val prefs = EnginePrefs(MemoryPrefsStore())
        val session = EngineSession(prefs, MemorySecretStore())
        session.pick("system", "none")
        assertThat(session.features()).isEqualTo(FeatureAuto.of("system", "none"))
        assertThat(session.features()).doesNotContain(Feature.HIGH_AI)
        session.pick("openai", "grok")
        assertThat(session.features()).isEqualTo(FeatureAuto.of("openai", "grok"))
        assertThat(session.features()).contains(Feature.HIGH_AI)
        assertThat(session.features()).contains(Feature.COMMAND)
        assertThat(session.features()).contains(Feature.LIVE_PARTIAL)
    }

    @Test
    fun empty_save_clears_current_ids() {
        val secrets = MemorySecretStore()
        val session = EngineSession(EnginePrefs(MemoryPrefsStore()), secrets)
        session.pick("assemblyai", "anthropic")
        session.saveKey("keep-me-not")
        session.saveKey("")
        assertThat(secrets.get("assemblyai")).isNull()
        assertThat(secrets.get("anthropic")).isNull()
    }
}
