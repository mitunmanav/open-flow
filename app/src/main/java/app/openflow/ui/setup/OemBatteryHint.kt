package app.openflow.ui.setup

import android.os.Build

/**
 * Wispr buries OEM kill in a support page. We show the right hint per device.
 * Pure logic for Truth tests — Build.* only in [hintForDevice].
 */
object OemBatteryHint {
    fun hint(manufacturerRaw: String?): String {
        val m = manufacturerRaw?.lowercase()?.trim() ?: ""
        return when {
            m.contains("samsung") ->
                "Samsung: Settings → Apps → Open Flow → Battery → Unrestricted. Skip keeps battery default."
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ->
                "Xiaomi: Settings → Apps → Open Flow → Battery → No restrictions, then enable Autostart and lock Open Flow in recents. Skip keeps battery default."
            m.contains("huawei") || m.contains("honor") ->
                "Huawei: Settings → Apps → Apps → Open Flow → Battery → Manage manually → enable all toggles. Skip keeps battery default."
            m.contains("oppo") || m.contains("realme") || m.contains("oneplus") ->
                "OnePlus / Oppo / Realme: Settings → Apps → Open Flow → Battery → Allow background + Autostart. Skip keeps battery default."
            m.contains("vivo") ->
                "Vivo: Settings → Apps → Open Flow → Battery → High background power + Autostart. Skip keeps battery default."
            m.isBlank() ->
                "Tip: Settings → Apps → Open Flow → Battery → Unrestricted. Some phones also need Autostart. Skip keeps battery default."
            else ->
                "Tip: Settings → Apps → Open Flow → Battery → Unrestricted. Some phones also need Autostart. Skip keeps battery default."
        }
    }

    fun hintForDevice(): String = hint(Build.MANUFACTURER)
}
