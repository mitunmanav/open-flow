package app.openflow.ui.a11y

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon + stable contentDescription pair for a11y. */
data class LabeledIcon(
    val image: ImageVector,
    val contentDescription: String
)

object OpenIcons {
    val Home = Icons.Default.Home
    val Book = Icons.Default.Book
    val ShortText = Icons.AutoMirrored.Filled.ShortText
    val Style = Icons.Default.Style
    val Insights = Icons.Default.Insights
    val Settings = Icons.Default.Settings
    val ContentCopy = Icons.Default.ContentCopy
    val Delete = Icons.Default.Delete
    val Share = Icons.Default.Share

    const val HomeDesc = "Home tab"
    const val BookDesc = "Dictionary tab"
    const val ShortTextDesc = "Snippets tab"
    const val StyleDesc = "Style tab"
    const val InsightsDesc = "Insights tab"
    const val SettingsDesc = "Settings"
    const val CopyDesc = "Copy to clipboard"
    const val DeleteDesc = "Delete item"
    const val ShareDesc = "Share"

    val HomeLabeled = LabeledIcon(Home, HomeDesc)
    val BookLabeled = LabeledIcon(Book, BookDesc)
    val ShortTextLabeled = LabeledIcon(ShortText, ShortTextDesc)
    val StyleLabeled = LabeledIcon(Style, StyleDesc)
    val InsightsLabeled = LabeledIcon(Insights, InsightsDesc)
    val SettingsLabeled = LabeledIcon(Settings, SettingsDesc)
    val CopyLabeled = LabeledIcon(ContentCopy, CopyDesc)
    val DeleteLabeled = LabeledIcon(Delete, DeleteDesc)
    val ShareLabeled = LabeledIcon(Share, ShareDesc)

    /** Resolve description for a known OpenIcons vector; null if unknown. */
    fun contentDescription(icon: ImageVector): String? = when (icon) {
        Home -> HomeDesc
        Book -> BookDesc
        ShortText -> ShortTextDesc
        Style -> StyleDesc
        Insights -> InsightsDesc
        Settings -> SettingsDesc
        ContentCopy -> CopyDesc
        Delete -> DeleteDesc
        Share -> ShareDesc
        else -> null
    }

    fun labeled(icon: ImageVector, fallback: String = ""): LabeledIcon {
        val desc = contentDescription(icon) ?: fallback
        return LabeledIcon(icon, desc)
    }
}
