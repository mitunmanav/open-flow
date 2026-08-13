package app.openflow.ui.a11y

import androidx.compose.ui.graphics.RectangleShape
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OpenShapesTest {

    @Test
    fun card_is_rectangle_no_round_clip() {
        assertThat(OpenShapes.Card).isEqualTo(RectangleShape)
    }

    @Test
    fun chip_button_field_are_rectangle() {
        assertThat(OpenShapes.Chip).isEqualTo(RectangleShape)
        assertThat(OpenShapes.Button).isEqualTo(RectangleShape)
        assertThat(OpenShapes.Field).isEqualTo(RectangleShape)
    }
}
