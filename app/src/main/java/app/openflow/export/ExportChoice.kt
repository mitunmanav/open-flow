package app.openflow.export

enum class ExportFormat { MARKDOWN, PLAIN, JSON }

data class ExportChoice(
    val format: ExportFormat,
    val includeRaw: Boolean,
)
