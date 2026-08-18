package app.openflow.ui.setup

object BatteryExemption {
    const val REQUEST = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    const val APP_DETAILS = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val ALL_APPS = "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"

    fun action(alreadyIgnoring: Boolean): String =
        if (alreadyIgnoring) APP_DETAILS else REQUEST

    fun dataUri(packageName: String): String = "package:$packageName"

    fun fallbackAction(): String = APP_DETAILS
}
