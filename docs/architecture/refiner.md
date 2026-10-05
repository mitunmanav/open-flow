# TranscriptRefiner

Provider-independent cleanup of raw STT text. Deterministic in V1 — no LLM required. The advanced-refiner slot in the pipeline is reserved (identity in V1) for a future local-LLM refiner without changing the pipeline shape.

## Pipeline order

1. Normalization: whitespace, casing baseline.
2. Spoken-command expansion: spoken punctuation → symbols, new lines/paragraphs, quotes/parens.
3. Backtracking resolution: "scratch that <X>" deletes back to the last sentence boundary; "no", "I mean", "actually" are soft markers only (the following clause does not replace the prior one in V1).
4. Filler removal: "um", "uh", discourse markers like "you know"/"like" — per-language filler lists, user toggle.
5. List detection: ordinals ("number one", "first") → numbered list; line-leading "dash"/"bullet" → bulleted list. Plain-text `1.` / `-`.
6. Dictionary replacements: user wrong→right map, matched per **Entry matching** below.
7. Snippet expansion: user shorthand→authored text expansion map, user-toggled. V1 supports phrases, paragraphs and multiline blocks. Expand matches in this stage's input once; inserted bodies remain literal and never trigger another snippet. Carry ordered dictated/authored segments into stage 8. The settings list uses a two-line preview with an inline full-body editor (see [What does a snippet longer than one line look like?](../../.scratch/openflow-v1/issues/51-long-snippet-surface.md)).
8. App-style formatting: one of `Casual` / `Neutral` / `Formal` presets (capitalization + ending punctuation rules), assigned per app package. Read complete-text context, change only dictated segments, then join the segments for output.

Style formatting runs last so it sees final content. Its authority over authored text is
settled by [How should app-style formatting treat a snippet's authored text?](../../.scratch/openflow-v1/issues/60-style-authored-snippet-text.md).

## Entry matching

Stages 6 and 7 share one matcher. A dictionary entry and a snippet differ only in what
the body is for, so there is no per-kind rule and multi-word triggers are allowed for
both. Settled by
[How do dictionary and snippet entries match spoken text?](../../.scratch/openflow-v1/issues/65-dictionary-snippet-matching.md).

**A token** is a maximal run of Unicode letters and digits, where an apostrophe (`'` or
`’`) between letters is internal and every other non-alphanumeric character separates.
So `don't` is one token, `sign-off` is two, `iphone15` is one token while `iphone 15` is
two, and `24/7` is two. Comparison is Unicode case-folded, with **no accent folding**:
`Café` matches `café`, but `cafe` does not match `café`, because folding would also make
`resume` match `résumé`. Punctuation and case in a stored trigger are therefore not
load-bearing.

Any run of whitespace separates tokens, newlines included, so a match may span a
paragraph break. **One match replaces the range from the first matched token's start to
the last token's end**, and nothing outside that range is touched. Matching is defined on
the text as it reaches these two stages and is re-tokenized there, deliberately
independent of stage 1's normalization.

**Choosing matches.** Per position the longest applicable match wins, except that
**specificity filters first**: if any app-scoped entry matches at a position, the longest
of those wins, and only when none does is the longest global match considered. Then one
left-to-right scan resumes after each match, and every occurrence is replaced. Entry-list
order is never consulted. The specificity rule exists so that "the app's own version
wins over the every-app one" holds against a longer global trigger, not only against an
identical one.

**Conflicts.** A duplicate trigger in the same scope — compared as token sequences, so
`sign-off` and `sign off` are the same trigger — is **rejected at save**, naming the
existing entry. The same trigger in different scopes is allowed and resolved by
specificity. The same trigger as both a dictionary entry and a snippet in one scope is
allowed: the stages are ordered, so the dictionary runs first and the snippet then fires
on its output.

**Origin.** Replacement bodies from both stages are authored, and origin means stage 8's
write authority only. Stage 7's own insertions are never rescanned by stage 7, while a
stage 6 body is scannable by stage 7. A match spanning a stage 6 boundary yields a wholly
authored range, never a partial one.

**Accepted limitations.** A snippet body is never dictionary-corrected, because stage 6
already ran; re-running it would reopen the pipeline order. And triggers are effectively
unusable in a script without word spacing, because such a run is a single token — V1's
default is English and the bilingual tier is a download-tier test, so no per-language
matcher is built. The matcher is deterministic string logic and is unit-tested in
`core`, not exercised by the acceptance gate.

## Authored snippet text and style

Every V1 preset preserves authored capitalization, punctuation, spaces, line breaks and
blank lines exactly, both in storage and at output. A snippet boundary supplies no sentence
break, separator or punctuation by itself. Style uses the full assembled text's context;
the start of a dictated segment alone does not start a new sentence.

When the text ends in an authored snippet, append no final punctuation, even if the body
has none. Trailing whitespace does not make that ending eligible for punctuation.
Do not trim or collapse authored whitespace at boundaries or output edges.

For example, this four-line sign-off retains its blank line and final comma under Formal:

```text
Best regards,

Mitun
OpenFlow,
```

A paragraph such as `Could you review the draft and send your comments by Friday?` retains
its question mark. In `please review this. [snippet] i will call tomorrow`, only the
surrounding dictation may change; the authored body and its whitespace stay intact.

Stage 7 marks unreplaced input as dictated and each inserted body as authored. Stage 8
keeps this origin information through its edits, then joins the segments. It must not
infer protection by searching for text equal to a stored body: identical dictated words
remain dictated. A dictionary replacement body is authored on the same terms.
Replacement bodies containing another shorthand are not expanded again.

What produces those segments — the tokenizer, the replaced range, match selection and
conflict handling — is [Entry matching](#entry-matching).

## Per-app inputs

The Dictation Controller supplies the original target package and the applicable dictionary,
snippet and style settings frozen before recording. Package name determines app scope;
field characteristics are private to insertion safety. Changes of active app or settings
apply to subsequent Dictations. See
[Where does app identity enter the pipeline?](../../.scratch/openflow-v1/issues/52-where-does-app-identity-enter.md).

History recovery inserts stored text unchanged and does not invoke this pipeline again.

## Language behaviour

Command sets and filler lists are per-language, keyed by provider-reported language. V1 ships English + a small set; unknown language → pass-through with dictionary/snippets/style only.

## Failure

A refiner exception degrades to the raw transcript marked unprocessed (ADR-0002); the dictation is not failed. An "advanced refiner" failure never fails insertion.
