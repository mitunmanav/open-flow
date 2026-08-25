package app.openflow.ui.privacy

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Play Accessibility-API prominent disclosure.
 * Shown before the user is sent to system Accessibility settings.
 * Affirmative consent required — no silent enable path.
 */
object A11yDisclosurePolicy {
    const val TITLE = "Accessibility disclosure"
    const val BODY =
        "The Flow Bubble service reads the focused text field and the foreground app package name " +
            "so it can insert your dictation. Text changes near an insert are used to learn fixes " +
            "only if you turn Auto-learn on — it is off by default.\n\n" +
            "Audio is recorded only while you dictate. Phone speech may send audio to Google. " +
            "Open Flow has no server: history and learned fixes stay on this phone. " +
            "Your keyboard stays."
    const val AGREE = "Agree — Open Accessibility"
    const val DECLINE = "No thanks"

    data class Copy(val title: String, val body: String, val agree: String, val decline: String)

    fun copy(): Copy = Copy(TITLE, BODY, AGREE, DECLINE)

    /** Gate: show until the user accepts once. */
    fun shouldShow(accepted: Boolean): Boolean = !accepted
}

@Composable
fun A11yDisclosureDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    val c = A11yDisclosurePolicy.copy()
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(c.title) },
        text = { Text(c.body) },
        confirmButton = {
            TextButton(onClick = onAgree) { Text(c.agree) }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text(c.decline) }
        },
    )
}
