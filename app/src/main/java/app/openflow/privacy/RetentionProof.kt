package app.openflow.privacy

object RetentionProof {
    data class Row(val id: String, val createdAt: Long)

    fun kept(rows: List<Row>, policy: String, now: Long): List<String> {
        if (!RetentionPolicy.shouldPersist(policy)) return emptyList()
        val cut = RetentionPolicy.cutoffEpochMs(now, policy) ?: return rows.map { it.id }
        return rows.filter { it.createdAt >= cut }.map { it.id }
    }
}
