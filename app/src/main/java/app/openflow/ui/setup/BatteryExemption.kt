package app.openflow.ui.setup

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

object BatteryExemption {
    const val REQUEST = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    const val APP_DETAILS = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val ALL_APPS = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"

    fun action(alreadyIgnoring: Boolean): String =
        if (alreadyIgnoring) APP_DETAILS else REQUEST

    fun dataUri(packageName: String): String = "package:$packageName"

    fun fallbackAction(): String = APP_DETAILS
}

/**
 * Play restricted-permission justification: battery exemption is optional and
 * only keeps the bubble alive on aggressive OEMs. Shown before the system
 * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS screen.
 */
object BatteryExemptionPolicy {
    const val TITLE = "Battery optimization"
    const val BODY =
        "Some phones stop the bubble while it runs in the background. Battery " +
            "exemption keeps dictation ready — you can skip it anytime.\n\n" +
            "This is optional and uses no battery unless you are dictating."
    const val AGREE = "Allow — open setting"
    const val DECLINE = "Skip"

    fun copy() = Copy(TITLE, BODY, AGREE, DECLINE)

    data class Copy(val title: String, val body: String, val agree: String, val decline: String)
}

@Composable
fun BatteryExemptionDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
) {
    val c = BatteryExemptionPolicy.copy()
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
