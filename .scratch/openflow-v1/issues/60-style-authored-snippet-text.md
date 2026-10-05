# How should app-style formatting treat a snippet's authored text?

Type: grilling
Status: resolved
Blocked by: 51

## Question

A Snippet now supports authored multiline blocks, with line breaks and blank lines kept in
storage. App-style formatting runs after snippet expansion and can change capitalization
and ending punctuation. What is its authority over text the user deliberately wrote?

Decide whether snippet spans are protected from style changes, are formatted like all other
text, or follow a more precise rule. Use a four-line sign-off ending in a comma, a complete
paragraph ending in a question mark, and a snippet embedded between dictated sentences to
make the boundary concrete. Include what happens to line breaks and blank lines at output.

The existing prototype only avoids appending a full stop when text already ends in
punctuation; that is prototype behaviour, not a settled production contract. Resolve the
stage 8 rule and the information stages 7 and 8 need to share, without implementing the
refiner. Consult the glossary and the TranscriptRefiner pipeline order.

Context: [What does a snippet longer than one line look like?](51-long-snippet-surface.md).

## Comments

### 2026-10-05 — Facts checked before discussion

The pipeline order is settled: stage 7 expands snippets, stage 8 applies app style.
Multiline support and preservation of authored newlines/blank lines are settled for
storage, while output-style authority remains explicitly open. Applicable per-package
settings are frozen before recording, and History inserts stored text unchanged.

No production refiner exists yet. The HTML prototype passes a plain string to style;
its Formal branch can trim outer whitespace. Its expansion loop can match text
introduced by an earlier entry. Those are prototype behaviors, not production contracts.
Protection needs actual origin information, not searching for substrings that happen
to equal a stored snippet body. Matching/overlap behavior also remains unspecified.

The first decision round proposes literal preservation of authored snippet characters
under every V1 style preset. Boundary behavior and the stage 7/8 handoff follow that
choice; nothing is resolved yet.

### 2026-10-05 — Authored text preservation agreed

The owner agreed that every V1 style preset preserves authored snippet capitalization,
punctuation, spaces, line breaks and blank lines exactly. Style may change surrounding
dictation. The four-line sign-off ending in a comma and the complete paragraph ending
in a question mark retain their authored text. Boundary behavior, expansion cascading
and the stage 7/8 handoff remain to be agreed before this ticket resolves.

### 2026-10-05 — Remaining recommendations agreed

The owner agreed to formatting against complete-text context with writes limited to
dictated text, no automatic separators or punctuation at snippet boundaries, no final
punctuation after a terminal snippet, one-pass snippet expansion, and ordered segments
carrying dictated/authored origins through style formatting. This confirms shared
understanding and completes the decision.

## Answer

Resolved through live exchange on 2026-10-05. The owner accepted all recommendations.

- Every V1 style preset preserves authored snippet characters exactly: capitalization,
  punctuation, spaces, line breaks and blank lines. Surrounding dictated text remains
  eligible for its selected preset.
- Style reads the complete assembled text for context, while writing only to dictated
  segments. A segment boundary creates no sentence break, space or punctuation by itself.
  Do not treat each dictated segment as a new independent sentence.
- A terminal authored snippet receives no appended punctuation, even if it has none.
  Trailing whitespace does not grant permission to punctuate that authored ending.
  Do not trim or collapse authored whitespace at joins or at output edges.
- Stage 7 expands matching ranges of its input once. Text inserted by an expansion is
  literal; it is never rescanned for another snippet, regardless of entry-list order.
- Stage 7 supplies ordered text segments with dictated/authored origin to stage 8.
  Unreplaced input stays dictated; replacement bodies are authored. Stage 8 preserves
  that origin while applying permitted style changes, then joins the segments for output.
  Searching for text equal to a stored body cannot substitute for actual origin.

Concrete cases:

1. The four-line sign-off below remains exactly as authored, including the blank second
   line and the final comma; Formal adds no full stop.

   ```text
   Best regards,

   Mitun
   OpenFlow,
   ```

2. A complete paragraph such as `Could you review the draft and send your comments by
   Friday?` retains its capitalization and question mark under every preset.
3. In `please review this. [snippet] i will call tomorrow`, the expanded body is unchanged.
   Only the surrounding dictation may be styled, using the full text's sentence context.
   The boundaries alone supply neither punctuation nor additional spacing.
4. An authored body containing another shorthand keeps that shorthand literally. Dictated
   words identical to a body remain dictated and eligible for style changes.

The prototype's plain-string formatting, whitespace trimming and cascading replacements
are not this production contract. Pipeline order, frozen per-package settings, exception
fallback and unchanged History recovery remain as previously settled. No refiner code
or prototype implementation is changed by this decision.

The contract is reflected in the Snippet definition and
[TranscriptRefiner](../../../docs/architecture/refiner.md). Entry matching, boundaries
and overlap rules now have their own decision:
[How do dictionary and snippet entries match spoken text?](65-dictionary-snippet-matching.md).
