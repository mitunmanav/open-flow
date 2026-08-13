package app.openflow.bubble

import app.openflow.text.Feature
import app.openflow.text.FeatureAuto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BrainPickTest {

    @Test
    fun none_and_on_phone_do_not_rewrite() {
        assertThat(BrainPick.rewrite("none")).isFalse()
        assertThat(BrainPick.rewrite("NONE")).isFalse()
        assertThat(BrainPick.rewrite("on_phone")).isFalse()
        assertThat(BrainPick.rewrite("")).isFalse()
    }

    @Test
    fun picked_cloud_or_laptop_rewrites() {
        assertThat(BrainPick.rewrite("grok")).isTrue()
        assertThat(BrainPick.rewrite("openai")).isTrue()
        assertThat(BrainPick.rewrite("laptop")).isTrue()
        assertThat(BrainPick.rewrite("custom")).isTrue()
        assertThat(BrainPick.rewrite("anthropic")).isTrue()
    }

    @Test
    fun command_is_same_rule_as_rewrite() {
        assertThat(BrainPick.command("none")).isFalse()
        assertThat(BrainPick.command("on_phone")).isFalse()
        assertThat(BrainPick.command("grok")).isTrue()
        assertThat(BrainPick.command("laptop")).isTrue()
        assertThat(BrainPick.command("grok")).isEqualTo(BrainPick.rewrite("grok"))
        assertThat(BrainPick.command("none")).isEqualTo(BrainPick.rewrite("none"))
    }

    @Test
    fun rewrite_and_command_follow_feature_auto() {
        for (brain in listOf(
            "none", "on_phone", "grok", "openai", "laptop", "custom",
            "anthropic", "unknown", "", "GROK",
        )) {
            val features = FeatureAuto.of("system", brain)
            assertThat(BrainPick.rewrite(brain)).isEqualTo(Feature.HIGH_AI in features)
            assertThat(BrainPick.command(brain)).isEqualTo(Feature.COMMAND in features)
        }
    }
}
