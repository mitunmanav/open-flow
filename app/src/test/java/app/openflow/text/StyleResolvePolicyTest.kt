package app.openflow.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StyleResolvePolicyTest {
    @Test
    fun detect_personal_messaging() {
        assertThat(StyleResolvePolicy.detect("com.whatsapp")).isEqualTo(StyleCategory.PERSONAL)
        assertThat(StyleResolvePolicy.detect("org.telegram.messenger")).isEqualTo(StyleCategory.PERSONAL)
        assertThat(StyleResolvePolicy.detect("org.thoughtcrime.securesms")).isEqualTo(StyleCategory.PERSONAL)
        assertThat(StyleResolvePolicy.detect("com.instagram.android")).isEqualTo(StyleCategory.PERSONAL)
        assertThat(StyleResolvePolicy.detect("com.discord")).isEqualTo(StyleCategory.PERSONAL)
    }

    @Test
    fun detect_email() {
        assertThat(StyleResolvePolicy.detect("com.google.android.gm")).isEqualTo(StyleCategory.EMAIL)
        assertThat(StyleResolvePolicy.detect("com.microsoft.office.outlook")).isEqualTo(StyleCategory.EMAIL)
        assertThat(StyleResolvePolicy.detect("ch.protonmail.android")).isEqualTo(StyleCategory.EMAIL)
    }

    @Test
    fun detect_work() {
        assertThat(StyleResolvePolicy.detect("com.Slack")).isEqualTo(StyleCategory.WORK)
        assertThat(StyleResolvePolicy.detect("com.microsoft.teams")).isEqualTo(StyleCategory.WORK)
        assertThat(StyleResolvePolicy.detect("com.linkedin.android")).isEqualTo(StyleCategory.WORK)
    }

    @Test
    fun detect_unknown_is_other() {
        assertThat(StyleResolvePolicy.detect("app.openflow.debug")).isEqualTo(StyleCategory.OTHER)
        assertThat(StyleResolvePolicy.detect(null)).isEqualTo(StyleCategory.OTHER)
        assertThat(StyleResolvePolicy.detect("")).isEqualTo(StyleCategory.OTHER)
    }

    @Test
    fun precedence_personal_beats_work_token_collision() {
        // If a package somehow matched both, PERSONAL wins (Wispr Android).
        assertThat(
            StyleResolvePolicy.categoryForMatches(
                setOf(StyleCategory.WORK, StyleCategory.PERSONAL, StyleCategory.EMAIL)
            )
        ).isEqualTo(StyleCategory.PERSONAL)
        assertThat(
            StyleResolvePolicy.categoryForMatches(
                setOf(StyleCategory.WORK, StyleCategory.EMAIL)
            )
        ).isEqualTo(StyleCategory.EMAIL)
    }

    @Test
    fun user_assignment_wins_over_detect() {
        val assignments = mapOf("com.whatsapp" to StyleCategory.WORK)
        val styles = mapOf(
            StyleCategory.PERSONAL to WritingStyle.VERY_CASUAL,
            StyleCategory.WORK to WritingStyle.FORMAL,
            StyleCategory.EMAIL to WritingStyle.FORMAL,
            StyleCategory.OTHER to WritingStyle.CASUAL,
        )
        assertThat(
            StyleResolvePolicy.resolve("com.whatsapp", assignments, styles)
        ).isEqualTo(WritingStyle.FORMAL)
    }

    @Test
    fun resolve_uses_category_style_after_detect() {
        val styles = mapOf(
            StyleCategory.PERSONAL to WritingStyle.VERY_CASUAL,
            StyleCategory.WORK to WritingStyle.EXCITED,
            StyleCategory.EMAIL to WritingStyle.FORMAL,
            StyleCategory.OTHER to WritingStyle.CASUAL,
        )
        assertThat(
            StyleResolvePolicy.resolve("com.whatsapp", emptyMap(), styles)
        ).isEqualTo(WritingStyle.VERY_CASUAL)
        assertThat(
            StyleResolvePolicy.resolve("com.google.android.gm", emptyMap(), styles)
        ).isEqualTo(WritingStyle.FORMAL)
    }

    @Test
    fun resolve_coerces_disallowed_stored_style() {
        val styles = mapOf(
            StyleCategory.PERSONAL to WritingStyle.EXCITED, // invalid for personal
            StyleCategory.WORK to WritingStyle.FORMAL,
            StyleCategory.EMAIL to WritingStyle.FORMAL,
            StyleCategory.OTHER to WritingStyle.FORMAL,
        )
        assertThat(
            StyleResolvePolicy.resolve("com.whatsapp", emptyMap(), styles)
        ).isEqualTo(WritingStyle.CASUAL)
    }
}
