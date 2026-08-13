package app.openflow.ai.providers.cloud

import app.openflow.engine.SendPolicy

/** One strip. Same as [SendPolicy.forBrain]. */
internal object CloudMinimize {
    fun forBrain(text: String): String = SendPolicy.forBrain(text)
}
