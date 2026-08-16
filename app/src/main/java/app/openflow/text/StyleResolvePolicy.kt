package app.openflow.text

/**
 * Resolve [WritingStyle] from package + user assignments + per-category styles.
 * Wispr Android precedence on multi-match: Personal → Email → Work → Other.
 */
object StyleResolvePolicy {

    private val personalTokens = listOf(
        "whatsapp", "telegram", "org.thoughtcrime.securesms",
        "instagram", "sms", "messaging", "facebook.orca", "discord", "viber",
        "line.android", "signal", "im.vector", "wechat", "messenger"
    )

    private val emailTokens = listOf(
        "android.gm", "gmail", "k9", "outlook", "mail.android",
        "yahoo.mobile.client.android.mail", "protonmail", "thunderbird", "spark",
        "superhuman", "bluemail", "samsung.android.email", "huawei.email"
    )

    private val workTokens = listOf(
        "slack", "teams", "linkedin", "skype", "webex", "jira", "asana",
        "linear", "basecamp", "trello", "clickup", "zoom"
    )

    private val precedence = listOf(
        StyleCategory.PERSONAL,
        StyleCategory.EMAIL,
        StyleCategory.WORK,
        StyleCategory.OTHER,
    )

    fun detect(packageName: String?): StyleCategory {
        val pkg = packageName.orEmpty().lowercase().trim()
        if (pkg.isEmpty()) return StyleCategory.OTHER
        val matches = mutableSetOf<StyleCategory>()
        if (personalTokens.any { pkg.contains(it) }) matches += StyleCategory.PERSONAL
        if (emailTokens.any { pkg.contains(it) }) matches += StyleCategory.EMAIL
        if (workTokens.any { pkg.contains(it) }) matches += StyleCategory.WORK
        return categoryForMatches(matches)
    }

    fun categoryForMatches(matches: Set<StyleCategory>): StyleCategory {
        if (matches.isEmpty()) return StyleCategory.OTHER
        for (cat in precedence) {
            if (cat in matches) return cat
        }
        return StyleCategory.OTHER
    }

    fun category(
        packageName: String?,
        assignments: Map<String, StyleCategory>,
    ): StyleCategory {
        val pkg = packageName.orEmpty().lowercase().trim()
        if (pkg.isNotEmpty()) {
            assignments[pkg]?.let { return it }
            assignments.entries.firstOrNull {
                it.key.lowercase().trim() == pkg
            }?.value?.let { return it }
        }
        return detect(packageName)
    }

    fun resolve(
        packageName: String?,
        assignments: Map<String, StyleCategory>,
        styles: Map<StyleCategory, WritingStyle>,
    ): WritingStyle {
        val cat = category(packageName, assignments)
        val raw = styles[cat] ?: cat.defaultStyle
        return cat.coerce(raw)
    }
}
