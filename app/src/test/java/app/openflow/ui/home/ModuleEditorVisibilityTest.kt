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
        val locked = setOf("honesty")
        assertThat(ModuleEditorVisibility.showHideChip("honesty", locked)).isFalse()
        assertThat(ModuleEditorVisibility.showHideChip("note", locked)).isTrue()
        assertThat(ModuleEditorVisibility.showHideChip("search", emptySet())).isTrue()
    }
}
