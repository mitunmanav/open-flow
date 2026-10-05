package dev.openflow.dictation.core.refiner

/**
 * Stages 6 and 7: dictionary replacements and snippet expansion, through one matcher.
 *
 * The contract is [Entry matching][entry-matching] in `docs/architecture/refiner.md`,
 * settled by ticket 65 and replacing this class's prototype stand-in. The rules, and
 * the reason each is here rather than left to a reader:
 *
 * - **Specificity filters per position, before the longest-match scan.** This is the
 *   rule that had to be added mid-grill, because nearest-scope-wins and
 *   longest-match-wins contradict each other: for an app-scoped `sign` against a
 *   global `sign off`, one says `sign`, the other says `sign off`. Filtering per
 *   position keeps both promises — the app's own entry wins even against a longer
 *   global one — while leaving the scan order-free.
 * - **Entry-list order is never consulted.** Every choice below is made by comparing
 *   content: scope, token count, then the entry's own text. There is no tie-break
 *   that reads a list position, so permuting the entry list cannot change a result.
 * - **Re-tokenized here, deliberately.** Stages 6 and 7 tokenize the text as it
 *   reaches them rather than trusting stage 1's normalization, so no future change to
 *   stage 1 can silently change which entries match.
 * - **One pass, then nothing is rescanned.** The scan reads the tokens of the input
 *   and writes bodies into the output, so an inserted body is never itself a
 *   candidate. That is the whole of stage 7's insertion rule, and it is structural:
 *   there is no code path that could re-enter the scan over output text. A stage 6
 *   body *is* rescanned, because it is part of stage 7's input.
 *
 * The matcher is deterministic string logic — no device, no provider, no Android
 * framework — so it is unit-tested here rather than exercised by the acceptance gate.
 * It cannot fail a Dictation: it does no I/O and raises nothing while matching
 * (ADR-0002's degradation belongs to the refiner around it).
 *
 * [entry-matching]: ../../../../../../docs/architecture/refiner.md#entry-matching
 */
class EntryMatcher(entries: List<RefinerEntry>) {

    /**
     * Copied at construction so a caller mutating its own list afterwards cannot change
     * what matches mid-Dictation.
     */
    private val entries: List<RefinerEntry> = entries.toList()

    /**
     * Stage 6: replaces every occurrence of a dictionary entry's trigger with its body.
     *
     * The input is stage 5's plain text, so all of it is [Origin.DICTATED] and every
     * body it inserts is [Origin.AUTHORED] — a dictionary body is authored on the same
     * terms as a snippet's, and stage 8 therefore cannot repair its capitalization.
     */
    fun applyDictionary(text: String, targetPackage: String): RefinedText =
        expand(RefinerEntryKind.DICTIONARY, RefinedText.dictated(text), targetPackage)

    /**
     * Stage 7: expands every occurrence of a snippet's trigger with its body, once.
     *
     * [input] is stage 6's segmented output, so its authored bodies are part of this
     * stage's input and are matched in like anything else. A match spanning a stage 6
     * boundary yields a wholly authored range — see [RefinedText].
     */
    fun applySnippets(input: RefinedText, targetPackage: String): RefinedText =
        expand(RefinerEntryKind.SNIPPET, input, targetPackage)

    private fun expand(
        kind: RefinerEntryKind,
        input: RefinedText,
        targetPackage: String,
    ): RefinedText {
        val applicable = entries.filter {
            it.kind == kind && it.scope.appliesTo(targetPackage) && it.triggerKey.isNotEmpty()
        }
        if (applicable.isEmpty()) return input

        // Only the first token has to be looked up; the rest is checked in place. One
        // list per distinct first token, so a transcript scans in token count rather
        // than in entries tried.
        val byFirstToken = applicable.groupBy { it.triggerKey.first() }

        val text = input.text
        val tokens = RefinerTokens.tokenize(text)
        val builder = RefinedTextBuilder()
        var copied = 0
        var at = 0
        while (at < tokens.size) {
            val hit = bestAt(tokens, at, byFirstToken)
            if (hit == null) {
                at++
                continue
            }
            val rangeStart = tokens[at].start
            val rangeEnd = tokens[at + hit.triggerKey.size - 1].end
            builder.appendRange(input, copied, rangeStart)
            builder.append(hit.body, Origin.AUTHORED)
            copied = rangeEnd
            // Resume after the match, so `sig sig` replaces both and `sign off` is not
            // re-examined as a bare `sign` at the same position.
            at += hit.triggerKey.size
        }
        builder.appendRange(input, copied, text.length)
        return builder.build()
    }

    /**
     * The entry that wins at token position [at], or null when none does.
     *
     * Specificity first: if any app-scoped entry matches here, the global entries are
     * not considered at all. That is what makes the app's own entry win against a
     * *longer* global one, not only against an identical one.
     */
    private fun bestAt(
        tokens: List<RefinerToken>,
        at: Int,
        byFirstToken: Map<String, List<RefinerEntry>>,
    ): RefinerEntry? {
        val candidates = byFirstToken[tokens[at].folded] ?: return null
        var bestAppScoped: RefinerEntry? = null
        var bestGlobal: RefinerEntry? = null
        for (entry in candidates) {
            if (!matchesAt(tokens, at, entry.triggerKey)) continue
            if (entry.appScoped) {
                if (bestAppScoped == null || BEST_MAXIMUM.compare(entry, bestAppScoped) < 0) {
                    bestAppScoped = entry
                }
            } else {
                if (bestGlobal == null || BEST_MAXIMUM.compare(entry, bestGlobal) < 0) {
                    bestGlobal = entry
                }
            }
        }
        // Specificity has already separated the pools, so this only picks the longest
        // within the one that survives. An app-scoped entry wins here even against a
        // longer global one, which is the rule that could not be expressed as
        // nearest-scope-wins alone.
        //
        // The tie-break inside a pool is on content — body, then scope — never on a
        // position in the caller's list, so the scan stays order-free even for a
        // duplicate set that save should already have refused. Choosing one of two
        // identical triggers beats degrading a whole Dictation over one bad row
        // (ADR-0002).
        return bestAppScoped ?: bestGlobal
    }

    /** Whether [key] is exactly the folded text of the tokens starting at [at]. */
    private fun matchesAt(tokens: List<RefinerToken>, at: Int, key: List<String>): Boolean {
        if (at + key.size > tokens.size) return false
        for (offset in key.indices) {
            if (tokens[at + offset].folded != key[offset]) return false
        }
        return true
    }

    private companion object {
        /**
         * Ranks entries by what the contract says wins at one position, longest trigger
         * first. Named for how it is used — `compare(a, b) < 0` means *a* is the better
         * match — because the direction is easy to invert and an inverted comparator
         * here picks the shortest trigger while looking entirely reasonable.
         *
         * The secondary keys exist only to make the comparison total for a duplicate
         * set, and both are content rather than list position.
         */
        private val BEST_MAXIMUM: Comparator<RefinerEntry> =
            compareByDescending<RefinerEntry> { it.triggerKey.size }
                .thenBy { it.body }
                .thenBy { it.scope.toString() }
    }
}