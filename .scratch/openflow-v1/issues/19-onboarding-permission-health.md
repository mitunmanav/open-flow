# Onboarding & permission-health flow

Type: prototype
Status: resolved
Blocked by: none

## Question

What exactly does a new user see from install to first dictation? Screens, copy, and deep links for: welcome/why-OpenFlow, mic permission, overlay permission (Android 11+ ignores `package:` deep links), Accessibility prominent disclosure + affirmative consent, model download/loading, and the permission-health screen (per-capability status rows, re-checked in onResume, "ready to dictate" summary). Prototype the flow and pin the copy.

## Answer

**Flow order was deliberately left open.** This ticket asked for a screen-by-screen flow; that was the wrong first move. Deciding the sequence before knowing what each permission *looks like* would fix a copy problem in the abstract. So the design work answered the visual and copy question, and the flow is still undecided — which also matches ticket 03's settled constraint that permissions are requested **contextually, not at first launch** (overlay "at the moment the user first enables the bubble"; mic "contextually tied to the user action").

**Design: brutalist.** Zero radius, no shadows, flat ground, one accent, hairline rules, 2px structural borders. Rationale: a permissions page is a form you don't want to be seduced by. Nothing on it should decorate.

Prototype: `.scratch/openflow-v1/prototype/onboarding.html`. Variant explorer that produced the final choices: `.scratch/openflow-v1/prototype/permissions-variants.html` (6 accents × 6 state marks, `?accent=…&mark=…`).

### The one memorable thing

**The granted state is the row's own perimeter drawing itself clockwise**, via a conic-gradient mask sweeping the border from the top. The row's outline arriving *is* the state — nothing is bolted onto the side of the row. An earlier build used a nested opaque box to punch out the interior; it painted over every row's text and blanked the list. The conic mask leaves the interior transparent so that cannot recur.

### Copy (pinned)

Each permission states what it touches **and** what it cannot do. The limit clause is set in mono against the capability clause in the UI face, so the honesty half of each line looks mechanically different from the marketing half:

| Ask | Does | Cannot |
|---|---|---|
| Microphone | hears you while you hold it | silent otherwise |
| Bubble | one circle over your apps | cannot see inside them |
| Dictation | reads the box you type in | nothing else on screen |
| Voice model | 45 MB once, then offline | never uploaded |

Plus one promise line: **"Four asks, and that is all. No location, contacts, photos, or network. Audio is deleted the moment it becomes text."**

Total 38 static words + 34 words of permission copy. Longest string 27 characters.

### Colour

**Electric violet** — `#5B21D6` light / `#A78BFA` dark. Chosen over six AA-qualified candidates (orange, yellow, magenta, cyan, violet, forest) and over the earlier cobalt. Verified WCAG AA for every text role in both themes (4.64–17.98:1), and CTA label on the accent fill at 7.97:1 light / 7.27:1 dark. Each theme gets its own accent value; a single hex fails on one side.

### Motion

Springs throughout, critically damped. The count is a real **odometer** at 116px that rolls on change — the single celebratory overshoot on the page (`zeta 0.88`); everything else stays critically damped. Row press feedback is instant (`.1s linear`), because brutalism should not feel floaty. `prefers-reduced-motion`, `prefers-contrast`, and `prefers-reduced-transparency` all handled.

### Structural devices that carry information

- The **numeral** counts granted; the status line beneath reports what is **left**, so the two work together instead of repeating each other.
- The **model row alone** carries a progress meter — it is the only ask with bytes in flight.
- **No numbered markers.** The four asks are not a sequence; numbering them would imply an order that does not exist.
- Rows are 76px, past the 48dp Material floor. Dynamic colour deliberately unused: a dictation app's accent should not shift with the wallpaper.

### Still open (deliberately)

- **Flow order** — untouched, per above.
- **Android 11+ overlay deep link.** Settled that the `package:` URI is ignored and the intent lands on top-level settings, so the copy must name the manual path (`Settings → Apps → Special app access → Display over other apps → OpenFlow`). That instruction does not live on this screen yet; it belongs to the moment of request.
- **Accessibility prominent disclosure** is required by Play to be a *separate* screen in normal usage flow with affirmative consent, and must not set `isAccessibilityTool`. This page states the limit but is not that disclosure — it needs its own surface when flow is decided.
- **In-app vs system prompt.** Rows currently toggle in place. In the real app a tap opens the corresponding system screen; the mapping is mic → app permissions, overlay → `ACTION_MANAGE_OVERLAY_PERMISSION`, dictation → `ACTION_ACCESSIBILITY_SETTINGS`.
