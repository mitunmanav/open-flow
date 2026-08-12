package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PackagePolicyTest {
    @Test
    fun hides_known_bank_packages() {
        assertThat(PackagePolicy.shouldHideBubble("com.chase.sig.android")).isTrue()
        assertThat(PackagePolicy.shouldHideBubble("com.google.android.apps.authenticator2")).isTrue()
    }

    @Test
    fun allows_normal_apps() {
        assertThat(PackagePolicy.shouldHideBubble("com.whatsapp")).isFalse()
        assertThat(PackagePolicy.shouldHideBubble("com.google.android.apps.docs")).isFalse()
    }

    @Test
    fun null_or_blank_allowed() {
        assertThat(PackagePolicy.shouldHideBubble(null)).isFalse()
        assertThat(PackagePolicy.shouldHideBubble("")).isFalse()
    }

    @Test
    fun substring_bank_token() {
        assertThat(PackagePolicy.shouldHideBubble("com.example.bank.mobile")).isTrue()
        assertThat(PackagePolicy.shouldHideBubble("com.foo.wallet")).isTrue()
    }
}
