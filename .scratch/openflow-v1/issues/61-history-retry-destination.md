# How does History retry choose a new destination?

Type: prototype
Status: resolved
Blocked by: 14, 52

## Question

History retry inserts stored text unchanged into a destination explicitly chosen by the
user. Opening History puts OpenFlow in the foreground, so retry cannot simply assume the
currently focused field is the user's intended destination. What interaction lets the user
return to a writable field and explicitly commit that recovery insertion?

Prototype the destination-selection and confirmation flow. Preserve the controller-owned
Target Snapshot, fresh writable/sensitive-field checks and insertion-time re-resolution;
never steal focus or silently redirect. Show cancellation, no usable target, a sensitive
field and a target changing before insertion. The transcript remains recoverable when the
attempt cannot safely finish, and is never re-expanded or reformatted.

Keep the settled Bubble design and contextual permission pattern. This ticket decides the
interaction, not the production recovery implementation.

Context: [Where does app identity enter the pipeline?](52-where-does-app-identity-enter.md)
and [History & recovery storage](14-history-storage.md).

## Comments

### 2026-10-05 — Interaction prototype ready for review

Asset: [History retry destination prototype](../prototype/history-retry-destination.html).
Open the single self-contained HTML file directly. State is in memory, the fonts are
embedded, and Android apps/permissions are simulated. This is a logic prototype of the
handoff, not production insertion or proof of Android overlay behavior.

Proposed interaction:

- Choose **Place text** on a saved History entry. This arms that entry without capturing
  a destination or navigating to its original app.
- Open the desired app yourself and focus a field. The idle, non-focusable Bubble offers
  **Use here** and **Cancel**. While placement is pending its action places saved text
  rather than starting recording; microphone and refiner stay unused.
- **Use here** captures a fresh Target Snapshot and checks that field. A same-window
  confirmation names the app and field and offers **Insert**, **Change destination**
  and **Cancel**. It shows no stored transcript in the idle overlay.
- **Insert** re-resolves and checks the selected destination before writing stored text
  unchanged. A different app or field, missing field, sensitive field or lost setup
  prevents that write. Selecting again requires another explicit **Use here**.
- Cancellation and success clear pending mode and return the Bubble to idle. Process
  interruption clears pending authority; the saved entry remains available in History.
  Setup problems use the existing contextual Bubble/accessibility setup path and do not
  request microphone access. This prototype exercises direct insertion; the existing
  layered insertion/clipboard fallback contract remains a separate implementation concern.

Guided cases: successful placement, cancellation, no field, sensitive field, different
app, same app/different field, disappeared field, unavailable setup and process
interruption. Free play exposes every action and the full relevant state, including
the selected destination, actual focus, stored text, write count and History outcome.

Browser verification: all nine walkthroughs exercised; stale confirmations wrote
nothing, fresh selection permitted one insertion, stored text stayed unchanged and
password fields stayed empty. Additional checks covered accessibility lost after
confirmation and a duplicate Insert press (no write and no second write respectively).
At 390px both themes fit without horizontal overflow, visible buttons are at least
48px high, DOM IDs are unique and the browser reported no script errors.
Documentation integrity passed for all 43 checked Markdown files.

Recommendation: accept this explicit selection-plus-confirmation handoff. No owner
verdict yet; the ticket remains unresolved, with no change to the production contract.

### 2026-10-05 — Owner verdict

The owner agreed with the recommended selection and confirmation flow, including its
cancellation, refusal and interruption behavior. This completes the live review.

## Answer

Resolved through live prototype review on 2026-10-05. The owner accepted the recommendation:
**Place text → focus a field → Use here → Insert**.

- Place text arms the selected History entry without capturing a destination or opening
  the original app. The user opens the intended app and focuses its field themselves.
- The idle, non-focusable Bubble offers Use here and Cancel. Pending placement replaces
  its recording action; the microphone stays closed, and neither speech provider nor
  refiner runs. The stored transcript is not shown in the idle overlay.
- Use here captures a fresh controller-owned Target Snapshot and performs fresh writable,
  identifiable-app and sensitive-field checks. The existing window confirms app and field,
  with Insert, Change destination and Cancel.
- Insert re-resolves and checks the selected destination before inserting stored text
  unchanged. A changed app or field, disappeared field, sensitive field or unavailable
  setup prevents that write and clears the invalid snapshot. A fresh explicit Use here is
  needed to select again; changing focus never redirects insertion by itself.
- Change destination clears the snapshot and returns to field selection. Cancellation
  clears pending mode, retains the saved entry and returns the Bubble to idle. Success
  clears pending mode with Done feedback; the confirmation cannot authorize a second write.
- Process interruption clears pending authority and its snapshot while retaining the
  stored History entry. Setup uses the existing contextual Bubble/accessibility path;
  recovery does not request microphone access.

Why: History makes OpenFlow the foreground app. Separating entry selection from field
selection makes the new destination deliberate, and the final check prevents a stale
confirmation from writing to a different field.

Primary source: branch `prototype/history-retry-destination`, commit
`793c2fbc16b14d2dee6fb68ff14ea8c517d62b41`, asset
[History retry destination prototype](../prototype/history-retry-destination.html).
The commit captures only this self-contained HTML prototype, authored as the repository
owner. It leaves the shared working branch and index untouched.

Verification: nine browser walkthroughs plus lost-accessibility and duplicate-Insert
checks; stored text unchanged, refusals/cancellation preserved recoverability, mobile
layout and both themes checked, and no browser script errors. This models direct insertion;
Android overlay behavior and the existing layered insertion/clipboard fallback still
require production implementation and device verification.

The Target Snapshot glossary and controller/insertion ADRs reflect the approved flow.
Production implementation remains a handoff. The remaining History recovery surface is
now a separate decision:
[How does History present recoverable and unprocessed entries?](66-history-recovery-surface.md).
