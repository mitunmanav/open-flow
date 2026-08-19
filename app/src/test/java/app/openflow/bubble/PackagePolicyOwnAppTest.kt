package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PackagePolicyOwnAppTest {
    @Test
    fun debug_suffix_is_own() {
        assertThat(PackagePolicy.isOwnApp("app.openflow")).isTrue()
        assertThat(PackagePolicy.isOwnApp("app.openflow.debug")).isTrue()
        assertThat(PackagePolicy.isOwnApp("com.whatsapp")).isFalse()
    }
}
