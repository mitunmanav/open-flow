package app.openflow.secrets

import app.openflow.prefs.FlowPrefs
import app.openflow.prefs.MemoryPrefsStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecretStoreTest {

    @Test
    fun memory_put_get_clear() {
        val store: SecretStore = MemorySecretStore()
        assertThat(store.get("openai")).isNull()
        store.put("openai", "sk-test")
        assertThat(store.get("openai")).isEqualTo("sk-test")
        store.clear("openai")
        assertThat(store.get("openai")).isNull()
    }

    @Test
    fun memory_keys_are_isolated() {
        val store = MemorySecretStore()
        store.put("openai", "a")
        store.put("grok", "b")
        store.clear("openai")
        assertThat(store.get("openai")).isNull()
        assertThat(store.get("grok")).isEqualTo("b")
    }

    @Test
    fun prefs_backed_store_round_trip() {
        val prefs = MemoryPrefsStore()
        val store: SecretStore = PrefsSecretStore(prefs)
        store.put("custom", "tok-1")
        assertThat(store.get("custom")).isEqualTo("tok-1")
        store.clear("custom")
        assertThat(store.get("custom")).isNull()
    }

    @Test
    fun empty_key_reads_as_missing() {
        val store = MemorySecretStore()
        store.put("openai", "")
        assertThat(store.get("openai")).isNull()
    }

    @Test
    fun empty_put_deletes_encrypted() {
        val prefs = MemoryPrefsStore()
        val store = encrypted(prefs)
        store.put("openai", "sk-live")
        store.put("openai", "")
        assertThat(store.get("openai")).isNull()
        assertThat(prefs.getString("openai", "")).isEmpty()
    }

    @Test
    fun android_store_round_trip_is_not_plaintext() {
        val prefs = MemoryPrefsStore()
        val store = encrypted(prefs)
        store.put("openai", "sk-live")
        assertThat(store.get("openai")).isEqualTo("sk-live")
        val atRest = prefs.getString("openai", "")
        assertThat(atRest).isNotEmpty()
        assertThat(atRest).doesNotContain("sk-live")
        assertThat(atRest).startsWith(AesGcmWrap.PREFIX)
    }

    @Test
    fun android_store_keys_isolated() {
        val store = encrypted(MemoryPrefsStore())
        store.put("openai", "a")
        store.put("grok", "b")
        store.clear("openai")
        assertThat(store.get("openai")).isNull()
        assertThat(store.get("grok")).isEqualTo("b")
    }

    @Test
    fun android_store_reads_legacy_plaintext() {
        val prefs = MemoryPrefsStore()
        prefs.putString("openai", "sk-old")
        val store = encrypted(prefs)
        assertThat(store.get("openai")).isEqualTo("sk-old")
        store.put("openai", "sk-new")
        assertThat(prefs.getString("openai", "")).doesNotContain("sk-new")
        assertThat(store.get("openai")).isEqualTo("sk-new")
    }

    @Test
    fun corrupt_sealed_blob_is_missing() {
        val prefs = MemoryPrefsStore()
        prefs.putString("openai", AesGcmWrap.PREFIX + "not-valid-cipher")
        val store = encrypted(prefs)
        assertThat(store.get("openai")).isNull()
    }

    @Test
    fun secrets_file_is_not_flow_prefs() {
        assertThat(AndroidSecretStore.PREFS_NAME).isEqualTo("openflow_secrets")
        assertThat(AndroidSecretStore.PREFS_NAME).isNotEqualTo(FlowPrefs.PREFS_NAME)
    }

    private fun encrypted(prefs: MemoryPrefsStore): AndroidSecretStore {
        val key = softwareKey()
        return AndroidSecretStore(prefs) { key }
    }

    private fun softwareKey(): SecretKey {
        val gen = KeyGenerator.getInstance("AES")
        gen.init(256)
        return gen.generateKey()
    }
}
