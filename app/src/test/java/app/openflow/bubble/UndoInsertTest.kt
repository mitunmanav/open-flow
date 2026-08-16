package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UndoInsertTest {

    @Test
    fun restore_returns_previous_field() {
        val snap = UndoInsert.Snapshot(
            previousField = "hello",
            insertedMerged = "hello world",
            raw = "um world",
            clean = "world",
        )
        assertThat(UndoInsert.canUndo(snap)).isTrue()
        assertThat(UndoInsert.restoredField(snap)).isEqualTo("hello")
    }

    @Test
    fun cannot_undo_null_or_blank_insert() {
        assertThat(UndoInsert.canUndo(null)).isFalse()
        assertThat(
            UndoInsert.canUndo(
                UndoInsert.Snapshot(
                    previousField = "x",
                    insertedMerged = "",
                    raw = "",
                    clean = "",
                )
            )
        ).isFalse()
    }

    @Test
    fun use_raw_merges_uncleaned_into_prefix() {
        val out = UndoInsert.useRawMerged("Hey,", "um meet at six")
        assertThat(out).contains("Hey")
        assertThat(out).contains("um meet at six")
    }

    @Test
    fun snapshot_after_insert_keeps_raw_and_previous() {
        val snap = UndoInsert.afterInsert(
            previousField = "Hi",
            merged = "Hi there",
            raw = "um there",
            clean = "there",
        )
        assertThat(snap).isNotNull()
        assertThat(snap!!.previousField).isEqualTo("Hi")
        assertThat(snap.raw).isEqualTo("um there")
        assertThat(snap.clean).isEqualTo("there")
    }

    @Test
    fun snapshot_null_when_merged_blank() {
        assertThat(
            UndoInsert.afterInsert(
                previousField = "",
                merged = "  ",
                raw = "x",
                clean = "x",
            )
        ).isNull()
    }
}
