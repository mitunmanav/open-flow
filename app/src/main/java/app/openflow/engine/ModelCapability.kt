package app.openflow.engine

data class ModelCapability(
    val streamLive: Boolean,
    val languages: Set<String>,
    val rewrite: Boolean,
    val commandMode: Boolean,
    val audioLeavesDevice: Boolean,
    val needsNet: Boolean,
    val earPunct: Boolean = false,
) {
    companion object {
        fun systemEar() = ModelCapability(
            streamLive = true,
            languages = setOf("en-US"),
            rewrite = false,
            commandMode = false,
            audioLeavesDevice = true,
            needsNet = false,
            earPunct = true,
        )

        fun noneBrain() = ModelCapability(
            streamLive = false,
            languages = emptySet(),
            rewrite = false,
            commandMode = false,
            audioLeavesDevice = false,
            needsNet = false,
        )

        fun cloudBrain() = ModelCapability(
            streamLive = false,
            languages = emptySet(),
            rewrite = true,
            commandMode = true,
            audioLeavesDevice = true,
            needsNet = true,
        )

        fun onPhoneEar() = ModelCapability(
            streamLive = true,
            languages = emptySet(),
            rewrite = false,
            commandMode = false,
            audioLeavesDevice = false,
            needsNet = false,
        )

        fun laptop() = ModelCapability(
            streamLive = true,
            languages = emptySet(),
            rewrite = true,
            commandMode = true,
            audioLeavesDevice = true,
            needsNet = true,
        )
    }
}
