package app.openflow.orchestrate

data class RouteSignals(
    val online: Boolean,
    val keyedEars: Set<String>,
    val keyedBrains: Set<String>,
    val preferOnDevice: Boolean = false,
)
