package app.openflow.bubble

import app.openflow.prefs.FlowPrefs
import app.openflow.text.WritingStyle

/**
 * High-level application category for contextual styling and LLM prompt adaptation.
 */
enum class AppCategory(
    val label: String,
    val defaultStyle: WritingStyle,
    val promptGuideline: String
) {
    MESSAGING(
        label = "Chat",
        defaultStyle = WritingStyle.CASUAL,
        promptGuideline = "Target is a messaging/chat conversation. Keep sentences natural, concise, and conversational. Do not add formal email greetings or closings."
    ),
    EMAIL(
        label = "Email",
        defaultStyle = WritingStyle.FORMAL,
        promptGuideline = "Target is an email client. Format with clean paragraphs, professional grammar, and preserve proper greetings and sign-offs."
    ),
    WORK_COLLAB(
        label = "Work",
        defaultStyle = WritingStyle.FORMAL,
        promptGuideline = "Target is a professional collaboration tool (Slack/Teams). Format clearly with concise phrasing and crisp bullet points when appropriate."
    ),
    DOCS_NOTES(
        label = "Notes",
        defaultStyle = WritingStyle.CASUAL,
        promptGuideline = "Target is a note or document editor. Format into structured paragraphs, headers, and bulleted or numbered items when speaking lists."
    ),
    DEV_TERMINAL(
        label = "Dev",
        defaultStyle = WritingStyle.CASUAL,
        promptGuideline = "Target is a developer terminal, code repository, or coding tool. Preserve exact technical terminology, code identifiers, camelCase, snake_case, CLI flags, and symbols verbatim."
    ),
    AI_SEARCH(
        label = "Search",
        defaultStyle = WritingStyle.CASUAL,
        promptGuideline = "Target is a search bar or AI query box. Format as a direct, concise search query or prompt. Strip conversational pleasantries (e.g. 'can you please tell me')."
    ),
    GENERAL(
        label = "General",
        defaultStyle = WritingStyle.CASUAL,
        promptGuideline = "Format clean, natural dictation."
    );

    companion object {
        fun fromName(name: String, fallback: AppCategory = GENERAL): AppCategory =
            values().firstOrNull { it.name.equals(name, ignoreCase = true) } ?: fallback
    }
}

/**
 * User-configured custom override for a specific application package.
 */
data class AppOverride(
    val packageName: String,
    val category: AppCategory,
    val style: WritingStyle,
    val customPrompt: String = ""
)

/**
 * Context descriptor for active app and focused field.
 */
data class AppContext(
    val category: AppCategory,
    val packageName: String,
    val hintText: String?,
    val defaultStyle: WritingStyle = category.defaultStyle,
    val promptHint: String = category.promptGuideline,
    val isCustomOverride: Boolean = false
)

/**
 * Universal App Detection Engine.
 * Matches package tokens and hint strings to determine app category and context.
 */
object AppContextEngine {

    private val messagingTokens = listOf(
        "whatsapp", "telegram", "org.telegram", "org.thoughtcrime.securesms",
        "instagram", "sms", "messaging", "facebook.orca", "discord", "viber",
        "line", "signal", "im.vector", "matrix"
    )

    private val emailTokens = listOf(
        "android.gm", "gmail", "k9", "outlook", "mail.android",
        "yahoo.mobile.client.android.mail", "protonmail", "thunderbird", "spark",
        "superhuman", "bluemail"
    )

    private val workTokens = listOf(
        "slack", "teams", "linkedin", "skype", "webex", "jira", "asana",
        "linear", "basecamp", "trello", "clickup"
    )

    private val docsTokens = listOf(
        "notion", "obsidian", "docs.editors.docs", "keep", "samsung.android.app.notes",
        "standardnotes", "simplenote", "evernote", "onenote", "bear"
    )

    private val devTokens = listOf(
        "termux", "github", "gitlab", "gitjournal", "code", "terminal",
        "stackexchange", "stackoverflow", "android.studio"
    )

    private val searchTokens = listOf(
        "chatgpt", "perplexity", "claude", "chrome", "firefox", "brave",
        "browser", "search", "duckduckgo", "edge", "opera"
    )

    private val searchHintTokens = listOf(
        "search", "query", "find", "type url", "ask anything", "search or type"
    )

    fun detect(packageName: String?, hintText: String? = null): AppContext {
        val pkg = packageName.orEmpty().lowercase().trim()
        val hint = hintText.orEmpty().lowercase().trim()

        val category = when {
            hint.isNotEmpty() && searchHintTokens.any { hint.contains(it) } -> AppCategory.AI_SEARCH
            emailTokens.any { pkg.contains(it) } -> AppCategory.EMAIL
            devTokens.any { pkg.contains(it) } -> AppCategory.DEV_TERMINAL
            workTokens.any { pkg.contains(it) } -> AppCategory.WORK_COLLAB
            docsTokens.any { pkg.contains(it) } -> AppCategory.DOCS_NOTES
            messagingTokens.any { pkg.contains(it) } -> AppCategory.MESSAGING
            searchTokens.any { pkg.contains(it) } -> AppCategory.AI_SEARCH
            else -> AppCategory.GENERAL
        }

        return AppContext(
            category = category,
            packageName = pkg,
            hintText = hintText,
            defaultStyle = category.defaultStyle,
            promptHint = category.promptGuideline
        )
    }

    /**
     * Resolves application context taking into account user preferences, category customizations,
     * and explicit per-app package overrides.
     */
    fun resolveContext(
        packageName: String?,
        hintText: String? = null,
        prefs: FlowPrefs? = null
    ): AppContext {
        val pkg = packageName.orEmpty().lowercase().trim()
        if (prefs == null) {
            return detect(packageName, hintText)
        }

        // If user disabled automatic app context detection, use global default writing style
        if (!prefs.appContextEnabled) {
            return AppContext(
                category = AppCategory.GENERAL,
                packageName = pkg,
                hintText = hintText,
                defaultStyle = prefs.style(),
                promptHint = AppCategory.GENERAL.promptGuideline
            )
        }

        // Check if user set a specific per-app override
        val override = prefs.getAppOverride(pkg)
        if (override != null) {
            val prompt = if (override.customPrompt.isNotBlank()) {
                override.customPrompt
            } else {
                override.category.promptGuideline
            }
            return AppContext(
                category = override.category,
                packageName = pkg,
                hintText = hintText,
                defaultStyle = override.style,
                promptHint = prompt,
                isCustomOverride = true
            )
        }

        // Detect default category
        val detected = detect(packageName, hintText)
        val customStyle = prefs.getCategoryStyle(detected.category)
        val userPrompt = prefs.getCategoryPrompt(detected.category).trim()

        val blendedPrompt = if (userPrompt.isNotBlank()) {
            "${detected.category.promptGuideline} Custom instructions: $userPrompt"
        } else {
            detected.category.promptGuideline
        }

        return detected.copy(
            defaultStyle = customStyle,
            promptHint = blendedPrompt
        )
    }
}
