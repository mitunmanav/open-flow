package app.openflow.orchestrate

data class RouteExplain(val providerId: String, val reason: String) {
    override fun toString(): String = "$providerId: $reason"
}
