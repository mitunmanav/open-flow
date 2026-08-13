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
    fun back_from_dictionary_tab_goes_to_home() {
        assertThat(NavStack.canGoBack(listOf(AppRoute.Dictionary))).isTrue()
        assertThat(NavStack.goBack(listOf(AppRoute.Dictionary))).containsExactly(AppRoute.Home)
    }

    @Test
    fun back_from_home_stays() {
        assertThat(NavStack.canGoBack(listOf(AppRoute.Home))).isFalse()
        assertThat(NavStack.goBack(listOf(AppRoute.Home))).containsExactly(AppRoute.Home)
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

    @Test
    fun back_from_settings_tab_goes_to_home() {
        assertThat(NavStack.canGoBack(listOf(AppRoute.Settings))).isTrue()
        assertThat(NavStack.goBack(listOf(AppRoute.Settings))).containsExactly(AppRoute.Home)
    }

    @Test
    fun back_from_history_tab_goes_to_home() {
        assertThat(NavStack.canGoBack(listOf(AppRoute.History))).isTrue()
        assertThat(NavStack.goBack(listOf(AppRoute.History))).containsExactly(AppRoute.Home)
    }

    @Test
    fun settings_child_from_any_tab_sits_on_settings() {
        val s = NavStack.navigate(listOf(AppRoute.Home), AppRoute.Appearance)
        assertThat(s).containsExactly(AppRoute.Settings, AppRoute.Appearance).inOrder()
        assertThat(NavStack.goBack(s)).containsExactly(AppRoute.Settings)
    }

    @Test
    fun settings_child_toolbar_back_is_settings() {
        assertThat(AppRoute.Appearance.backTarget()).isEqualTo(AppRoute.Settings)
        assertThat(AppRoute.Privacy.backTarget()).isEqualTo(AppRoute.Settings)
        assertThat(AppRoute.Settings.backTarget()).isEqualTo(AppRoute.Settings)
        assertThat(AppRoute.Home.backTarget()).isEqualTo(AppRoute.Home)
        assertThat(AppRoute.Dictionary.backTarget()).isEqualTo(AppRoute.Dictionary)
    }

    @Test
    fun system_back_matches_navstack_current_after_goBack() {
        val stack = listOf(AppRoute.Settings, AppRoute.Appearance)
        val after = NavStack.goBack(stack)
        assertThat(NavStack.current(after)).isEqualTo(AppRoute.Appearance.backTarget())
        val tabs = listOf(AppRoute.Settings)
        assertThat(NavStack.current(NavStack.goBack(tabs))).isEqualTo(AppRoute.Home)
    }

    @Test
    fun openDeepLink_settings_child_sits_on_settings() {
        val s = NavStack.openDeepLink(AppRoute.Privacy)
        assertThat(s).containsExactly(AppRoute.Settings, AppRoute.Privacy).inOrder()
        assertThat(NavStack.goBack(s)).containsExactly(AppRoute.Settings)
    }

    @Test
    fun goBack_orphan_settings_child_goes_to_settings() {
        assertThat(NavStack.canGoBack(listOf(AppRoute.Appearance))).isTrue()
        assertThat(NavStack.goBack(listOf(AppRoute.Appearance))).containsExactly(AppRoute.Settings)
    }

    @Test
    fun setup_is_not_a_tab_and_has_no_back() {
        assertThat(AppRoute.Setup.isBottomBar()).isFalse()
        assertThat(NavStack.canGoBack(listOf(AppRoute.Setup))).isFalse()
        assertThat(NavStack.goBack(listOf(AppRoute.Setup))).containsExactly(AppRoute.Setup)
    }
}
