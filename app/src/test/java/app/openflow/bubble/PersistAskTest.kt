package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PersistAskTest {

    @Test
    fun polished_not_blank_saves_even_if_insert_failed() {
        val a = PersistAsk.afterPolish(
            sessionId = "s",
            wasRetry = false,
            raw = "raw",
            clean = "clean",
            durationMs = 10L,
            languageTag = "en-IN",
            retentionPolicy = "keep",
            packageName = "com.x",
            createdAtEpochMs = 9L,
        )
        assertThat(a.kind).isEqualTo(PersistAsk.Kind.SAVE_OK)
        assertThat(a.rawText).isEqualTo("raw")
        assertThat(a.cleanText).isEqualTo("clean")
    }

    @Test
    fun retry_marks() {
        val a = PersistAsk.afterPolish(
            sessionId = "s", wasRetry = true, raw = "r", clean = "c",
            durationMs = 1L, languageTag = "en", retentionPolicy = "keep",
            packageName = "", createdAtEpochMs = 1L,
        )
        assertThat(a.kind).isEqualTo(PersistAsk.Kind.MARK_OK)
    }

    @Test
    fun blank_polish_skips() {
        val a = PersistAsk.afterPolish(
            sessionId = "s", wasRetry = false, raw = "r", clean = "  ",
            durationMs = 1L, languageTag = "en", retentionPolicy = "keep",
            packageName = "", createdAtEpochMs = 1L,
        )
        assertThat(a.kind).isEqualTo(PersistAsk.Kind.SKIP)
    }

    @Test
    fun fail_stop_saves_empty_failed() {
        val a = PersistAsk.afterFail(
            sessionId = "s", durationMs = 5L, languageTag = "en-IN",
            retentionPolicy = "keep", packageName = "p", createdAtEpochMs = 2L,
        )
        assertThat(a.kind).isEqualTo(PersistAsk.Kind.SAVE_FAILED)
        assertThat(a.rawText).isEmpty()
        assertThat(a.cleanText).isEmpty()
        assertThat(a.id).isEqualTo("s")
    }

    @Test
    fun fail_blank_id_skips() {
        val a = PersistAsk.afterFail(
            sessionId = "  ", durationMs = 1L, languageTag = "en",
            retentionPolicy = "keep", packageName = "p", createdAtEpochMs = 1L,
        )
        assertThat(a.kind).isEqualTo(PersistAsk.Kind.SKIP)
        assertThat(a.id).isEmpty()
    }
}
