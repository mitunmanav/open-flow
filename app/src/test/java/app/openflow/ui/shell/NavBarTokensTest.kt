package app.openflow.ui.shell

import app.openflow.ui.a11y.Dimen
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavBarTokensTest {

    @Test
    fun tab_min_is_material_touch() {
        assertThat(NavBarTokens.tabMin).isEqualTo(Dimen.MIN_TOUCH)
        assertThat(NavBarTokens.iconMin).isEqualTo(Dimen.TOUCH_TARGET)
    }

    @Test
    fun selected_pad_is_tight_for_five_tabs() {
        assertThat(NavBarTokens.selectedPadH).isEqualTo(Dimen.Space2)
        assertThat(NavBarTokens.selectedPadV).isEqualTo(Dimen.Space1)
        assertThat(NavBarTokens.tabPadV).isEqualTo(Dimen.Space1)
    }

    @Test
    fun short_labels_stay_short() {
        assertThat(NavBarTokens.shortLabel(AppRoute.Home)).isEqualTo("Home")
        assertThat(NavBarTokens.shortLabel(AppRoute.Dictionary)).isEqualTo("Dict")
        assertThat(NavBarTokens.shortLabel(AppRoute.Snippets)).isEqualTo("Snips")
        assertThat(NavBarTokens.shortLabel(AppRoute.Style)).isEqualTo("Style")
        assertThat(NavBarTokens.shortLabel(AppRoute.Insights)).isEqualTo("Stats")
    }

    @Test
    fun every_bottom_tab_has_a_short_label() {
        BottomBarRoutes.forEach { route ->
            assertThat(NavBarTokens.shortLabel(route).length).isAtMost(6)
        }
    }
}
