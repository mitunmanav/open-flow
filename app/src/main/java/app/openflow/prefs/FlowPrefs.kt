package app.openflow.prefs

import android.content.Context
import android.content.SharedPreferences
import app.openflow.text.TextPostProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App prefs. Production uses SharedPreferences.
 * Tests inject [PrefsStore] — no Robolectric download.
 */
class FlowPrefs internal constructor(private val store: PrefsStore) {
    constructor(context: Context) : this(
        SharedPrefsStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    )

    var bubbleScale: Float
        get() = store.getFloat("bubble_scale", 0.85f)
        set(v) = store.putFloat("bubble_scale", v)

    var bubbleOpacity: Float
        get() = store.getFloat("bubble_opacity", 0.80f)
        set(v) = store.putFloat("bubble_opacity", v)

    var languageTag: String
        get() = store.getString("language_tag", java.util.Locale.getDefault().toLanguageTag())
        set(v) = store.putString("language_tag", v)

    var styleName: String
        get() = store.getString("style", TextPostProcessor.Style.CASUAL.name)
        set(v) = store.putString("style", v)

    var snoozeUntilMs: Long
        get() = store.getLong("snooze_until", 0L)
        set(v) = store.putLong("snooze_until", v)

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

    private val _darkMode = MutableStateFlow(normalizeDarkMode(store.getString("dark_mode", "system")))
    val darkMode: StateFlow<String> = _darkMode.asStateFlow()

    fun setDarkMode(value: String) {
        val v = normalizeDarkMode(value)
        _darkMode.value = v
        store.putString("dark_mode", v)
    }

    companion object {
        const val PREFS_NAME = "openflow_prefs"

        fun normalizeDarkMode(value: String): String =
            when (value) {
                "dark", "light", "system" -> value
                else -> "system"
            }
    }
}

interface PrefsStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getFloat(key: String, default: Float): Float
    fun putFloat(key: String, value: Float)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
}

class SharedPrefsStore(private val sp: SharedPreferences) : PrefsStore {
    override fun getString(key: String, default: String): String =
        sp.getString(key, default) ?: default

    override fun putString(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }

    override fun getFloat(key: String, default: Float): Float = sp.getFloat(key, default)

    override fun putFloat(key: String, value: Float) {
        sp.edit().putFloat(key, value).apply()
    }

    override fun getLong(key: String, default: Long): Long = sp.getLong(key, default)

    override fun putLong(key: String, value: Long) {
        sp.edit().putLong(key, value).apply()
    }
}

/** In-memory store for JVM unit tests (no Android runtime). */
class MemoryPrefsStore : PrefsStore {
    private val map = mutableMapOf<String, Any>()

    override fun getString(key: String, default: String): String =
        map[key] as? String ?: default

    override fun putString(key: String, value: String) {
        map[key] = value
    }

    override fun getFloat(key: String, default: Float): Float =
        map[key] as? Float ?: default

    override fun putFloat(key: String, value: Float) {
        map[key] = value
    }

    override fun getLong(key: String, default: Long): Long =
        map[key] as? Long ?: default

    override fun putLong(key: String, value: Long) {
        map[key] = value
    }
}
