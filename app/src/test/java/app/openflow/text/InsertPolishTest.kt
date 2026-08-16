package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InsertPolishTest {

    @Test
    fun insert_never_requests_brain_rewrite() {
        assertThat(InsertPolish.brainRewriteOnInsert("openai")).isFalse()
        assertThat(InsertPolish.brainRewriteOnInsert("none")).isFalse()
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
    fun insert_brain_id_is_none_so_features_stay_local() {
        assertThat(InsertPolish.brainIdForInsert("openai")).isEqualTo("none")
        assertThat(InsertPolish.brainIdForInsert("none")).isEqualTo("none")
    }
}
