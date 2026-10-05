# Correct the permissions list that lists a download as a permission

Type: task
Status: blocked:human
Reason: Awaiting owner decision on deleting the model row or moving it outside the permissions list.
Blocked by: none

## Question

[Onboarding & permission-health flow](19-onboarding-permission-health.md) is resolved, and
its prototype at `.scratch/openflow-v1/prototype/onboarding.html` is its record. That record
contains a row that is now wrong:

```js
{ id:'model', name:'Voice model', does:'45 MB once, then offline', nots:'never uploaded', bytes:true },
```

It sits in the **permissions** list. A model download is not a permission: it requests no
capability, grants no ongoing access, appears in no Android permission dialog, and is not
in `docs/privacy/permissions-policy.md`. [Where the first-launch model download sits in the
onboarding flow](47-first-launch-download-and-onboarding-order.md) settled that it is a
blocking first-run *step*, and
[ADR-0013](../../../docs/adr/0013-first-launch-order.md) records the reasoning: putting it in
a permissions list borrows that screen's context to make a 45 MB download feel smaller than
it is.

So the row has to go from the permissions list. That is the easy half, and it is not the
whole ticket.

**What to decide:**

- **Delete it, or replace it with an accurate statement?** The permissions page is where a
  user looks to find out what OpenFlow touches. A row saying the app fetches 45 MB once and
  never uploads audio is *true and reassuring* — it is just filed under the wrong heading.
  Moving it to a "what else this app does" position, or to
  `docs/privacy/permissions-policy.md` where it belongs, may serve the user better than
  deletion. The prototype's own header says it is "a permissions page, not a product demo",
  which argues for deletion — but the row also discharges a promise the page otherwise does
  not make.
- **Does anything in `docs/privacy/permissions-policy.md` need the same correction?** The
  first-launch fetch is a network transfer the privacy policy may or may not currently name.
  If it does not, that is a gap in a document rather than in a prototype, and it is the
  half of this ticket that actually reaches users.
- **The 45 MB figure is now two figures.** [ADR-0013](../../../docs/adr/0013-first-launch-order.md)
  states 45 MB fetched and **~175 MB free** while it unpacking, and
  [Is 175 MB the headline?](56-is-175-mb-the-headline.md) is open. Correct this row now
  with both numbers, or wait for that ticket and copy its wording — copying a number that
  is about to be re-decided is how documents drift.

**Why this is its own ticket rather than an edit to ticket 19's asset:** that file is a
resolved ticket's record. Editing it in passing would rewrite the evidence for a decision
this map has already made, and nothing in the commit history would say the asset changed
underneath its own ticket.

## Answer

Not started.

## Comments

### Investigation — 2026-10-04

The task contains an unresolved presentation decision, so it is parked for the
owner rather than resolved without their input.

- CodeGraph does not index the onboarding prototype; inspected its model row
  and the original resolution in **Onboarding & permission-health flow**.
- The prototype includes model completion in its permission state and count.
  The original pinned copy also promises “no network”; removing only the row
  would leave misleading copy and behaviour.
- The Model Store downloads the entire pinned archive, then selectively extracts
  the required files. **A ModelStore that fetches, verifies and hands over a model
  by path** and `docs/providers/sherpa-onnx.md` record the archive as 127,887,156
  bytes: about 128 MB transferred, about 45 MB extracted, about 175 MB temporary
  free space. ADR-0013 and **Is 175 MB the headline?** conflate transfer and
  extracted size; the next discussion needs these corrected facts.
- `docs/privacy/permissions-policy.md` has no model permission row to remove.
  `docs/privacy/privacy-policy.md` does not disclose the model fetch.

Recommendation for owner decision: remove the model row and its state/count/meter
from the permissions prototype, correct associated copy, and disclose the opt-in
GitHub model fetch in the privacy policy, cross-linked from the permissions
checklist. Keep numerical headline choice with **Is 175 MB the headline?**; use
accurate transfer/storage labels wherever factual corrections are made. Preserve
the original resolution as history and append a correction pointer when resolved.
