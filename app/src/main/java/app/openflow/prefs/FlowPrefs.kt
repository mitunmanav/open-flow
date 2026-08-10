package app.openflow.prefs

import android.content.Context
import android.content.SharedPreferences
import app.openflow.stt.SttTuning
import app.openflow.text.TextPostProcessor
import app.openflow.ui.theme.VisualSkin
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

    /** full | compact | dot — idle bubble size mode (F14) */
    var bubbleMode: String
        get() = normalizeBubbleMode(store.getString("bubble_mode", "full"))
        set(v) = store.putString("bubble_mode", normalizeBubbleMode(v))

    var languageTag: String
        get() = store.getString(
            "language_tag",
            // English-first product focus; override in Bubble / Home settings
            SttTuning.DEFAULT_LANGUAGE
        )
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

    private val _visualSkin = MutableStateFlow(
        VisualSkin.fromStorage(store.getString("visual_skin", defaultVisualSkinStorage()))
    )
    val visualSkin: StateFlow<VisualSkin> = _visualSkin.asStateFlow()

    fun setVisualSkin(skin: VisualSkin) {
        _visualSkin.value = skin
        store.putString("visual_skin", skin.storage)
    }

    /** Drop 2: home module order + visibility encoding */
    var homeLayout: String
        get() = store.getString("home_layout", LayoutPrefs.DEFAULT_HOME)
        set(v) = store.putString("home_layout", v)

    /** Drawer extras visibility (history, customize). Settings always on. */
    var navLayout: String
        get() = store.getString("nav_layout", LayoutPrefs.DEFAULT_NAV)
        set(v) = store.putString("nav_layout", v)

    /** Listen pulse on bubble */
    var bubblePulse: Boolean
        get() = store.getString("bubble_pulse", "true") == "true"
        set(v) = store.putString("bubble_pulse", if (v) "true" else "false")

    /** Live speech text on bubble — default OFF (control chrome only). */
    var bubbleShowText: Boolean
        get() = store.getString("bubble_show_text", "false") == "true"
        set(v) = store.putString("bubble_show_text", if (v) "true" else "false")

    /** circle | pill | square | dot */
    var bubbleShape: String
        get() = normalizeBubbleShape(store.getString("bubble_shape", "circle"))
        set(v) = store.putString("bubble_shape", normalizeBubbleShape(v))

    var bubbleHaptics: Boolean
        get() = store.getString("bubble_haptics", "true") == "true"
        set(v) = store.putString("bubble_haptics", if (v) "true" else "false")

    var bubbleEdgeSnap: Boolean
        get() = store.getString("bubble_edge_snap", "true") == "true"
        set(v) = store.putString("bubble_edge_snap", if (v) "true" else "false")

    var bubbleSounds: Boolean
        get() = store.getString("bubble_sounds", "false") == "true"
        set(v) = store.putString("bubble_sounds", if (v) "true" else "false")

    /** none | light | medium | high */
    var cleanupLevel: String
        get() = normalizeCleanupLevel(store.getString("cleanup_level", "medium"))
        set(v) = store.putString("cleanup_level", normalizeCleanupLevel(v))

    /** keep | wipe_24h | never_store */
    var retentionPolicy: String
        get() = normalizeRetention(store.getString("retention", "keep"))
        set(v) = store.putString("retention", normalizeRetention(v))

    fun homeModules(): List<LayoutPrefs.Module> =
        LayoutPrefs.parseModules(homeLayout, LayoutPrefs.HOME_MODULES)

    fun setHomeModules(modules: List<LayoutPrefs.Module>) {
        homeLayout = LayoutPrefs.encodeModules(modules)
    }

    fun navModules(): List<LayoutPrefs.Module> =
        LayoutPrefs.parseModules(navLayout, LayoutPrefs.DRAWER_EXTRAS)

    fun setNavModules(modules: List<LayoutPrefs.Module>) {
        navLayout = LayoutPrefs.encodeModules(modules)
    }

    fun isDrawerItemVisible(routeName: String): Boolean =
        LayoutPrefs.isDrawerVisible(navLayout, routeName)

    companion object {
        const val PREFS_NAME = "openflow_prefs"

        /** Override in product-brutal flavor via subclass or build config later. */
        fun defaultVisualSkinStorage(): String = VisualSkin.M3.storage

        fun normalizeDarkMode(value: String): String =
            when (value) {
                "dark", "light", "system" -> value
                else -> "system"
            }

        fun normalizeBubbleMode(value: String): String =
            when (value) {
                "full", "compact", "dot" -> value
                else -> "full"
            }

        fun normalizeBubbleShape(value: String): String =
            when (value) {
                "circle", "pill", "square", "dot" -> value
                else -> "circle"
            }

        fun normalizeCleanupLevel(value: String): String =
            when (value.lowercase()) {
                "none", "light", "medium", "high" -> value.lowercase()
                else -> "medium"
            }

        fun normalizeRetention(value: String): String =
            when (value.lowercase()) {
                "keep", "wipe_24h", "never_store" -> value.lowercase()
                else -> "keep"
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
