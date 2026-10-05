package dev.openflow.dictation.core.refiner

/**
 * Which stage an entry belongs to. A dictionary entry and a snippet differ only in
 * what the body is for, so this value selects which list is scanned and never how a
 * match is chosen — that is the whole content of "one matcher for both kinds".
 *
 * [DICTIONARY] repairs what was said; [SNIPPET] says something the user had not. Both
 * bodies are authored, and both triggers may span several words.
 */
enum class RefinerEntryKind {
    DICTIONARY,
    SNIPPET,
}

/**
 * Where an entry applies. Package name is the identity, settled by ticket 52: field
 * characteristics are the inserter's concern and never reach the matcher, and one
 * Dictation has one target package, so two entries scoped to two different apps can
 * never compete.
 */
sealed interface EntryScope {

    /** Applies to whatever package is being dictated into. */
    data object EveryApp : EntryScope {
        override fun appliesTo(targetPackage: String): Boolean = true
        override fun toString(): String = "every app"
    }

    /** Applies only to [packageName]. */
    data class App(val packageName: String) : EntryScope {
        override fun appliesTo(targetPackage: String): Boolean = packageName == targetPackage
        override fun toString(): String = packageName
    }

    /** Whether this scope covers a Dictation being inserted into [targetPackage]. */
    fun appliesTo(targetPackage: String): Boolean
}

/**
 * One dictionary entry or snippet: what the user says, what lands in the field, and
 * which apps it belongs to.
 *
 * [trigger] is kept exactly as typed, because that is what the settings surface shows
 * back and what a duplicate rejection has to name. [triggerKey] is the same trigger
 * reduced to the token sequence the matcher compares, so `sign-off` and `sign off`
 * are one trigger to every rule here — matching and duplicate rejection agree because
 * they are the same derivation, not two implementations of it.
 */
data class RefinerEntry(
    val kind: RefinerEntryKind,
    val trigger: String,
    val body: String,
    val scope: EntryScope = EntryScope.EveryApp,
) {
    /** The trigger as a token sequence; empty when the trigger holds no word. */
    val triggerKey: List<String> = RefinerTokens.triggerKey(trigger)

    /** Whether this entry is app-scoped, which is what specificity filters on. */
    val appScoped: Boolean = scope is EntryScope.App

    init {
        require(triggerKey.isNotEmpty()) {
            "a trigger with no word in it can never match; checkNewEntry refuses it at save"
        }
    }
}