package app.openflow.bubble

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BubbleDragCacheTest {
    @Test
    fun drag_cache_holds_size_until_clear() {
        val c = BubbleDragCache()
        c.begin(200, 96)
        assertThat(c.sizeOr(1, 1)).isEqualTo(200 to 96)
        c.clear()
        assertThat(c.sizeOr(10, 20)).isEqualTo(10 to 20)
    }
}
