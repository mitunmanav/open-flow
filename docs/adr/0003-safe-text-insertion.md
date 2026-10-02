# 0003: Safe text insertion

Date: 2026-10-02

## Status

Accepted

## Context

Insertion is the least reliable layer across arbitrary Android editors. Some editors expose standard `EditText` nodes; others (WebView, custom views) don't. Wispr and all reference projects fall back to clipboard paths, and insertion must never leak into sensitive fields or steal focus.

## Decision

Layered insertion strategy, in order:

1. `ACTION_SET_TEXT` on the re-resolved writable node.
2. `ACTION_PASTE` with the prepared text pushed to the clipboard.
3. Copy text to clipboard + prompt the user to paste.

Rules:

- **Sensitive-field refusal happens before recording starts.** If the focused field is already sensitive at bubble tap, dictation refuses to begin (bubble message; mic stays closed). Sensitive = inputType `TYPE_TEXT_VARIATION_PASSWORD/VISIBLE_PASSWORD/WEB_PASSWORD`, `isPassword == true`, package prefix blocklist (ships with defaults: banking/finance; user-editable), or node hint text like "password"/"otp".
- **No focus stealing**: overlay is `FLAG_NOT_FOCUSABLE`; the service never calls `requestFocus`.
- **Target captured at recording start** (package, view id/resource name, input type, multiline, selection), then **re-resolved at insertion** by finding the focused editable node and comparing package + characteristics. If the original field vanished → no insertion; bubble Problem; transcript kept; copy offered.
- **Cursor**: after SET_TEXT succeeds, selection moves to just after the inserted text. The paste fallback can't guarantee cursor placement — documented limitation.
- **Clipboard**: paste fallback saves the previous clipboard first; restore after the paste attempt resolves (best effort; documented that an immediate manual copy can clobber a restore).
- **Accessibility service must be bound** before a dictation can start; otherwise bubble shows a setup prompt (permission-health screen already covers this).
- V1 ships an instrumented test editor app: standard EditText, WebView, custom view, password field, field-gone-mid-dictation.

## Consequences

Insertion failures degrade to copy/paste recovery paths rather than dropping text; sensitive fields are blocked pre-recording; the surrounding app's focus is never stolen.
