package app.openflow.bubble

import app.openflow.text.Feature
import app.openflow.text.FeatureAuto

/** Lights from FeatureAuto. system ear — brain only. */
object BrainPick {

    fun rewrite(brainId: String): Boolean =
        Feature.HIGH_AI in FeatureAuto.of("system", brainId)

    fun command(brainId: String): Boolean =
        Feature.COMMAND in FeatureAuto.of("system", brainId)
}
