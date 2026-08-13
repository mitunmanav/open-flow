package app.openflow.secrets

import android.content.Context
import app.openflow.prefs.PrefsStore
import app.openflow.prefs.SharedPrefsStore

/** API keys. Never store these in [app.openflow.prefs.FlowPrefs]. */
interface SecretStore {
    fun put(id: String, key: String)
    fun get(id: String): String?
    fun clear(id: String)
}

/** In-memory store for JVM unit tests. */
class MemorySecretStore : SecretStore {
    private val map = mutableMapOf<String, String>()

    override fun put(id: String, key: String) {
        if (key.isEmpty()) map.remove(id) else map[id] = key
    }

    override fun get(id: String): String? = map[id]

    override fun clear(id: String) {
        map.remove(id)
    }
}

/** Prefs-backed store. Production file is [AndroidSecretStore.PREFS_NAME], not FlowPrefs. */
class PrefsSecretStore(private val store: PrefsStore) : SecretStore {
    override fun put(id: String, key: String) {
        store.putString(id, key)
    }

    override fun get(id: String): String? =
        store.getString(id, "").ifEmpty { null }

    override fun clear(id: String) {
        store.putString(id, "")
    }
}

/** Device store. Separate file; backup already off on the app. */
class AndroidSecretStore(context: Context) : SecretStore by PrefsSecretStore(
    SharedPrefsStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
) {
    companion object {
        const val PREFS_NAME = "openflow_secrets"
    }
}
