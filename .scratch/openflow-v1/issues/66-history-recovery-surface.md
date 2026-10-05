# How does History present recoverable and unprocessed entries?

Type: prototype
Status: blocked:human
Reason: Awaiting an owner verdict on which of the three History variants wins; the prototype is built and verified.
Blocked by: 05, 14, 28, 61

## Answer

**Awaiting an owner verdict.** The prototype is built and verified; the three variants are on
the table and none has been chosen. See the comments below for the asset, the recommendation
and the findings — the parts that are *not* variant-dependent are settled below.

### Settled regardless of which variant wins

- **Status is two independent marks, never one verdict.** "Did the text land" and "did the
  cleanup run" are separate facts, and ADR-0002's terminal-outcome enum plus ticket 14's
  `transcript_entry.outcome` force them into one value. An entry can be **both** unprocessed
  and not-inserted, and an unprocessed entry can have landed perfectly well. A single
  "failed" label cannot hold that, so a History entry shows up to two marks and each limit
  explains only its own mark. This is the same shape as ticket 65's finding that a value
  asked to carry two questions is a lie about what it controls.
- **Four status words, and they say where the text is, not how it went.** `Inserted`,
  `Not inserted`, `Unprocessed` / `Not processed`, `Nothing captured`. "Processed" (never
  "processed" alone) is the positive of `Unprocessed`, so a clean entry does not have to
  claim a virtue.
- **Where the text is, in words, on every row.** `In Mail · Message body`, `Not in any
  field · 2 attempts, last 25 min ago, cancelled`, `Text kept, not placed`. The ticket's
  core prohibition — do not imply text has already been inserted when it remains
  recoverable — is a sentence that has to exist, not a styling choice.
- **Place text always carries its limit**, wherever it appears: *"Placing re-inserts these
  words unchanged. Nothing is transcribed or cleaned up again."* The ticket forbids implying
  retry re-transcribes or re-runs the refiner, and ticket 61 settled the flow as inserting
  stored text unchanged; the sentence is where those two meet. **This is not a layout
  decision and the variants were not allowed to disagree on it.**
- **An inserted entry offers no Place text**, and says why: *"Already in the field. Placing
  it again would put the same words there twice."* G14's post-conditions already forbid
  duplicated text in the target field, so offering the button would invite a gate failure.
- **Dismiss and Delete are two different verbs, and only one destroys anything.**
  - **Dismiss** stops offering recovery and **keeps the stored text**. It is reversible, with
    Undo on screen. The entry stays listed and reads `Abandoned`, never `Inserted`.
  - **Delete** removes `raw_text` and `final_text` **together** (the privacy policy's
    "marked deletable together") and cannot be undone. It is the separately labelled
    destructive button, never the same row as Dismiss with only a colour difference.
  Dismiss is what the ticket's "cleanly abandonable" means; Delete is what
  Settings → Privacy → Clear all history means for one row.
- **A cancelled or refused placement changes nothing about the entry.** The outcome does not
  move; `retry_count` and `last_attempt_at` record that an attempt happened and was
  cancelled. A cancelled tap must not silently resolve a failure the user never resolved —
  otherwise the entry stops being recoverable because the user changed their mind about a
  *destination*, which is a different decision.
- **A failure with no transcript offers Delete and nothing else** — no Place text, no Copy,
  because there is nothing to place or copy — and says so: *"There is nothing to place, copy
  or recover. This dictation captured no words."* No entry in History offers audio at all;
  recordings are deleted after processing, and a play button here would be a second
  retention path in the one place nobody was looking.

### Not settled here — needs the owner

Which variant is the History surface: **A** (ledger, recovery is a section), **B** (flat,
recovery is ordinary), or **C** (inspector, recovery needs a pane). A and C both privilege
recovery; B refuses to. See the recommendation in the comments.

### The schema question this surfaced, and it is a real one

Ticket 14's schema has `raw_text?` but **NOT NULL `final_text`**, and ADR-0002's
`ErrorRecoverable(reason, transcriptId?)` admits a failure with no transcript. **As settled,
History cannot hold a textless failure at all.** Either no row is written when nothing was
captured, or `final_text` has to become nullable. The prototype toggles this rather than
picking, because it is a storage decision wearing a UI question's clothes and it should be
answered on its own terms. It is recorded here and not yet ticketed.

### 2026-10-05 — Three checks that only failed because they were damaged first

The prototype audits its own rendered surface for the ticket's honesty rules, and that
audit is worth a note on its own, because **its first version could not fail at all.**

It compared `limitsFor()` output against `LIMITS.place` — and `limitsFor()` reads
`LIMITS.place`, so the check agreed with itself by construction. Sabotage run 1, stripping
the limit sentence off a Place text button, left it reporting "Consistent". This map has now
recorded the same shape seven times (`docs-check`'s original `paths:` filter, requiring a
check nobody had seen pass, the uncalibrated latency threshold, ticket 44's half-landed
count, the keyframe-held resting style, the sub-AA pressed cross-fade, `apksigner verify`'s
too-weak claim), and this is the first where **the check was written to be right and was
merely tautological** — it would have shipped green forever.

It now reads the rendered DOM, and every one of the six rules was confirmed to fire when the
surface is damaged deliberately: removing the limit, renaming a row to claim a field it is
not in, adding a playback button, offering Dismiss with Delete removed, adding Copy to a
textless failure, and mislabelling an unprocessed entry. The generalisable move is the one
already in the map: **make the check compare against the thing it claims to test, not against
its own success** — and then break the thing on purpose to confirm it notices.

Two more from the same pass, both the same shape:

- **A container query has to be measured on the element that scales.** Variant C's two panes
  collapsed to a sliver at 200% text on a 420px screen, because the reflow keyed on viewport
  width while the *content* scaled in rem. It now keys on the phone's own inline size. This is
  the map's recorded "a threshold expressed in a unit the measured thing also scales in looks
  scale-invariant and is not".
- **A contrast auditor is a checker too, and mine was measuring itself.** It reported 214 of
  620 roles failing at 1.06:1 — ticket 37's black-on-black figure, arriving from the wrong
  direction. The parser did `.slice(0,3)` on `rgba(0,0,0,0)`, so every transparent ancestor
  read as **opaque black** and the ground walk stopped on the first one. Fixed, then
  confirmed to still fail when `--low`, `--ink` or the accent chip is deliberately damaged —
  because a checker fixed three times is exactly the one that stops being trusted.

Primary source: branch `prototype/history-recovery-surface`, commit `4186bff`, asset
[History recovery surface prototype](../prototype/history-recovery-surface.html). The commit
captures only that one self-contained HTML file, authored as the repository owner with the
`Co-authored-by` trailer AGENTS.md requires, and leaves the shared working branch and index
untouched — 90 other in-flight files verified still present and unstaged afterwards.

## Question

The explicit destination handoff for History retry is settled, but the History surface
that leads to it remains unspecified. Prototype how a user recognizes and inspects an
entry after recoverable insertion failure, and how raw text produced by refiner failure
is visibly marked unprocessed. Distinguish these from a successfully inserted entry;
an unprocessed transcript is not automatically an insertion failure.

Decide the entry summary, full-text inspection, status/reason copy and available actions.
Show the settled Place text flow's entry point, copy recovery and how an entry is cleanly
abandoned. Clarify what dismissing a recovery affordance means relative to deleting the
saved transcript. Include a failure with no available transcript and the return from a
cancelled or refused placement attempt. Do not imply that text has already been inserted
when it remains recoverable or that retry will re-transcribe or rerun the refiner.

Preserve stored text, the chosen Room/retention contract, privacy rules and the settled
destination flow. Use the existing design language and contextual permission pattern.
This decides the History recovery surface, not production implementation or a new
Dictation state machine.

Context: [History & recovery storage](14-history-storage.md),
[Dictation state machine](05-dictation-state-machine.md),
[How the acceptance gate is run and recorded](28-acceptance-gate-protocol.md), and
[How does History retry choose a new destination?](61-history-retry-destination.md).

## Comments

### 2026-10-05 — Variant prototype ready for review

Asset: [History recovery surface prototype](../prototype/history-recovery-surface.html).
Open the single self-contained HTML file directly; state is in memory and the fonts are
embedded, so nothing is fetched. `?variant=A|B|C` or the arrows at the foot switch, and the
surface itself is simulated — no Android, no Room, no microphone.

Three variants, and they disagree about a different thing each:

- **A · ledger** — recovery is a **section**. "Needs you" is hoisted above an archive, and
  its entries carry their actions inline; everything else is a collapsed row you open. Tests
  whether a section boundary can be the affordance on its own.
- **B · flat** — recovery is **ordinary**. One chronological column, no hoisting, every row
  expands in place, and the actions are identical in kind whatever the state. Tests whether
  hoisting is a judgement this app has any basis to make.
- **C · inspector** — recovery needs **room**. A narrow list beside a pane holding the
  reason, the limit and the actions. Tests whether a disclosure triangle can hold an
  explanation at all.

**Recommendation: A.** Three reasons, and one cost.

A hoists because the three states are not equally time-sensitive, and A is the only variant
that says so with a rule rather than a style. A row that cannot be acted on needs no
attention; a row that can is the only thing on this screen a person came for, and burying it
in a chronological list means the surface's whole job is invisible until you happen to scroll
to it. B has the sharper objection, and it is a good one: a band called "Needs you" is a
claim about what needs you, and the app can only compute that claim from
`outcome`/`dismissed` — so the band is exactly as honest as the schema underneath it. A is
still right because the band is derived from a stored flag rather than inferred at render
time, and it is therefore recomputable by anyone reading the row. C is the best *detail*
surface and the wrong *list*: at six entries the pane is roomy, but a History that is meant
to grow to hundreds puts every lookup behind a selection, and the one fact that matters —
has this landed — is the one thing the nav cannot show at a glance.

The cost, stated rather than discovered later: A's ledger is a **second representation**, so
"Needs you" and "Everything" are two views of one list and they can disagree — dismiss
enough entries and the same entry is in both bands, which is what variant A does here
(dismissed entries read `Abandoned` and stay listed). That is a real maintenance cost and it
is the argument for B.

Two things worth knowing before flipping:

- **B's weakness is visible on purpose.** A `Not inserted` row and an `Inserted` row differ
  only by a small mono word at the head. That is B's argument made rather than asserted —
  if you cannot tell them apart in the screenshot, B is answering the question badly.
- **C's weakness is the reverse**: it shows the most, and most of what it shows is a
  transcript the user already dictated.

Cases the prototype runs, all eight to completion: recognise a recoverable entry; unprocessed
text that landed; an entry bad at both; a failure with no transcript; the return from a
cancelled placement; dismiss-is-not-delete; a plain inserted entry; and an entry recovered
from History into a different app than the original.

**Browser verification:** 80 assertions across 3 variants × 2 themes — all eight guided cases
run to completion, the 48dp floor **probed** by walking outward from each control's box rather
than added up, **620 text roles all WCAG AA** in both themes (lowest 5.91:1), no horizontal
overflow at 390px or at 200% text, no duplicate DOM ids, no script errors, and variant
switching working by URL and by keyboard. The prototype also carries a live honesty audit
that reads the *rendered* row; see the note below on why that mattered.

The textless-failure case is behind the **"Textless failures"** toggle at the top, because
the storage answer is not this ticket's to give.

No owner verdict yet, so the ticket stays unresolved and the production contract is unchanged.
