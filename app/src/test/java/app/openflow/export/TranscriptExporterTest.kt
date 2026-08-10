package app.openflow.export

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TranscriptExporterTest {

    private val sample = ExportSession(
        id = "s1",
        title = "Budget talk",
        createdAtEpochMs = 1_700_000_000_000L,
        durationMs = 90_000L,
        transcript = "We need a bigger budget for marketing.",
        languageTag = "en-US"
    )

    @Test
    fun txt_contains_title_and_body() {
        val out = TranscriptExporter.toTxt(sample)
        assertThat(out).contains("Budget talk")
        assertThat(out).contains("We need a bigger budget for marketing.")
    }

    @Test
    fun md_has_heading_and_meta() {
        val out = TranscriptExporter.toMarkdown(sample)
        assertThat(out).startsWith("# Budget talk")
        assertThat(out).contains("language: en-US")
        assertThat(out).contains("We need a bigger budget")
    }

    @Test
    fun srt_has_index_and_timestamps() {
        val out = TranscriptExporter.toSrt(sample)
        assertThat(out).contains("1\n")
        assertThat(out).contains("-->")
        assertThat(out).contains("We need a bigger budget")
    }

    @Test
    fun json_is_object_with_fields() {
        val out = TranscriptExporter.toJson(sample)
        assertThat(out).contains("\"id\":\"s1\"")
        assertThat(out).contains("\"transcript\"")
        assertThat(out).contains("Budget talk")
    }
}
