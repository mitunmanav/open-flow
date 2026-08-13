package app.openflow.bubble

import app.openflow.engine.BrainId
import app.openflow.engine.ProviderId

/** Lights from the picked brain id. none / on_phone stay rules-only. */
object BrainPick {

    fun rewrite(brainId: String): Boolean = when (ProviderId.parseBrain(brainId)) {
        BrainId.NONE, BrainId.ON_PHONE -> false
        else -> true
    }

    /** Same rule as rewrite. */
    fun command(brainId: String): Boolean = rewrite(brainId)
}
