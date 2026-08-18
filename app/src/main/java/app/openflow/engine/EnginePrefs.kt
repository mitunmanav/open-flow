package app.openflow.engine

import android.content.Context
import app.openflow.orchestrate.AiWhen
import app.openflow.orchestrate.RouteMode
import app.openflow.prefs.PrefsStore
import app.openflow.prefs.SharedPrefsStore

/**
 * Selected ear/brain ids and non-secret settings.
 * Keys live in a separate prefs file — never [app.openflow.prefs.FlowPrefs].
 */
class EnginePrefs internal constructor(private val store: PrefsStore) {
    constructor(context: Context) : this(
        SharedPrefsStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    )

    var earId: String
        get() = normalizeId(store.getString(KEY_EAR, DEFAULT_EAR), DEFAULT_EAR)
        set(v) = store.putString(KEY_EAR, normalizeId(v, DEFAULT_EAR))

    var brainId: String
        get() = normalizeId(store.getString(KEY_BRAIN, DEFAULT_BRAIN), DEFAULT_BRAIN)
        set(v) = store.putString(KEY_BRAIN, normalizeId(v, DEFAULT_BRAIN))

    var brainModel: String
        get() = store.getString(KEY_BRAIN_MODEL, "")
        set(v) = store.putString(KEY_BRAIN_MODEL, v)

    var earModel: String
        get() = store.getString(KEY_EAR_MODEL, "")
        set(v) = store.putString(KEY_EAR_MODEL, v)

    var customBaseUrl: String
        get() = store.getString(KEY_CUSTOM_URL, "")
        set(v) = store.putString(KEY_CUSTOM_URL, v)

    var sarvamMode: String
        get() = normalizeSarvam(store.getString(KEY_SARVAM, DEFAULT_SARVAM))
        set(v) = store.putString(KEY_SARVAM, normalizeSarvam(v))

    var autoRoute: Boolean
        get() = routeMode != RouteMode.LOCAL_ONLY
        set(v) {
            routeMode = if (v) RouteMode.LOCAL_THEN_AI else RouteMode.LOCAL_ONLY
        }

    var routeMode: RouteMode
        get() {
            val raw = store.getString(KEY_ROUTE, "")
            if (raw.isNotEmpty()) return RouteMode.fromPref(raw)
            val migrated = RouteMode.fromLegacy(
                store.getBoolean(KEY_AUTO, true),
                brainId,
            )
            store.putString(KEY_ROUTE, migrated.pref)
            return migrated
        }
        set(v) = store.putString(KEY_ROUTE, v.pref)

    var aiWhen: AiWhen
        get() = AiWhen.fromPref(store.getString(KEY_AI_WHEN, AiWhen.EVERY.pref))
        set(v) = store.putString(KEY_AI_WHEN, v.pref)

    companion object {
        const val PREFS_NAME = "openflow_engine"
        const val DEFAULT_EAR = "system"
        const val DEFAULT_BRAIN = "none"
        const val DEFAULT_SARVAM = "transcribe"

        private const val KEY_EAR = "ear_id"
        private const val KEY_BRAIN = "brain_id"
        private const val KEY_BRAIN_MODEL = "brain_model"
        private const val KEY_EAR_MODEL = "ear_model"
        private const val KEY_CUSTOM_URL = "custom_base_url"
        private const val KEY_SARVAM = "sarvam_mode"
        private const val KEY_AUTO = "auto_route"
        private const val KEY_ROUTE = "route_mode"
        private const val KEY_AI_WHEN = "ai_when"

        fun normalizeSarvam(value: String): String =
            when (value.lowercase()) {
                "transcribe", "translate", "verbatim", "translit", "codemix" -> value.lowercase()
                // Old prefs from pre-v3 labels
                "mix" -> "codemix"
                "roman" -> "translit"
                else -> DEFAULT_SARVAM
            }

        private fun normalizeId(value: String, default: String): String =
            value.trim().ifEmpty { default }
    }
}
