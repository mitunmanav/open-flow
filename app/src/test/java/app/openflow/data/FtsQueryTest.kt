package app.openflow.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FtsQueryTest {

    @Test
    fun blank_returns_null_skip_match() {
        assertThat(FtsQuery.sanitize("")).isNull()
        assertThat(FtsQuery.sanitize("   ")).isNull()
    }

    @Test
    fun strips_fts_operators_keeps_words() {
        assertThat(FtsQuery.sanitize("hello* world")).isEqualTo("hello* world*")
        assertThat(FtsQuery.sanitize("a AND b")).isEqualTo("a* b*")
        assertThat(FtsQuery.sanitize("\"quote\" (paren)")).isEqualTo("quote* paren*")
    }

    @Test
    fun prefixes_each_token_for_prefix_match() {
        assertThat(FtsQuery.sanitize("mit")).isEqualTo("mit*")
        assertThat(FtsQuery.sanitize("open flow")).isEqualTo("open* flow*")
    }

    @Test
    fun only_noise_returns_null() {
        assertThat(FtsQuery.sanitize("***")).isNull()
        assertThat(FtsQuery.sanitize("\"\"")).isNull()
    }
}
