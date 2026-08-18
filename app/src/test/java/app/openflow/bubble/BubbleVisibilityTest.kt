package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleVisibilityTest {

    @Test
    fun ime_open_ignores_snooze() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = true,
                bankHide = false,
                hasEditable = true,
                imeVisible = true
            )
        ).isTrue()
    }

    @Test
    fun snooze_hides_when_ime_closed() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = true,
                bankHide = false,
                hasEditable = true,
                imeVisible = false
            )
        ).isFalse()
    }

    @Test
    fun hide_when_bank() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = false,
                bankHide = true,
                hasEditable = true,
                imeVisible = true
            )
        ).isFalse()
    }

    @Test
    fun hide_without_field() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = false,
                bankHide = false,
                hasEditable = false,
                imeVisible = true
            )
        ).isFalse()
    }

    @Test
    fun hide_without_ime_when_gated() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = false,
                bankHide = false,
                hasEditable = true,
                imeVisible = false
            )
        ).isFalse()
    }

    @Test
    fun show_with_field_and_ime() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = false,
                bankHide = false,
                hasEditable = true,
                imeVisible = true
            )
        ).isTrue()
    }

    @Test
    fun always_show_overrides_field() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = false,
                bankHide = false,
                hasEditable = false,
                imeVisible = false,
                alwaysShow = true
            )
        ).isTrue()
    }

    @Test
    fun hide_inside_own_app() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = false,
                bankHide = false,
                hasEditable = true,
                imeVisible = true,
                insideOwnApp = true
            )
        ).isFalse()
    }

    @Test
    fun listening_shows_inside_own_app() {
        assertThat(
            BubbleVisibility.shouldShow(
                snoozed = false,
                bankHide = false,
                hasEditable = false,
                imeVisible = false,
                listening = true,
                insideOwnApp = true
            )
        ).isTrue()
    }
}
