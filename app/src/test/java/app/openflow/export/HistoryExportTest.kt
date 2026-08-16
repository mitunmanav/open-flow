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
            rawText = "hello world"
        ),
        HistoryExport.Row(
            createdAtEpochMs = 1_700_000_100_000L,
            text = "Second note",
            languageTag = "en-US",
            wordCount = 2,
            rawText = "second note actually"
        ),
    )

    @Test
    fun markdown_has_title_and_items() {
        val md = HistoryExport.toMarkdown(rows)
        assertThat(md).contains("# Open Flow history")
        assertThat(md).contains("Hello world")
        assertThat(md).contains("Second note")
    }

    @Test
    fun markdown_with_raw_includes_diff() {
        val md = HistoryExport.toMarkdown(rows, includeRaw = true)
        assertThat(md).contains("> *Raw STT:*")
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
        assertThat(md).doesNotContain("###")
        assertThat(md).doesNotContain("words")
    }

    @Test
    fun empty_list_plain_empty() {
        assertThat(HistoryExport.toPlainText(emptyList())).isEmpty()
    }

    @Test
    fun empty_list_share_empty_no_fake_rows() {
        val share = HistoryExport.shareText(emptyList())
        assertThat(share).isEmpty()
        assertThat(share).doesNotContain("Hello")
        assertThat(HistoryExport.filterRows(emptyList(), "x")).isEmpty()
    }

    @Test
    fun share_blankText_fallsBackToRawText() {
        val share = HistoryExport.shareText(
            listOf(
                HistoryExport.Row(
                    createdAtEpochMs = 1L,
                    text = " ",
                    rawText = "raw fallback",
                )
            )
        )

        assertThat(share).contains("raw fallback")
    }

    @Test
    fun markdown_blankText_fallsBackToRawText() {
        val md = HistoryExport.toMarkdown(
            listOf(
                HistoryExport.Row(
                    createdAtEpochMs = 1L,
                    text = "",
                    rawText = "raw fallback",
                )
            )
        )

        assertThat(md).contains("raw fallback")
        assertThat(md).doesNotContain("> *Raw STT:*")
    }

    @Test
    fun exports_all_today_fields() {
        val row = HistoryExport.Row(
            createdAtEpochMs = 1_700_000_000_000L,
            text = "Hello world",
            languageTag = "en-US",
            wordCount = 2,
            rawText = "hello world",
            id = "abc-1",
            durationMs = 1500L,
        )
        val md = HistoryExport.toMarkdown(listOf(row), includeRaw = true)
        assertThat(md).contains("abc-1")
        assertThat(md).contains("1500")
        assertThat(md).contains("Hello world")
        assertThat(md).contains("hello world")
        assertThat(md).contains("en-US")
        assertThat(md).contains("2 words")
        val txt = HistoryExport.toPlainText(listOf(row))
        assertThat(txt).contains("abc-1")
        assertThat(txt).contains("1500")
        assertThat(txt).contains("Hello world")
    }

    @Test
    fun filter_returns_matches() {
        val matches = HistoryExport.filterRows(rows, "second")
        assertThat(matches).hasSize(1)
        assertThat(matches.first().text).isEqualTo("Second note")
    }

    @Test
    fun filter_empty_returns_all() {
        val matches = HistoryExport.filterRows(rows, "")
        assertThat(matches).hasSize(2)
    }

    @Test
    fun catalog_lang_kept_in_export() {
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
        assertThat(md).contains("fr-FR")
        assertThat(md).doesNotContain("(en-US,")
    }
}
