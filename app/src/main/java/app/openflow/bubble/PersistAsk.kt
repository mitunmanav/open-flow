package app.openflow.bubble

object PersistAsk {
    enum class Kind { SAVE_OK, MARK_OK, SAVE_FAILED, SKIP }

    data class Request(
        val kind: Kind,
        val id: String,
        val rawText: String,
        val cleanText: String,
        val durationMs: Long,
        val languageTag: String,
        val retentionPolicy: String,
        val packageName: String,
        val createdAtEpochMs: Long,
    )

    fun afterPolish(
        sessionId: String,
        wasRetry: Boolean,
        raw: String,
        clean: String,
        durationMs: Long,
        languageTag: String,
        retentionPolicy: String,
        packageName: String,
        createdAtEpochMs: Long,
    ): Request {
        if (clean.isBlank()) {
            return Request(
                kind = Kind.SKIP,
                id = sessionId,
                rawText = raw,
                cleanText = clean,
                durationMs = durationMs,
                languageTag = languageTag,
                retentionPolicy = retentionPolicy,
                packageName = packageName,
                createdAtEpochMs = createdAtEpochMs,
            )
        }
        return Request(
            kind = if (wasRetry) Kind.MARK_OK else Kind.SAVE_OK,
            id = sessionId,
            rawText = raw,
            cleanText = clean,
            durationMs = durationMs,
            languageTag = languageTag,
            retentionPolicy = retentionPolicy,
            packageName = packageName,
            createdAtEpochMs = createdAtEpochMs,
        )
    }

    fun afterFail(
        sessionId: String,
        durationMs: Long,
        languageTag: String,
        retentionPolicy: String,
        packageName: String,
        createdAtEpochMs: Long,
    ): Request {
        if (sessionId.isBlank()) {
            return Request(
                kind = Kind.SKIP,
                id = "",
                rawText = "",
                cleanText = "",
                durationMs = durationMs,
                languageTag = languageTag,
                retentionPolicy = retentionPolicy,
                packageName = packageName,
                createdAtEpochMs = createdAtEpochMs,
            )
        }
        return Request(
            kind = Kind.SAVE_FAILED,
            id = sessionId,
            rawText = "",
            cleanText = "",
            durationMs = durationMs,
            languageTag = languageTag,
            retentionPolicy = retentionPolicy,
            packageName = packageName,
            createdAtEpochMs = createdAtEpochMs,
        )
    }
}
