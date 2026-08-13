package app.openflow.ui.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavStackTest {

    @Test
    fun bottom_tab_replaces_stack() {
        val s = NavStack.navigate(listOf(AppRoute.Home, AppRoute.Appearance), AppRoute.History)
        assertThat(s).containsExactly(AppRoute.History)
    }

    @Test
    fun push_settings_child() {
        val s = NavStack.navigate(listOf(AppRoute.Settings), AppRoute.Appearance)
        assertThat(s).containsExactly(AppRoute.Settings, AppRoute.Appearance).inOrder()
    }

    @Test
    fun back_pops_one() {
        val s = listOf(AppRoute.Settings, AppRoute.Appearance, AppRoute.BubbleSettings)
        assertThat(NavStack.canGoBack(s)).isTrue()
        assertThat(NavStack.goBack(s)).containsExactly(AppRoute.Settings, AppRoute.Appearance).inOrder()
    }

    @Test
    fun back_at_root_stays() {
        assertThat(NavStack.goBack(listOf(AppRoute.Home))).containsExactly(AppRoute.Home)
        assertThat(NavStack.canGoBack(listOf(AppRoute.Home))).isFalse()
    }

    @Test
    fun no_duplicate_push() {
        val s = NavStack.navigate(listOf(AppRoute.Settings, AppRoute.Appearance), AppRoute.Appearance)
        assertThat(s).containsExactly(AppRoute.Settings, AppRoute.Appearance).inOrder()
    }

    @Test
    fun initial_setup_when_not_ready() {
        assertThat(NavStack.initial(ready = false)).containsExactly(AppRoute.Setup)
        assertThat(NavStack.initial(ready = true)).containsExactly(AppRoute.Home)
    }
}
