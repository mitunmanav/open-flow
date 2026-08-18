package app.openflow.prefs

import app.openflow.stt.SttTuning
import app.openflow.text.CleanupLevel
import app.openflow.text.WritingStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Feature prefs must exist. Missing getter = hole.
 * Empty store must not crash.
 */
class FlowPrefsFeatureMapTest {

    @Test
    fun missing_keys_use_safe_defaults() {
        val p = FlowPrefs(MemoryPrefsStore())
        assertThat(p.cleanupLevel).isEqualTo("medium")
        assertThat(p.cleanup()).isEqualTo(CleanupLevel.NORMAL)
        assertThat(p.styleName).isEqualTo(WritingStyle.CASUAL.name)
        assertThat(p.style()).isEqualTo(WritingStyle.CASUAL)
        assertThat(p.retentionPolicy).isEqualTo("keep")
        assertThat(p.bubbleScale).isEqualTo(0.85f)
        assertThat(p.sttProfile).isEqualTo(SttTuning.PROFILE_BALANCED)
        assertThat(p.sttTuning().preferFormattingQuality).isTrue()
        assertThat(p.preferOnDevice).isFalse()
        assertThat(p.autoLearn).isTrue()
        assertThat(p.darkMode.value).isEqualTo("light")
        assertThat(p.seenHowTo).isFalse()
        assertThat(p.languageTag).isEqualTo(SttTuning.DEFAULT_LANGUAGE)
    }

    @Test
    fun cleanup_level_roundtrip_and_normalize() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.cleanupLevel = "high"
        assertThat(p.cleanupLevel).isEqualTo("high")
        assertThat(p.cleanup()).isEqualTo(CleanupLevel.HIGH)
        p.cleanupLevel = "NONE"
        assertThat(p.cleanupLevel).isEqualTo("none")
        assertThat(p.cleanup()).isEqualTo(CleanupLevel.RAW)
        p.cleanupLevel = "light"
        assertThat(p.cleanup()).isEqualTo(CleanupLevel.LIGHT)
        p.cleanupLevel = "garbage"
        assertThat(p.cleanupLevel).isEqualTo("medium")
        assertThat(p.cleanup()).isEqualTo(CleanupLevel.NORMAL)
    }

    @Test
    fun style_roundtrip_and_unknown_falls_casual() {
        val store = MemoryPrefsStore()
        val p = FlowPrefs(store)
        p.styleName = "formal"
        assertThat(p.style()).isEqualTo(WritingStyle.FORMAL)
        assertThat(store.getString("style", "")).isEqualTo(WritingStyle.FORMAL.name)
        p.styleName = "nope"
        assertThat(p.style()).isEqualTo(WritingStyle.CASUAL)
    }

    @Test
    fun retention_roundtrip_and_normalize() {
        val store = MemoryPrefsStore()
        val p = FlowPrefs(store)
        p.retentionPolicy = "wipe_24h"
        assertThat(p.retentionPolicy).isEqualTo("wipe_24h")
        p.retentionPolicy = "NEVER_STORE"
        assertThat(p.retentionPolicy).isEqualTo("never_store")
        assertThat(store.getString("retention", "")).isEqualTo("never_store")
        p.retentionPolicy = "cloud"
        assertThat(p.retentionPolicy).isEqualTo("keep")
    }

    @Test
    fun bubble_scale_roundtrip() {
        val store = MemoryPrefsStore()
        val p = FlowPrefs(store)
        p.bubbleScale = 1.1f
        assertThat(p.bubbleScale).isWithin(0.001f).of(1.1f)
        assertThat(store.getFloat("bubble_scale", 0f)).isWithin(0.001f).of(1.1f)
    }

    @Test
    fun language_tag_roundtrip_catalog() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.languageTag = "hi-IN"
        assertThat(p.languageTag).isEqualTo("hi-IN")
        p.languageTag = "en-IN"
        assertThat(p.languageTag).isEqualTo("en-IN")
    }

    @Test
    fun stt_profile_maps_tuning() {
        val p = FlowPrefs(MemoryPrefsStore())
        p.sttProfile = "fast"
        assertThat(p.sttTuning().completeSilenceMs).isLessThan(SttTuning().completeSilenceMs)
        p.sttProfile = "accurate"
        assertThat(p.sttTuning().preferFormattingQuality).isTrue()
        p.sttProfile = "nope"
        assertThat(p.sttProfile).isEqualTo(SttTuning.PROFILE_BALANCED)
    }

    @Test
    fun prefer_on_device_roundtrip_default_off() {
        val store = MemoryPrefsStore()
        val p = FlowPrefs(store)
        assertThat(p.preferOnDevice).isFalse()
        p.preferOnDevice = true
        assertThat(p.preferOnDevice).isTrue()
        assertThat(store.getString("prefer_on_device", "")).isEqualTo("true")
        p.preferOnDevice = false
        assertThat(p.preferOnDevice).isFalse()
    }
}
