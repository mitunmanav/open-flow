package app.openflow.secrets

import app.openflow.prefs.MemoryPrefsStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
