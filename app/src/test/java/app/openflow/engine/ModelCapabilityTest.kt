package app.openflow.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCapabilityTest {
    @Test
    fun system_ear_cannot_rewrite() {
        val c = ModelCapability.systemEar()
        assertFalse(c.rewrite)
        assertFalse(c.commandMode)
        assertFalse(c.needsNet)
    }

    @Test
    fun none_brain_is_rules_only() {
        val c = ModelCapability.noneBrain()
        assertFalse(c.rewrite)
        assertFalse(c.commandMode)
        assertFalse(c.needsNet)
        assertFalse(c.audioLeavesDevice)
    }

    @Test
    fun cloud_brain_can_rewrite() {
        val c = ModelCapability.cloudBrain()
        assertTrue(c.rewrite)
        assertTrue(c.commandMode)
        assertTrue(c.needsNet)
        assertTrue(c.audioLeavesDevice)
    }

    @Test
    fun system_ear_may_set_ear_punct() {
        val c = ModelCapability.systemEar()
        assertThat(c.earPunct).isTrue()
        assertThat(c.streamLive).isTrue()
        assertThat(c.audioLeavesDevice).isTrue()
        assertThat(c.languages).contains("en-US")
    }

    @Test
    fun other_factories_default_ear_punct_false() {
        assertThat(ModelCapability.noneBrain().earPunct).isFalse()
        assertThat(ModelCapability.cloudBrain().earPunct).isFalse()
        assertThat(ModelCapability.onPhoneEar().earPunct).isFalse()
        assertThat(ModelCapability.laptop().earPunct).isFalse()
    }

    @Test
    fun on_phone_ear_audio_stays() {
        val c = ModelCapability.onPhoneEar()
        assertThat(c.streamLive).isTrue()
        assertThat(c.rewrite).isFalse()
        assertThat(c.commandMode).isFalse()
        assertThat(c.audioLeavesDevice).isFalse()
        assertThat(c.needsNet).isFalse()
    }

    @Test
    fun laptop_can_rewrite_and_needs_net() {
        val c = ModelCapability.laptop()
        assertThat(c.streamLive).isTrue()
        assertThat(c.rewrite).isTrue()
        assertThat(c.commandMode).isTrue()
        assertThat(c.audioLeavesDevice).isTrue()
        assertThat(c.needsNet).isTrue()
    }
}
