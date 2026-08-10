package app.openflow.prefs

import android.content.Context
import app.openflow.text.TextPostProcessor

class FlowPrefs(context: Context) {
    private val sp = context.getSharedPreferences("openflow_prefs", Context.MODE_PRIVATE)

    var bubbleScale: Float
        get() = sp.getFloat("bubble_scale", 0.85f)
        set(v) = sp.edit().putFloat("bubble_scale", v).apply()

    var bubbleOpacity: Float
        get() = sp.getFloat("bubble_opacity", 0.80f)
        set(v) = sp.edit().putFloat("bubble_opacity", v).apply()

    var languageTag: String
        get() = sp.getString("language_tag", java.util.Locale.getDefault().toLanguageTag())!!
        set(v) = sp.edit().putString("language_tag", v).apply()

    var styleName: String
        get() = sp.getString("style", TextPostProcessor.Style.CASUAL.name)!!
        set(v) = sp.edit().putString("style", v).apply()

    var snoozeUntilMs: Long
        get() = sp.getLong("snooze_until", 0L)
        set(v) = sp.edit().putLong("snooze_until", v).apply()

    fun style(): TextPostProcessor.Style =
        runCatching { TextPostProcessor.Style.valueOf(styleName) }
            .getOrDefault(TextPostProcessor.Style.CASUAL)

    fun isSnoozed(now: Long = System.currentTimeMillis()): Boolean = now < snoozeUntilMs

    fun snoozeMinutes(minutes: Int = 10) {
        snoozeUntilMs = System.currentTimeMillis() + minutes * 60_000L
    }

    fun clearSnooze() {
        snoozeUntilMs = 0L
    }
}
