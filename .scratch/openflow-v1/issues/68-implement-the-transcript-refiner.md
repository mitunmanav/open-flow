# Implement the TranscriptRefiner

Type: task
Status: claimed
Blocked by: 52, 60, 65

> **Partially claimed (matcher slice).** Stages 6–7 and the settings-side validation
> are implemented, tested and committed. **Stages 1–5 and 8 are not.** The ticket is
> not resolved: the refiner is still unimplemented above the matcher and below it, and
> this ticket still owns that work.

## Question

Write the code `core/` still has none of. The `TranscriptRefiner` is the largest
specified component in the project and **no ticket owns implementing it**: ticket 12
settled the pipeline, ticket 60 settled stage 8's authority over authored text, ticket 52
settled the per-app inputs, and ticket 65 settled entry matching — four contracts, zero
lines of production Kotlin. Ticket 42 owns the `SpeechProvider` and ticket 43 owns
`FakeProvider`, so the two ends of the pipeline have owners and the middle does not.

### The first takeable slice is the entry matcher

Stages 6 and 7 are fully specified as of
[How do dictionary and snippet entries match spoken text?](65-dictionary-snippet-matching.md)
and need **no device, no provider and no Android framework** — a matcher over a string
is the whole of it. That makes it the natural first slice and a legitimate unit-test
target on its own:

- the tokenizer (`[\p{L}\p{N}]+` with an internal apostrophe, Unicode case-folded, no
  accent folding) and the trigger key derived from it;
- the replaced range, first matched token's start to last token's end;
- per-position specificity filtering ahead of the longest-match scan, with entry-list
  order never consulted;
- the origin-segment output, which must be a list rather than a string because stage 8
  writes only to dictated segments and may not infer protection by searching for text
  equal to a stored body.

The stand-in in the settings prototype (`.scratch/openflow-v1/prototype/dictionary-snippets-styles.html`,
`patternFor`) still branches on whether a trigger contains a space and is marked as
superseded; this ticket is where it gets replaced. Its settings-side checks — a trigger
with no word in it, and two triggers colliding in one scope — were built ahead of the
matcher and should move with it.

### What else belongs here

Stages 1–5 and 8 on the same ownership: normalization, spoken-command expansion,
backtracking with soft markers, filler removal, list detection, and app-style formatting
that reads complete-text context and writes only dictated segments. This ticket owns the
whole refiner; the entry matcher is where a session should start, not the whole of it.

### Notes

- **FakeProvider (ticket 43) is not required to start.** The matcher is tested with plain
  strings. It is required before the refiner can be exercised end to end through the
  state machine, so it blocks *finishing* this ticket, not beginning it — the same
  "unblocked but not takeable" distinction ticket 42 had to make about the refiner's own
  inputs.
- Per-app settings are frozen before recording and arrive with the Dictation (ticket 52),
  so the refiner takes them as inputs and reads no global state.
- A refiner exception degrades to the raw transcript marked unprocessed (ADR-0002);
  matching must not be able to fail a Dictation.
- Nothing in this ticket reopens a decision. Where a stage's behaviour looks
  under-specified, the fix is a new decision ticket, not an implementation choice made
  here.

## Answer

**Not resolved — stages 6–7 are done, stages 1–5 and 8 are not.** The entry matcher was
the first takeable slice and it is landed: `core`'s first production Kotlin, in
`dev.openflow.dictation.core.refiner`, with no device, provider or Android framework in
it. It needs none of `FakeProvider` (ticket 43), which still blocks *finishing* this
ticket and not beginning it.

### What exists

Five files under `core/src/main/kotlin/dev/openflow/dictation/core/refiner/`, four test
files under the matching test tree, and nothing added to `core/build.gradle.kts` —
`testImplementation(libs.junit)` was already there.

The public API:

| Type | Shape |
| --- | --- |
| `EntryMatcher` | `EntryMatcher(entries: List<RefinerEntry>)`; `applyDictionary(text: String, targetPackage: String): RefinedText` (stage 6); `applySnippets(input: RefinedText, targetPackage: String): RefinedText` (stage 7) |
| `RefinedText` | `RefinedText.of(segments: List<OriginSegment>)`, `RefinedText.dictated(text: String)`; `val segments`, `val text`, `val isEmpty`. Private constructor, so the merge invariant cannot be bypassed |
| `OriginSegment` | `data class OriginSegment(text: String, origin: Origin)` |
| `Origin` | `enum class Origin { DICTATED, AUTHORED }` |
| `RefinerEntry` | `data class RefinerEntry(kind, trigger, body, scope = EveryApp)` with derived `triggerKey: List<String>` and `appScoped: Boolean` |
| `RefinerEntryKind` | `enum class RefinerEntryKind { DICTIONARY, SNIPPET }` |
| `EntryScope` | `sealed interface`: `EntryScope.EveryApp`, `EntryScope.App(packageName)`, `fun appliesTo(targetPackage: String): Boolean` |
| `RefinerTokens` | `object`: `tokenize(text): List<RefinerToken>`, `fold(text): String`, `triggerKey(trigger): List<String>` |
| `RefinerToken` | `data class RefinerToken(text, start, end, folded)` |
| `checkNewEntry` | `fun checkNewEntry(kind, trigger, existing, scope = EveryApp): EntryConflict?` |
| `EntryConflict` | `sealed interface`: `NoWordInTrigger`, `DuplicateTrigger(existing: RefinerEntry)`, each with a `message` |

One matcher serves both entry kinds, as ticket 65 settled: `applyDictionary` and
`applySnippets` are the same scan with a different `RefinerEntryKind`, so there is no
branch anywhere on a trigger's shape. That is the prototype's `patternFor` defect gone —
the branch on whether a trigger contains a space is what made `sign off` fire inside
"design off" while `sig` was safe inside "signal", and `aMultiWordTriggerDoesNotFireInsideALongerWord`
is that exact test inverted.

**The origin-segment output is a list, and the doc comment says why.** `RefinedText`
carries `List<OriginSegment>` rather than returning a `String`, because stage 8 writes
only to dictated segments and may not infer that protection by searching for text equal
to a stored body — the only way to recover the distinction from a string is exactly that
search. `text` is available because stage 8 *reads* it for context; it is not an
alternative for anything that writes. The constructor is private and `of` merges
adjacent same-origin runs, so "a segment is a maximal run of one origin" holds of every
`RefinedText` that exists — which is what makes a stage-6-boundary match *wholly*
authored a consequence of merging rather than a special case.

### The tests, and their count

**59 tests, all passing** (`./gradlew :core:testDebugUnitTest`), read from
`core/build/test-results/`:

| File | Tests |
| --- | --- |
| `EntryMatcherTest` | 32 |
| `RefinerTokensTest` | 11 |
| `EntryConflictTest` | 9 |
| `RefinedTextTest` | 7 |

They assert on **segments** rather than on joined text wherever the rule is about
origin, because a `String` assertion cannot tell a dictated run from an authored one.

**The checks were shown to fail.** Nineteen deliberate breaks were applied one at a time
to the production matcher, each removing exactly one settled rule, and every one was
caught: the specificity filter; longest-match-wins; the scan not resuming after a match;
each of the three ways to get the replaced range wrong; stage 7 rescanning its own
insertions; a dictionary body marked dictated; an app-scoped entry leaking to every app;
accent folding added via `Normalizer`; the apostrophe internal anywhere rather than
between letters; a digit-only trigger treated as wordless; the duplicate check comparing
strings instead of token sequences; the duplicate check spanning kinds; the no-word check
dropped; a refusal that names no existing entry; stage 7 discarding its input's origin;
and the merge of adjacent same-origin runs removed. Three of these initially **survived**
and are worth naming, because surviving is what a test suite does when nobody checks:
longest-match-wins, the scan resuming after a match, and the between-letters apostrophe
rule all passed every test. Each was then given a test that fails without it —
`nothingInsideAChosenMatchIsMatchedAgain`, and the `5'ft` / `2'3'4` cases.

Verified by a test that could have failed: app-scoped `sign` beating a longer global
`sign off`; `sig` inside "signal"; `cafe` vs `café` and `resume` vs `résumé`; punctuation
and case in a stored trigger; six permutations of the entry list producing identical
segments *and* the one correct text; the replaced range leaving surrounding odd spacing
verbatim; stage-7 insertions not rescanned; a stage-6 body rescanned; a match spanning a
stage-6 boundary becoming wholly authored; a dictionary body authored; duplicates
refused at save naming the existing entry; a wordless trigger refused; `don't` one token
and `café-au-lait` three.

### Notes on writing the code

Three places where `refiner.md` was thinner than the code, all resolved without reopening
a decision:

- **Case folding is `lowercase(Locale.ROOT)`, not `Normalizer`.** Full case folding would
  also fold Greek final sigma and decompose `ﬁ`. Folding them together means
  normalizing, and normalizing would decompose accented letters — the folding this
  matcher refuses. Documented in `RefinerTokens` as a limit rather than left to look
  like an oversight.
- **Two entries with an identical trigger in one scope cannot both reach the matcher**
  because `checkNewEntry` refuses the second and `RefinerEntry` refuses to construct a
  wordless trigger. The comparator still needs a total order for safety, so it falls back
  to body then scope — both content, never a list position, which is what keeps the
  order-free promise unconditional. Documented at the call site.
- **`\p{N}` is any Unicode number**, not `Character.isDigit`, which reports only the
  decimal digits. "Letters and digits" of the Unicode kind means a superscript or a
  Roman-numeral digit is a digit to the user saying it.

### What remains

Stages 1–5 (normalization, spoken-command expansion, backtracking with soft markers,
filler removal, list detection) and stage 8 (app-style formatting: three presets, reading
complete-text context, writing only to dictated segments, joining after). Stage 8 has a
real dependency on this slice's `RefinedText` and is now unblocked by it. The
`TranscriptRefiner` type that sequences all eight stages does not exist yet, and neither
does the degradation path of ADR-0002. The prototype's `patternFor` and its cascading
substitution loop are still in the settings HTML; the comment above them now records that
the contract is implemented and what the screen still does differently.
