package app.openflow.ui.a11y

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * Hard product edges. Material Card/Surface clip children to [Shape];
 * a rectangle does not cut chips or labels at rounded corners.
 */
object OpenShapes {
    val Card: Shape = RectangleShape
    val Chip: Shape = RectangleShape
    val Button: Shape = RectangleShape
    val Field: Shape = RectangleShape
}
