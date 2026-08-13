package app.openflow.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SendPolicyTest {
    @Test
    fun for_brain_strips_email_and_phone() {
        val out = SendPolicy.forBrain("mail me at a@b.com or 9876543210")
        assertThat(out).doesNotContain("a@b.com")
        assertThat(out).doesNotContain("9876543210")
        assertThat(out).contains("mail me at")
        assertThat(out).contains("or")
    }

    @Test
    fun for_brain_keeps_other_words() {
        val out = SendPolicy.forBrain("please send the report today")
        assertThat(out).isEqualTo("please send the report today")
    }

    @Test
    fun for_brain_incomplete_tokens_may_remain() {
        val out = SendPolicy.forBrain("call 123 or mail a@")
        assertThat(out).contains("call")
        assertThat(out).contains("123")
        assertThat(out).contains("mail")
        assertThat(out).contains("a@")
    }

    @Test
    fun for_brain_never_includes_history() {
        val out = SendPolicy.forBrain("just this utterance")
        assertThat(out).isEqualTo("just this utterance")
        assertThat(out).doesNotContain("history")
    }

    @Test
    fun audio_must_leave_follows_ear_net() {
        assertThat(SendPolicy.audioMustLeave(earNeedsNet = true)).isTrue()
        assertThat(SendPolicy.audioMustLeave(earNeedsNet = false)).isFalse()
    }

    @Test
    fun for_ear_cloud_audio_leaves() {
        assertThat(SendPolicy.forEar(cloud = true).audioLeaves).isTrue()
        assertThat(SendPolicy.forEar(cloud = false).audioLeaves).isFalse()
    }
}
