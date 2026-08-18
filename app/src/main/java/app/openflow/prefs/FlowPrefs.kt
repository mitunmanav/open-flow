package app.openflow.prefs

import android.content.Context
import android.content.SharedPreferences
import app.openflow.bubble.AppCategory
import app.openflow.bubble.AppOverride
import app.openflow.bubble.BubbleChrome
import app.openflow.bubble.BubbleLook
import app.openflow.bubble.BubbleScaleSteps
import app.openflow.stt.LanguagePolicy
import app.openflow.stt.SttTuning
import app.openflow.text.CapsMode
import app.openflow.text.CleanupLevel
import app.openflow.text.CustomStyleConfig
import app.openflow.text.EndPunct
import app.openflow.text.LearnEngine
import app.openflow.text.StyleCategory
import app.openflow.text.WritingStyle
import app.openflow.ui.HapticFeel
import app.openflow.ui.HapticPick
import app.openflow.ui.theme.AppearancePalette
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

    init {
        val raw = store.getString("learn_sides", "")
        if (raw.isNotEmpty()) LearnEngine.loadSides(raw)
        LearnEngine.loadPending(store.getString("learn_pending", ""))
        LearnEngine.persistHook = { encoded -> store.putString("learn_sides", encoded) }
        LearnEngine.pendingHook = { encoded -> store.putString("learn_pending", encoded) }
    }

    var bubbleScale: Float
        get() = store.getFloat("bubble_scale", 0.85f)
        set(v) = store.putFloat("bubble_scale", v)

    var bubbleOpacity: Float
        get() = store.getFloat("bubble_opacity", 0.80f).coerceIn(0.20f, 1.00f)
        set(v) = store.putFloat("bubble_opacity", v.coerceIn(0.20f, 1.00f))

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
        get() = LanguagePolicy.normalize(
            store.getString("language_tag", SttTuning.DEFAULT_LANGUAGE)
        )
        set(v) = store.putString("language_tag", LanguagePolicy.normalize(v))

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

    /** Home local scratch note (device-only). */
    var homeNote: String
        get() = store.getString(KEY_HOME_NOTE, "")
        set(v) = store.putString(KEY_HOME_NOTE, v)

    fun style(): WritingStyle = WritingStyle.fromPref(styleName)

    /** Typed cleanup for TEXT pipeline. none→RAW, light, medium→NORMAL, high. */
    fun cleanup(): CleanupLevel = CleanupLevel.fromPref(cleanupLevel)

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
        emitPalette()
    }

    var colorBg: String
        get() = store.getString("color_bg", "")
        set(v) {
            store.putString("color_bg", v)
            emitPalette()
        }
    var colorCards: String
        get() = store.getString("color_cards", "")
        set(v) {
            store.putString("color_cards", v)
            emitPalette()
        }
    var colorText: String
        get() = store.getString("color_text", "")
        set(v) {
            store.putString("color_text", v)
            emitPalette()
        }
    var colorAccent: String
        get() = store.getString("color_accent", "")
        set(v) {
            store.putString("color_accent", v)
            emitPalette()
        }
    var colorBorder: String
        get() = store.getString("color_border", "")
        set(v) {
            store.putString("color_border", v)
            emitPalette()
        }
    var colorBubbleIdle: String
        get() = store.getString("color_bubble_idle", "")
        set(v) {
            store.putString("color_bubble_idle", v)
            emitPalette()
        }
    var colorBubbleListen: String
        get() = store.getString("color_bubble_listen", "")
        set(v) {
            store.putString("color_bubble_listen", v)
            emitPalette()
        }
    var colorBubbleText: String
        get() = store.getString("color_bubble_text", "")
        set(v) {
            store.putString("color_bubble_text", v)
            emitPalette()
        }

    private val _appearance = MutableStateFlow(computePalette())
    val appearance: StateFlow<AppearancePalette> = _appearance.asStateFlow()

    fun palette(): AppearancePalette = computePalette()

    fun resetAppearanceColors() {
        store.putString("color_bg", "")
        store.putString("color_cards", "")
        store.putString("color_text", "")
        store.putString("color_accent", "")
        store.putString("color_border", "")
        store.putString("color_bubble_idle", "")
        store.putString("color_bubble_listen", "")
        store.putString("color_bubble_text", "")
        emitPalette()
    }

    private fun computePalette(): AppearancePalette {
        val overlay = AppearancePalette.overlay(
            dark = darkMode.value == "dark",
            bg = store.getString("color_bg", ""),
            cards = store.getString("color_cards", ""),
            text = store.getString("color_text", ""),
            accent = store.getString("color_accent", ""),
            border = store.getString("color_border", ""),
            bubbleIdle = store.getString("color_bubble_idle", ""),
            bubbleListen = store.getString("color_bubble_listen", ""),
            bubbleText = store.getString("color_bubble_text", ""),
        )
        val tint = BubbleTint.normalize(store.getString("bubble_tint", BubbleTint.CHARCOAL))
        return overlay.copy(
            bubbleIdleArgb = BubbleLook.fillArgb(store.getString("color_bubble_idle", ""), tint),
            bubbleListenArgb = BubbleLook.fillArgb(store.getString("color_bubble_listen", ""), tint),
            bubbleTextArgb = BubbleLook.onArgb(store.getString("color_bubble_text", ""), tint),
        )
    }

    private fun emitPalette() {
        _appearance.value = computePalette()
    }

    private val _visualSkin = MutableStateFlow(
        VisualSkin.fromStorage(store.getString("visual_skin", defaultVisualSkinStorage()))
    ).also {
        // Soft removed — persist healed brutal so Appearance never re-reads soft.
        store.putString("visual_skin", VisualSkin.BRUTAL.storage)
        it.value = VisualSkin.BRUTAL
    }
    val visualSkin: StateFlow<VisualSkin> = _visualSkin.asStateFlow()

    fun setVisualSkin(skin: VisualSkin) {
        // Soft skin removed — product is Brutal only.
        val forced = VisualSkin.BRUTAL
        _visualSkin.value = forced
        store.putString("visual_skin", forced.storage)
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

    /** circle | pill | square | dot — default pill (clean edge bar) */
    var bubbleShape: String
        get() = normalizeBubbleShape(store.getString("bubble_shape", "pill"))
        set(v) = store.putString("bubble_shape", normalizeBubbleShape(v))

    var bubbleShowCancel: Boolean
        get() = store.getString("bubble_show_cancel", "true") != "false"
        set(v) = store.putString("bubble_show_cancel", if (v) "true" else "false")

    var bubbleShowDone: Boolean
        get() = store.getString("bubble_show_done", "true") != "false"
        set(v) = store.putString("bubble_show_done", if (v) "true" else "false")

    var bubbleIconUri: String
        get() = store.getString("bubble_icon_uri", "")
        set(v) = store.putString("bubble_icon_uri", v)

    var bubbleRoundPct: Int
        get() {
            val raw = store.getString("bubble_round_pct", "")
            return if (raw.isNotEmpty()) {
                raw.toIntOrNull()?.coerceIn(0, 100) ?: 50
            } else {
                BubbleChrome.pctFromLegacy(bubbleRoundness)
            }
        }
        set(v) = store.putString("bubble_round_pct", v.coerceIn(0, 100).toString())

    var bubbleShrinkIdle: Boolean
        get() = store.getString("bubble_shrink_idle", "false") == "true"
        set(v) = store.putString("bubble_shrink_idle", if (v) "true" else "false")

    var bubbleShrinkDot: Boolean
        get() = store.getString("bubble_shrink_dot", "false") == "true"
        set(v) = store.putString("bubble_shrink_dot", if (v) "true" else "false")

    var bubbleShrinkSearch: Boolean
        get() = store.getString("bubble_shrink_search", "false") == "true"
        set(v) = store.putString("bubble_shrink_search", if (v) "true" else "false")

    fun resetBubbleScale() {
        bubbleScale = BubbleScaleSteps.DEFAULT
    }

    var bubbleHaptics: Boolean
        get() = store.getString("bubble_haptics", "true") == "true"
        set(v) = store.putString("bubble_haptics", if (v) "true" else "false")

    /** charcoal | cream | ink | stone | sky | forest | coral | grape */
    var bubbleTint: String
        get() = BubbleTint.normalize(store.getString("bubble_tint", BubbleTint.CHARCOAL))
        set(v) {
            store.putString("bubble_tint", BubbleTint.normalize(v))
            emitPalette()
        }

    /** hard | soft | round — default soft for clean pill */
    var bubbleRoundness: String
        get() = BubbleChrome.normalizeRoundness(store.getString("bubble_roundness", BubbleChrome.ROUND_SOFT))
        set(v) = store.putString("bubble_roundness", BubbleChrome.normalizeRoundness(v))

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

    fun hapticPick(event: HapticFeel.Event): String {
        val raw = store.getString(hapticKey(event), "")
        if (raw.isNotEmpty()) return HapticPick.normalize(raw)
        return when (hapticFeel) {
            HapticFeel.OFF -> HapticPick.OFF
            HapticFeel.LIGHT -> HapticPick.TICK
            else -> defaultHapticPick(event)
        }
    }

    fun setHapticPick(event: HapticFeel.Event, pick: String) {
        store.putString(hapticKey(event), HapticPick.normalize(pick))
    }

    fun resetHaptics() {
        HapticFeel.Event.entries.forEach { event ->
            store.putString(hapticKey(event), defaultHapticPick(event))
        }
    }

    private fun hapticKey(event: HapticFeel.Event): String = when (event) {
        HapticFeel.Event.TAP -> "haptic_tap"
        HapticFeel.Event.SAVE -> "haptic_save"
        HapticFeel.Event.CANCEL -> "haptic_cancel"
        HapticFeel.Event.ERROR -> "haptic_error"
        HapticFeel.Event.LISTEN -> "haptic_listen"
    }

    private fun defaultHapticPick(event: HapticFeel.Event): String = when (event) {
        HapticFeel.Event.TAP -> HapticPick.CLICK
        HapticFeel.Event.SAVE -> HapticPick.CONFIRM
        HapticFeel.Event.CANCEL -> HapticPick.REJECT
        HapticFeel.Event.ERROR -> HapticPick.REJECT
        HapticFeel.Event.LISTEN -> HapticPick.TICK
    }

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

    /** Remember word fixes after dictation (on-device). */
    var autoLearn: Boolean
        get() = store.getString("auto_learn", "true") == "true"
        set(v) = store.putString("auto_learn", if (v) "true" else "false")

    /**
     * Tiny learn map: `from=bag` auto, `from=*` manual.
     * No Room column. Hydrates [app.openflow.text.LearnEngine] on set.
     */
    var learnSides: String
        get() = store.getString("learn_sides", "")
        set(v) {
            store.putString("learn_sides", v)
            if (v.isNotEmpty()) LearnEngine.loadSides(v)
        }

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
     * Prefer [android.speech.SpeechRecognizer.createOnDeviceSpeechRecognizer] when the OS pack exists.
     * Default off — system recognizer may still send audio.
     * Live-read on each listen.
     */
    var preferOnDevice: Boolean
        get() = store.getString("prefer_on_device", "false") == "true"
        set(v) = store.putString("prefer_on_device", if (v) "true" else "false")

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

    var appContextEnabled: Boolean
        get() = store.getString("app_context_enabled", "true") == "true"
        set(v) = store.putString("app_context_enabled", if (v) "true" else "false")

    fun getCategoryStyle(category: AppCategory): WritingStyle {
        val raw = store.getString("cat_style_${category.name}", "")
        return if (raw.isNotEmpty()) WritingStyle.fromPref(raw) else category.defaultStyle
    }

    fun setCategoryStyle(category: AppCategory, style: WritingStyle) {
        store.putString("cat_style_${category.name}", style.name)
    }

    fun getCategoryPrompt(category: AppCategory): String {
        return store.getString("cat_prompt_${category.name}", "")
    }

    fun setCategoryPrompt(category: AppCategory, prompt: String) {
        store.putString("cat_prompt_${category.name}", prompt.trim())
    }

    fun getAppOverrides(): List<AppOverride> {
        val raw = store.getString("app_overrides", "")
        return parseAppOverrides(raw)
    }

    fun getAppOverride(packageName: String?): AppOverride? {
        val pkg = packageName.orEmpty().lowercase().trim()
        if (pkg.isEmpty()) return null
        return getAppOverrides().firstOrNull { it.packageName.lowercase().trim() == pkg }
    }

    fun saveAppOverride(override: AppOverride) {
        val current = getAppOverrides().filterNot {
            it.packageName.equals(override.packageName, ignoreCase = true)
        }.toMutableList()
        current.add(override)
        store.putString("app_overrides", encodeAppOverrides(current))
    }

    fun deleteAppOverride(packageName: String) {
        val current = getAppOverrides().filterNot {
            it.packageName.equals(packageName, ignoreCase = true)
        }
        store.putString("app_overrides", encodeAppOverrides(current))
    }

    /** Wispr-shaped Style hub: per-category WritingStyle. */
    fun getHubStyle(category: StyleCategory): WritingStyle {
        migrateLegacyAppContextIfNeeded()
        val raw = store.getString("hub_style_${category.name}", "")
        val style = if (raw.isNotEmpty()) WritingStyle.fromPref(raw) else category.defaultStyle
        return category.coerce(style)
    }

    fun setHubStyle(category: StyleCategory, style: WritingStyle) {
        store.putString("hub_style_${category.name}", category.coerce(style).name)
    }

    fun hubStylesMap(): Map<StyleCategory, WritingStyle> =
        StyleCategory.entries.associateWith { getHubStyle(it) }

    fun getStyleAppAssignments(): Map<String, StyleCategory> {
        migrateLegacyAppContextIfNeeded()
        val raw = store.getString("style_app_assignments", "")
        if (raw.isBlank()) return emptyMap()
        return raw.lines().mapNotNull { line ->
            val parts = line.split(";", limit = 2)
            if (parts.size < 2) return@mapNotNull null
            val pkg = parts[0].trim().lowercase()
            if (pkg.isEmpty()) return@mapNotNull null
            pkg to StyleCategory.fromName(parts[1])
        }.toMap()
    }

    fun setStyleAppAssignment(packageName: String, category: StyleCategory) {
        val pkg = packageName.trim().lowercase()
        if (pkg.isEmpty()) return
        val next = getStyleAppAssignments().toMutableMap()
        next[pkg] = category
        store.putString("style_app_assignments", encodeStyleAssignments(next))
    }

    fun removeStyleAppAssignment(packageName: String) {
        val pkg = packageName.trim().lowercase()
        val next = getStyleAppAssignments().toMutableMap()
        next.remove(pkg)
        store.putString("style_app_assignments", encodeStyleAssignments(next))
    }

    /**
     * One-shot: old 7-cat AppContext → 4-cat Style hub.
     * MESSAGING→PERSONAL, WORK_COLLAB→WORK, EMAIL→EMAIL, else OTHER.
     */
    fun migrateLegacyAppContextIfNeeded() {
        if (store.getString("style_hub_migrated", "") == "1") return
        val messaging = getCategoryStyle(AppCategory.MESSAGING)
        val work = getCategoryStyle(AppCategory.WORK_COLLAB)
        val email = getCategoryStyle(AppCategory.EMAIL)
        val other = getCategoryStyle(AppCategory.GENERAL)

        if (store.getString("hub_style_PERSONAL", "").isEmpty()) {
            store.putString("hub_style_PERSONAL", StyleCategory.PERSONAL.coerce(messaging).name)
        }
        if (store.getString("hub_style_WORK", "").isEmpty()) {
            store.putString("hub_style_WORK", StyleCategory.WORK.coerce(work).name)
        }
        if (store.getString("hub_style_EMAIL", "").isEmpty()) {
            store.putString("hub_style_EMAIL", StyleCategory.EMAIL.coerce(email).name)
        }
        if (store.getString("hub_style_OTHER", "").isEmpty()) {
            store.putString("hub_style_OTHER", StyleCategory.OTHER.coerce(other).name)
        }

        if (store.getString("style_app_assignments", "").isEmpty()) {
            val mapped = getAppOverrides().associate { ov ->
                val cat = when (ov.category) {
                    AppCategory.MESSAGING -> StyleCategory.PERSONAL
                    AppCategory.WORK_COLLAB -> StyleCategory.WORK
                    AppCategory.EMAIL -> StyleCategory.EMAIL
                    else -> StyleCategory.OTHER
                }
                ov.packageName.lowercase().trim() to cat
            }.filterKeys { it.isNotEmpty() }
            if (mapped.isNotEmpty()) {
                store.putString("style_app_assignments", encodeStyleAssignments(mapped))
            }
        }

        store.putString("style_hub_migrated", "1")
    }

    private fun encodeStyleAssignments(map: Map<String, StyleCategory>): String =
        map.entries.joinToString("\n") { (pkg, cat) ->
            "${pkg.replace(";", "_").replace("\n", " ")};${cat.name}"
        }

    companion object {
        const val PREFS_NAME = "openflow_prefs"
        private const val KEY_HOME_NOTE = "home_note"

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
                else -> "pill"
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

        fun encodeAppOverrides(list: List<AppOverride>): String {
            return list.joinToString("\n") { ov ->
                val safePkg = ov.packageName.replace(";", "_").replace("\n", " ").trim()
                val safeCat = ov.category.name
                val safeStyle = ov.style.name
                val safePrompt = ov.customPrompt.replace("\n", "\\n").replace(";", "\\;")
                "$safePkg;$safeCat;$safeStyle;$safePrompt"
            }
        }

        fun parseAppOverrides(raw: String): List<AppOverride> {
            if (raw.isBlank()) return emptyList()
            return raw.lines().mapNotNull { line ->
                val parts = line.split(";")
                if (parts.size >= 4) {
                    val pkg = parts[0].trim()
                    val cat = AppCategory.fromName(parts[1])
                    val style = WritingStyle.fromPref(parts[2])
                    val prompt = parts.drop(3).joinToString(";").replace("\\n", "\n").replace("\\;", ";")
                    if (pkg.isNotEmpty()) AppOverride(pkg, cat, style, prompt) else null
                } else null
            }
        }
    }
}

interface PrefsStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
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

    override fun getBoolean(key: String, default: Boolean): Boolean =
        sp.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        sp.edit().putBoolean(key, value).apply()
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

    override fun getBoolean(key: String, default: Boolean): Boolean =
        map[key] as? Boolean ?: default

    override fun putBoolean(key: String, value: Boolean) {
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
