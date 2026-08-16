package app.openflow.ui.home

/** Home layout Show/Hide chip — locked modules get no no-op toggle. */
object ModuleEditorVisibility {
    fun showHideChip(locked: Boolean): Boolean = !locked

    fun showHideChip(id: String, lockVisible: Set<String>): Boolean =
        showHideChip(locked = id in lockVisible)
}
