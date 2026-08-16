package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InsertPolishTest {

    @Test
    fun insert_requests_brain_rewrite_when_brain_picked() {
        assertThat(InsertPolish.brainRewriteOnInsert("openai")).isTrue()
        assertThat(InsertPolish.brainRewriteOnInsert("anthropic")).isTrue()
        assertThat(InsertPolish.brainRewriteOnInsert("laptop")).isTrue()
        assertThat(InsertPolish.brainRewriteOnInsert("custom")).isTrue()
        assertThat(InsertPolish.brainRewriteOnInsert("GROK")).isTrue()
    }

    @Test
    fun insert_skips_rewrite_for_none_and_on_phone_stub() {
        assertThat(InsertPolish.brainRewriteOnInsert("none")).isFalse()
        assertThat(InsertPolish.brainRewriteOnInsert("on_phone")).isFalse()
        assertThat(InsertPolish.brainRewriteOnInsert("")).isFalse()
        assertThat(InsertPolish.brainRewriteOnInsert("   ")).isFalse()
    }

    @Test
    fun insert_uses_pref_cleanup_not_high() {
        assertThat(InsertPolish.level("medium")).isEqualTo(CleanupLevel.NORMAL)
        assertThat(InsertPolish.level("light")).isEqualTo(CleanupLevel.LIGHT)
        assertThat(InsertPolish.level("none")).isEqualTo(CleanupLevel.RAW)
        assertThat(InsertPolish.level("high")).isEqualTo(CleanupLevel.HIGH)
    }

    @Test
    fun insert_language_keeps_catalog() {
        assertThat(InsertPolish.language("hi-IN")).isEqualTo("hi-IN")
        assertThat(InsertPolish.language("en-IN")).isEqualTo("en-IN")
        assertThat(InsertPolish.language(null)).isEqualTo("en-US")
    }

    @Test
    fun insert_brain_id_passes_through_rewrite_brains() {
        assertThat(InsertPolish.brainIdForInsert("openai")).isEqualTo("openai")
        assertThat(InsertPolish.brainIdForInsert("Anthropic")).isEqualTo("anthropic")
        assertThat(InsertPolish.brainIdForInsert("none")).isEqualTo("none")
        assertThat(InsertPolish.brainIdForInsert("on_phone")).isEqualTo("none")
    }
}
