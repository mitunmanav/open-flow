package app.openflow.ui.engine

import app.openflow.engine.EarGate

/**
 * Which ear/brain presets appear in Speech+AI menus.
 * Dead stubs are hidden — never shown as fake "(soon)" picks.
 */
object EnginePickerVisibility {
    fun showEar(id: String): Boolean = EarGate.live(id)

    fun showBrain(id: String, url: String): Boolean =
        EnginePickerState.brainEnabled(id, url)

    fun visibleEars(): List<EnginePreset> =
        EnginePickerState.ears.filter { showEar(it.id) }

    fun visibleBrains(url: String): List<EnginePreset> =
        EnginePickerState.brains.filter { showBrain(it.id, url) }

    fun visibleEarSections(): List<EngineSection> =
        EnginePickerState.earSections().mapNotNull { sec ->
            val items = sec.items.filter { showEar(it.id) }
            if (items.isEmpty()) null else sec.copy(items = items)
        }

    fun visibleBrainSections(url: String): List<EngineSection> =
        EnginePickerState.brainSections().mapNotNull { sec ->
            val items = sec.items.filter { showBrain(it.id, url) }
            if (items.isEmpty()) null else sec.copy(items = items)
        }
}
