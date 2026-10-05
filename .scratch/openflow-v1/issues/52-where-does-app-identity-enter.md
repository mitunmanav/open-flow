# Where does app identity enter the pipeline?

Type: grilling
Status: resolved
Blocked by: none

> **Graduated from ticket 37** (`.scratch/openflow-v1/issues/37-dictionary-snippets-styles-surface.md`),
> finding 1. Ticket 37 shipped a surface that is entirely per-app, and this is the reason that
> surface cannot be built yet.

## Question

Ticket 37 decided that dictionary entries, snippets and style overrides are **per-app**. Nothing
in the pipeline can honour that, because **nothing in the pipeline knows which app is being
dictated into.**

The premise ticket 37 was chartered on was wrong, and it is worth stating precisely what is wrong:

- **`RouterContext` carries no app identity.** ADR-0004 gives it `privacyMode`, `offline`,
  `costCeilingMicrosUsd`, `preferredProviderId`. That is the whole list. The ticket assumed the
  router already assumed an identifiable app; it does not.
- **`TranscriptRefiner` is not told either.** Its stage 8 says styles are "assigned per app
  category", and its stages 6 and 7 (dictionary, snippets) say nothing about scope at all. There is
  no app parameter to match an entry against.
- **The only place a package is known today is the inserter.** ADR-0003 captures the target at
  recording start — package, view id, input type, multiline, selection — and re-resolves by
  package + characteristics at insertion. That is real, it works, and it is **the inserter's
  private concern at the end of the pipeline**. It is not a router input, not a refiner input, and
  it is not available at `PREPARING` when a routing decision is made.

So the question is where a **target-app identity** enters, and the candidates are genuinely
different:

- **Extend `RouterContext`** with a target package. It is the existing context object for one
  routing decision, but the router does not need the app to choose a provider — adding an input
  nothing reads is a field that lies.
- **Extend the refiner's request**, separately from the router. The refiner is the only thing that
  would actually read it, so this may be the honest home — but it means the controller builds a
  second context object beside the first.
- **Make it part of the Dictation itself**, captured at the tap like the inserter's target, and
  carried on the single event channel ADR-0002 already has. Most faithful to "we know where we are
  typing", and the most invasive.
- **Something else entirely**, including deferring per-app scope out of V1 — which would contradict
  ticket 37 and should be argued for explicitly if anyone wants it.

Three consequences worth deciding in the same breath, because they follow from the answer:

1. **What is the identity — package name, or the re-resolved characteristics?** ADR-0003 already
   prefers *package plus characteristics* over package alone, so "which app" may not be one value.
2. **The inserter re-resolves at insertion time, which is later.** If the user starts dictating in
   Gmail and the field changes before insertion, whose app was the snippet for? Probably the one at
   the **start**, because that is what the user chose when they pressed — but it needs saying.
3. **`refiner.md` stage 8 says "per app category" and no taxonomy exists.** Ticket 37 settled
   per-*package* instead, so that sentence is now stale and owes an amendment regardless of how this
   resolves.

## Why this cannot be answered by looking at the code

`core/` has no `src/` directory at all. ADR-0001's contract is prose, and
`SherpaOnnxContractProbe.kt` says outright that the real implementation "arrives with the feature".
So there is nothing to read and nothing to run — this is a decision about a contract, and the
contract is the thing being decided. **It is the same class of problem as ticket 42: unblocked on
paper, and not takeable until something exists.**

Read `docs/adr/0004-adaptive-dictation-router.md`, `docs/adr/0002-dictation-state-machine.md`,
`docs/adr/0003-safe-text-insertion.md` and `docs/architecture/refiner.md` first, and
`GLOSSARY.md` for vocabulary.

## Comments

### 2026-10-04 — First round accepted

The owner accepted all three recommendations:

- The Dictation Controller owns a Target Snapshot captured before recording, alongside
  the sensitive-field check. The refiner receives its package name; the Safe Text Inserter
  receives its field characteristics.
- Package name identifies the app for dictionary, snippets and styles. Field identity is
  a separate insertion-safety concern. RouterContext gains no app input.
- Changing the active app does not change which app's settings apply to the Dictation.
  Insertion still re-resolves and validates the original target, with recovery if it cannot
  match safely.

The term Target Snapshot has been added to the glossary. Missing-target behaviour and
recovery re-insertion are the next round; this ticket is still open for that exchange.


## Answer

Resolved through two live rounds on 2026-10-04: the owner accepted all recommendations.

### Ownership and inputs

The Dictation Controller owns an immutable Target Snapshot for the Dictation. Capture it
before recording, alongside the sensitive-field check, using the same target observation
for both. It contains the original package name and the insertion characteristics already
specified by Safe Text Insertion (view id/resource name, input type, multiline, selection).
The controller retains it across state transitions; components report through the existing
DictationEvent channel rather than changing it themselves.

The controller supplies the original package name and frozen applicable dictionary,
snippet and style settings to the refiner's request. The Safe Text Inserter receives the
original target information for re-resolution. RouterContext and speech-provider inputs
gain no app identity: provider selection does not need this information.

Package name determines per-app settings; field characteristics determine safe insertion.
At the start, freeze the applicable settings as well as the target. Editing a snippet or
style while recording affects the next Dictation, not the one already underway. A change
of active app never changes the current Dictation's settings or redirects its insertion.

### Missing or changed targets

If no focused writable field is available, or its package cannot be identified, refuse
to start, keep the microphone closed, and prompt the user to focus a writable field.
The existing accessibility-binding and sensitive-field refusals still apply.

At insertion, re-resolve the focused editable node and compare it with the original Target
Snapshot. If it cannot match safely, do not insert into the new field; retain the transcript
and offer recovery under the existing insertion contract.

### Recovery

History retry re-inserts stored text unchanged: no re-transcription, re-expansion or
reformatting for the destination app. The user explicitly chooses the recovery destination,
which gets a fresh Target Snapshot and fresh writable/sensitive-field checks, followed by
re-resolution before insertion. Missing or unsafe destinations leave the entry recoverable.
The original package stored in History is provenance, not permission to insert or authority
to apply new settings.

The interaction for choosing that destination is now a separate question:
[How does History retry choose a new destination?](61-history-retry-destination.md).

### Contract updates and handoff

The controller and insertion ADRs, TranscriptRefiner contract, and History ticket now
reflect these decisions. Stage 8's stale app-category wording is corrected to per-package.
The glossary defines Target Snapshot. Production implementation remains a handoff; this
resolution does not add pipeline code.
