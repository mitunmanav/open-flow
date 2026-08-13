package app.openflow.prefs

import android.content.Context
import android.content.SharedPreferences
import app.openflow.stt.LanguagePolicy
import app.openflow.stt.SttTuning
import app.openflow.text.CapsMode
import app.openflow.text.CustomStyleConfig
import app.openflow.text.EndPunct
import app.openflow.text.WritingStyle
import app.openflow.ui.HapticFeel
import app.openflow.ui.theme.BubbleTint
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

    var bubbleX: Int
        get() = store.getString("bubble_x", "32").toIntOrNull() ?: 32
        set(v) = store.putString("bubble_x", v.toString())

    var bubbleY: Int
        get() = store.getString("bubble_y", "220").toIntOrNull() ?: 220
        set(v) = store.putString("bubble_y", v.toString())

    /** full | compact | dot — idle bubble size mode (F14) */
    var bubbleMode: String
        get() = normalizeBubbleMode(store.getString("bubble_mode", "full"))
        set(v) = store.putString("bubble_mode", normalizeBubbleMode(v))

    var languageTag: String
        get() = LanguagePolicy.force(
            store.getString("language_tag", SttTuning.DEFAULT_LANGUAGE)
        )
        set(v) = store.putString("language_tag", LanguagePolicy.force(v))

    /** formal | casual | very_casual | excited | custom */
    var styleName: String
        get() = WritingStyle.fromPref(store.getString("style", WritingStyle.CASUAL.name)).name
        set(v) = store.putString("style", WritingStyle.fromPref(v).name)

    /** Custom style: auto | period | bang | none */
    var customEndPunct: String
        get() = store.getString("custom_end_punct", "auto")
        set(v) = store.putString("custom_end_punct", v)

    /** Custom style: sentence | first | none */
    var customCaps: String
        get() = store.getString("custom_caps", "sentence")
        set(v) = store.putString("custom_caps", v)

    var customExpandInformal: Boolean
        get() = store.getString("custom_expand_informal", "false") == "true"
        set(v) = store.putString("custom_expand_informal", if (v) "true" else "false")

    /** Lines `from=>to` for custom style only. */
    var customStyleReplacements: String
        get() = store.getString("custom_style_replacements", "")
        set(v) = store.putString("custom_style_replacements", v)

    var snoozeUntilMs: Long
        get() = store.getLong("snooze_until", 0L)
        set(v) = store.putLong("snooze_until", v)

    /** First-run battery step shown or skipped (F22). */
    var setupBatterySeen: Boolean
        get() = store.getString("setup_battery_seen", "false") == "true"
        set(v) = store.putString("setup_battery_seen", if (v) "true" else "false")

    /** Home “How Open Flow works” card dismissed. */
    var seenHowTo: Boolean
        get() = store.getString("seen_how_to", "false") == "true"
        set(v) = store.putString("seen_how_to", if (v) "true" else "false")

    fun style(): WritingStyle = WritingStyle.fromPref(styleName)

    fun customStyleConfig(): CustomStyleConfig = CustomStyleConfig(
        endPunct = EndPunct.fromPref(customEndPunct),
        caps = CapsMode.fromPref(customCaps),
        expandInformal = customExpandInformal,
        replacements = CustomStyleConfig.parseReplacements(customStyleReplacements)
    )

    fun isSnoozed(now: Long = System.currentTimeMillis()): Boolean = now < snoozeUntilMs

    fun snoozeMinutes(minutes: Int = 10) {
        snoozeUntilMs = System.currentTimeMillis() + minutes * 60_000L
    }

    fun clearSnooze() {
        snoozeUntilMs = 0L
    }

    private val _darkMode = MutableStateFlow(normalizeDarkMode(store.getString("dark_mode", "light")))
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
        get() = normalizeBubbleShape(store.getString("bubble_shape", "square"))
        set(v) = store.putString("bubble_shape", normalizeBubbleShape(v))

    var bubbleHaptics: Boolean
        get() = store.getString("bubble_haptics", "true") == "true"
        set(v) = store.putString("bubble_haptics", if (v) "true" else "false")

    /** charcoal | cream | ink | stone */
    var bubbleTint: String
        get() = BubbleTint.normalize(store.getString("bubble_tint", BubbleTint.CHARCOAL))
        set(v) = store.putString("bubble_tint", BubbleTint.normalize(v))

    /** off | light | full — off maps [bubbleHaptics] false */
    var hapticFeel: String
        get() {
            val raw = store.getString("haptic_feel", "")
            return if (raw.isEmpty()) {
                if (bubbleHaptics) HapticFeel.FULL else HapticFeel.OFF
            } else {
                HapticFeel.normalize(raw)
            }
        }
        set(v) {
            val n = HapticFeel.normalize(v)
            store.putString("haptic_feel", n)
            bubbleHaptics = n != HapticFeel.OFF
        }

    /** Copy chip seconds: 3 | 6 | 10 */
    var copyChipSec: Int
        get() = normalizeCopyChipSec(store.getString("copy_chip_sec", "6").toIntOrNull() ?: 6)
        set(v) = store.putString("copy_chip_sec", normalizeCopyChipSec(v).toString())

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

    /** Last dictation session — raw STT (in-app copy; no auto-clipboard). */
    var lastSessionRaw: String
        get() = store.getString("last_session_raw", "")
        set(v) = store.putString("last_session_raw", v)

    /** Last dictation session — cleaned text after local pipeline. */
    var lastSessionClean: String
        get() = store.getString("last_session_clean", "")
        set(v) = store.putString("last_session_clean", v)

    /** Alias for call sites that use shorter names. */
    var lastRawText: String
        get() = lastSessionRaw
        set(v) {
            lastSessionRaw = v
        }

    /** Alias for call sites that use shorter names. */
    var lastCleanText: String
        get() = lastSessionClean
        set(v) {
            lastSessionClean = v
        }

    fun setLastSession(raw: String, clean: String) {
        lastSessionRaw = raw
        lastSessionClean = clean
    }

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

    /**
     * STT speed/accuracy profile: fast | balanced | accurate.
     * Applied when Accessibility service creates [app.openflow.stt.SttEngine].
     */
    var sttProfile: String
        get() = SttTuning.normalizeProfile(store.getString("stt_profile", SttTuning.PROFILE_BALANCED))
        set(v) = store.putString("stt_profile", SttTuning.normalizeProfile(v))

    fun sttTuning(): SttTuning = SttTuning.forProfile(sttProfile)

    /**
     * Preferred UI refresh Hz: 60 | 90 | 120 | 144.
     * Best-effort via display modes; device may clamp.
     */
    var refreshHz: Int
        get() = app.openflow.display.DisplayRefreshPolicy.normalizePreference(
            store.getString("refresh_hz", "120").toIntOrNull() ?: 120
        )
        set(v) = store.putString(
            "refresh_hz",
            app.openflow.display.DisplayRefreshPolicy.normalizePreference(v).toString()
        )

    companion object {
        const val PREFS_NAME = "openflow_prefs"

        /** Product default: light brutal (not soft M3). */
        fun defaultVisualSkinStorage(): String = VisualSkin.BRUTAL.storage

        fun normalizeDarkMode(value: String): String =
            when (value) {
                "dark", "light", "system" -> value
                else -> "light"
            }

        fun normalizeBubbleMode(value: String): String =
            when (value) {
                "full", "compact", "dot" -> value
                else -> "full"
            }

        fun normalizeBubbleShape(value: String): String =
            when (value) {
                "circle", "pill", "square", "dot" -> value
                else -> "square"
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

        fun normalizeCopyChipSec(sec: Int): Int = when (sec) {
            3, 10 -> sec
            else -> 6
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
