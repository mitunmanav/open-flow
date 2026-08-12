package app.openflow.export

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HistoryExportTest {

    private val rows = listOf(
        HistoryExport.Row(
            createdAtEpochMs = 1_700_000_000_000L,
            text = "Hello world",
            languageTag = "en-US",
            wordCount = 2,
        ),
        HistoryExport.Row(
            createdAtEpochMs = 1_700_000_100_000L,
            text = "Second note",
            languageTag = "en-US",
            wordCount = 2,
        ),
    )

    @Test
    fun markdown_has_title_and_items() {
        val md = HistoryExport.toMarkdown(rows)
        assertThat(md).contains("# Open Flow history")
        assertThat(md).contains("Hello world")
        assertThat(md).contains("Second note")
        assertThat(md).contains("-")
    }

    @Test
    fun plain_text_one_block_per_row() {
        val txt = HistoryExport.toPlainText(rows)
        assertThat(txt).contains("Hello world")
        assertThat(txt).contains("Second note")
        assertThat(txt).contains("---")
    }

    @Test
    fun empty_list_markdown_minimal() {
        val md = HistoryExport.toMarkdown(emptyList())
        assertThat(md.trim()).isEqualTo("# Open Flow history")
    }

    @Test
    fun empty_list_plain_empty() {
        assertThat(HistoryExport.toPlainText(emptyList())).isEmpty()
    }

    @Test
    fun non_english_tag_forced_to_en_us_in_output() {
        val md = HistoryExport.toMarkdown(
            listOf(
                HistoryExport.Row(
                    createdAtEpochMs = 1L,
                    text = "Hi",
                    languageTag = "fr-FR",
                    wordCount = 1,
                )
            )
        )
        assertThat(md).contains("en-US")
        assertThat(md).doesNotContain("fr-FR")
    }
}
