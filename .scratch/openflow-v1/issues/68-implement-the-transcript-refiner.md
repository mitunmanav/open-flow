# Implement the TranscriptRefiner

Type: task
Status: open
Blocked by: 52, 60, 65

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
