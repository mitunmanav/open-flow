package app.openflow.bubble

/** Last successful insert: restore previous field, or re-merge raw. */
object UndoInsert {
    data class Snapshot(
        val previousField: String,
        val insertedMerged: String,
        val raw: String,
        val clean: String,
    )

    fun canUndo(snap: Snapshot?): Boolean =
        snap != null && snap.insertedMerged.isNotBlank()

    fun restoredField(snap: Snapshot): String = snap.previousField

    fun useRawMerged(prefix: String, raw: String): String =
        FieldPolicy.mergeSession(prefix, raw)

    fun afterInsert(
        previousField: String,
        merged: String,
        raw: String,
        clean: String,
    ): Snapshot? {
        if (merged.isBlank()) return null
        return Snapshot(
            previousField = previousField,
            insertedMerged = merged,
            raw = raw,
            clean = clean,
        )
    }
}
