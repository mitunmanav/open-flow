package app.openflow.ui.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModuleEditorVisibilityTest {

    @Test
    fun lockedModule_hidesShowHideChip() {
        assertThat(ModuleEditorVisibility.showHideChip(locked = true)).isFalse()
        assertThat(ModuleEditorVisibility.showHideChip(locked = false)).isTrue()
    }

    @Test
    fun lockVisibleIds_hideToggleForThoseOnly() {
        val locked = setOf("setup")
        assertThat(ModuleEditorVisibility.showHideChip("setup", locked)).isFalse()
        assertThat(ModuleEditorVisibility.showHideChip("test", locked)).isTrue()
        assertThat(ModuleEditorVisibility.showHideChip("keys", emptySet())).isTrue()
    }
}
