package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AtomicTokensTest {

    @Test
    fun protect_restore_url_is_byte_exact() {
        val raw = "see https://example.com/x?a=1 now"
        val p = AtomicTokens.protect(raw)
        assertThat(p.text).doesNotContain("example.com")
        assertThat(AtomicTokens.restore(p.text, p.spans)).isEqualTo(raw)
    }

    @Test
    fun protect_restore_email_is_byte_exact() {
        val raw = "mail jane.doe+tag@corp.co.uk please"
        val p = AtomicTokens.protect(raw)
        assertThat(p.text).doesNotContain("corp.co.uk")
        assertThat(AtomicTokens.restore(p.text, p.spans)).isEqualTo(raw)
    }

    @Test
    fun protect_restore_path_is_byte_exact() {
        val raw = "hit the API endpoint at /v1/users next"
        val p = AtomicTokens.protect(raw)
        assertThat(p.text).doesNotContain("/v1/users")
        assertThat(AtomicTokens.restore(p.text, p.spans)).isEqualTo(raw)
    }

    @Test
    fun rewrite_around_sentinel_leaves_url_intact() {
        val raw = "see https://example.com/x?a=1 now"
        val p = AtomicTokens.protect(raw)
        val smashed = p.text.replace("see", "SEE").replace("now", "NOW")
        assertThat(AtomicTokens.restore(smashed, p.spans))
            .isEqualTo("SEE https://example.com/x?a=1 NOW")
    }

    @Test
    fun empty_and_plain_text_are_identity() {
        assertThat(AtomicTokens.protect("").text).isEmpty()
        val plain = AtomicTokens.protect("hello world")
        assertThat(plain.spans).isEmpty()
        assertThat(AtomicTokens.restore(plain.text, plain.spans)).isEqualTo("hello world")
    }
}
