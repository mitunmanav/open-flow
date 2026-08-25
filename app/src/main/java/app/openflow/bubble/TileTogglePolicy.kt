package app.openflow.bubble

/**
 * QS tile decision: dead service → launch setup; live service → flip the
 * hard-hide flag. Opacity is never touched (it belongs to the slider).
 */
object TileTogglePolicy {
    /** null = do not toggle (open app instead). */
    fun nextHidden(serviceAlive: Boolean, hidden: Boolean): Boolean? =
        if (!serviceAlive) null else !hidden
}
