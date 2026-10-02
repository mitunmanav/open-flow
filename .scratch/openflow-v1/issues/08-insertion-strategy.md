# Insertion strategy & sensitive-field policy

Type: grilling
Status: resolved
Blocked by: 03

## Question

How does insertion work across app surfaces (standard EditText, WebView, custom views), what field characteristics are captured at recording start, how is the cursor restored, what counts as a sensitive field to refuse, and what is the copy/paste recovery flow?

## Answer

Insertion settled (detail in `docs/adr/0003-safe-text-insertion.md`):

- Layered: `ACTION_SET_TEXT` → `ACTION_PASTE` → copy-to-clipboard + paste prompt.
- Clipboard preserved across the paste fallback (best effort).
- Target captured at recording start, re-resolved at insertion by package+characteristics; if gone → no insert, bubble Problem, transcript kept, copy offered.
- Cursor restored after SET_TEXT; paste fallback limitation documented.
- Sensitive fields refuse *before* recording: password inputTypes, `isPassword`, banking/finance package blocklist (defaults + user-editable), password/otp hints.
- Overlay never steals focus (`FLAG_NOT_FOCUSABLE`).
- Accessibility service must be bound before dictation start.
- V1 instrumented test editor app covers EditText, WebView, custom view, password field, and field-gone-mid-dictation.
