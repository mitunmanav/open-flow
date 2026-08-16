package app.openflow.secrets

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.openflow.prefs.PrefsStore
import app.openflow.prefs.SharedPrefsStore
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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

/** AES-GCM wrap. Prefix marks sealed blobs. Never log [plain] or opened text. */
object AesGcmWrap {
    const val PREFIX = "gcm1."
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun seal(master: SecretKey, plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, master)
        val iv = cipher.iv
        val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val blob = ByteArray(iv.size + ct.size)
        System.arraycopy(iv, 0, blob, 0, iv.size)
        System.arraycopy(ct, 0, blob, iv.size, ct.size)
        return PREFIX + Base64.getEncoder().encodeToString(blob)
    }

    fun open(master: SecretKey, blob: String): String? {
        if (!blob.startsWith(PREFIX)) return null
        return try {
            val raw = Base64.getDecoder().decode(blob.removePrefix(PREFIX))
            if (raw.size <= IV_BYTES) return null
            val iv = raw.copyOfRange(0, IV_BYTES)
            val ct = raw.copyOfRange(IV_BYTES, raw.size)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, master, GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ct), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}

/** AndroidKeyStore AES-256. Key material stays in the keystore. */
object KeystoreAes {
    const val ALIAS = "openflow_secrets_aes"
    const val PROVIDER = "AndroidKeyStore"

    @Synchronized
    fun getOrCreate(): SecretKey {
        load().getSecret()?.let { return it }
        return try {
            generate()
        } catch (_: Exception) {
            load().getSecret() ?: error("secret wrap key missing")
        }
    }

    private fun load(): KeyStore =
        KeyStore.getInstance(PROVIDER).apply { load(null) }

    private fun KeyStore.getSecret(): SecretKey? =
        getKey(ALIAS, null) as? SecretKey

    private fun generate(): SecretKey {
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return gen.generateKey()
    }
}

/**
 * Device store. AES-GCM at rest in [PREFS_NAME], wrapped by [KeystoreAes].
 * Empty [put] deletes. Never logs the key.
 */
class AndroidSecretStore(
    private val prefs: PrefsStore,
    master: () -> SecretKey,
) : SecretStore {
    constructor(context: Context) : this(
        SharedPrefsStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)),
        KeystoreAes::getOrCreate,
    )

    private val wrapKey: SecretKey by lazy(master)
    private val plainCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun put(id: String, key: String) {
        if (key.isEmpty()) {
            plainCache.remove(id)
            prefs.putString(id, "")
            return
        }
        prefs.putString(id, AesGcmWrap.seal(wrapKey, key))
        plainCache[id] = key
    }

    override fun get(id: String): String? {
        plainCache[id]?.let { return it }
        val raw = prefs.getString(id, "")
        if (raw.isEmpty()) return null
        AesGcmWrap.open(wrapKey, raw)?.let {
            plainCache[id] = it
            return it
        }
        if (raw.startsWith(AesGcmWrap.PREFIX)) return null
        plainCache[id] = raw
        return raw
    }

    override fun clear(id: String) {
        plainCache.remove(id)
        prefs.putString(id, "")
    }

    companion object {
        const val PREFS_NAME = "openflow_secrets"
    }
}
