package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppCategoryPromptTest {

    @Test
    fun every_category_has_a_nonblank_guideline() {
        for (c in AppCategory.entries) {
            assertThat(c.promptGuideline.isNotBlank()).isTrue()
        }
    }

    @Test
    fun search_guideline_strips_pleasantries() {
        assertThat(AppCategory.AI_SEARCH.promptGuideline).contains("search")
    }

    @Test
    fun dev_guideline_preserves_technical_tokens() {
        assertThat(AppCategory.DEV_TERMINAL.promptGuideline).contains("camelCase")
    }
}
