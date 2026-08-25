package app.openflow.stt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CloudFallbackNoticeTest {

    @Test
    fun cloud_ears_get_fallback_message() {
        for (id in listOf("sarvam", "deepgram", "openai", "assemblyai", "custom_stt")) {
            assertThat(CloudFallbackNotice.forFatal(id)).isNotNull()
        }
    }

    @Test
    fun local_ears_get_no_message() {
        for (id in listOf("system", "on_phone", "laptop", "", "garbage")) {
            assertThat(CloudFallbackNotice.forFatal(id)).isNull()
        }
    }

    @Test
    fun case_insensitive() {
        assertThat(CloudFallbackNotice.forFatal("SARVAM")).isNotNull()
    }

    @Test
    fun message_mentions_recovery_and_kept_text() {
        assertThat(CloudFallbackNotice.MESSAGE).contains("next listen")
        assertThat(CloudFallbackNotice.MESSAGE).contains("Kept")
    }
}
