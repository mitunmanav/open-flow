# 0003: Safe text insertion

Date: 2026-10-02

## Status

Accepted — amended 2026-10-04: target capture ownership, missing-target refusal and explicit recovery destinations; see [Where does app identity enter the pipeline?](../../.scratch/openflow-v1/issues/52-where-does-app-identity-enter.md).

Amended 2026-10-05: explicit History placement and confirmation flow; see
[How does History retry choose a new destination?](../../.scratch/openflow-v1/issues/61-history-retry-destination.md).

## Context

Insertion is the least reliable layer across arbitrary Android editors. Some editors expose standard `EditText` nodes; others (WebView, custom views) don't. Wispr and all reference projects fall back to clipboard paths, and insertion must never leak into sensitive fields or steal focus.

## Decision

Layered insertion strategy, in order:

1. `ACTION_SET_TEXT` on the re-resolved writable node.
2. `ACTION_PASTE` with the prepared text pushed to the clipboard.
3. Copy text to clipboard + prompt the user to paste.

Rules:

- **Sensitive-field refusal happens before recording starts.** If the focused field is already sensitive at bubble tap, dictation refuses to begin (bubble message; mic stays closed). Sensitive = inputType `TYPE_TEXT_VARIATION_PASSWORD/VISIBLE_PASSWORD/WEB_PASSWORD`, `isPassword == true`, package prefix blocklist (ships with defaults: banking/finance; user-editable), or node hint text like "password"/"otp".
- **Missing-target refusal**: if there is no focused writable field or its package cannot be identified, refuse start with the microphone closed and prompt the user to focus a writable field.
- **No focus stealing**: overlay is `FLAG_NOT_FOCUSABLE`; the service never calls `requestFocus`.
- **Target captured before recording** as a controller-owned immutable Target Snapshot (package, view id/resource name, input type, multiline, selection), from the same observation used for the sensitive-field check, then **re-resolved at insertion** by finding the focused editable node and comparing package + characteristics. If the original field vanished → no insertion; bubble Problem; transcript kept; copy offered.
- **Target changes never redirect insertion**: the Dictation retains its original app settings and destination; a focused field in another app is not an automatic replacement target.
- **History recovery is an explicit new insertion attempt**: the user chooses its destination. Capture a fresh Target Snapshot and perform writable/sensitive-field checks there, then re-resolve before inserting the stored text unchanged. Do not re-transcribe, expand snippets again, or apply the new app's style. A missing or unsafe recovery target leaves the History entry recoverable. The destination-selection interaction is owned by [How does History retry choose a new destination?](../../.scratch/openflow-v1/issues/61-history-retry-destination.md).
- **Cursor**: after SET_TEXT succeeds, selection moves to just after the inserted text. The paste fallback can't guarantee cursor placement — documented limitation.
- **Clipboard**: paste fallback saves the previous clipboard first; restore after the paste attempt resolves (best effort; documented that an immediate manual copy can clobber a restore).
- **Accessibility service must be bound** before a dictation can start; otherwise bubble shows a setup prompt (permission-health screen already covers this).
- V1 ships an instrumented test editor app: standard EditText, WebView, custom view, password field, field-gone-mid-dictation.

### History destination selection

The approved interaction is **Place text → focus a field → Use here → Insert**:

1. **Place text** in History arms that saved entry. It captures no destination and does
   not navigate to the original app recorded in History.
2. The user opens the destination app and focuses a field. The idle Bubble offers
   **Use here** and **Cancel** in its existing non-focusable window. While placement is
   pending, its action places saved text rather than starting a recording. The microphone,
   speech provider and refiner remain unused.
3. **Use here** captures a fresh controller-owned Target Snapshot and performs writable,
   identifiable-package and sensitive-field checks. A same-window confirmation names
   the destination app and field, with **Insert**, **Change destination** and **Cancel**.
   The idle overlay shows no stored transcript. Missing or unsafe fields leave the entry
   recoverable and prompt the user to choose a usable field.
4. **Insert** re-resolves the selected destination and checks it again before writing
   stored text unchanged. A different app or field, vanished field, sensitive field or
   lost setup prevents that write. Clear the invalid snapshot; choosing again requires
   another explicit **Use here**. Focus changes never select a replacement automatically.

**Change destination** clears the selected snapshot and returns to choosing a field.
Cancellation clears pending placement, keeps the saved text in History and returns the
Bubble to idle. Successful insertion also clears pending placement, with Done feedback;
the pending confirmation cannot authorize another write. Process interruption clears
pending placement and its snapshot, while the stored entry remains available in History.

Unavailable Bubble/overlay setup or accessibility binding uses the existing contextual
setup path. Restore setup and explicitly select again; recovery does not request microphone
access. The layered insertion and clipboard rules above still apply after destination
checks. The browser prototype simulates direct insertion and does not verify Android
overlay behavior or those fallbacks.

## Consequences

Insertion failures degrade to copy/paste recovery paths rather than dropping text; sensitive fields are blocked pre-recording; the surrounding app's focus is never stolen.
