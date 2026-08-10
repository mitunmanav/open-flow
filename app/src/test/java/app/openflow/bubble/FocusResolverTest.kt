package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure behavioral contract: when root is null, only cached can win.
 * Full a11y nodes need device; we test the decision table with fakes via null roots.
 */
class FocusResolverTest {

    @Test
    fun null_root_and_null_cached_returns_null() {
        val result = FocusResolver.resolveEditable(
            root = null,
            cached = null,
            isUsable = { true },
            findInSubtree = { null }
        )
        assertThat(result).isNull()
    }
}
