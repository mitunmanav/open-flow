# How the bubble overlay window is actually built

Type: grilling
Status: resolved
Blocked by: none

Graduated from the map's fog, where it waited for the Android shell to exist. It does
now, as of the Gradle skeleton.

## Question

Ticket 07 settled what the bubble **does** — tap, hold, drag, cancel, fully
customisable shape, size, opacity and position — and ticket 20 is claiming the motion.
Nothing has been said about the window itself, and everything about it is an Android
platform question rather than a design one:

- **The overlay window's actual type and bounds.** `TYPE_APPLICATION_OVERLAY` is the
  obvious choice, but its size, flags, and whether it is focusable or touch-modal
  decide whether a tap on the bubble can be distinguished from a tap on the app
  underneath it.
- **What happens when the world moves.** Rotation, the keyboard opening and closing,
  a configuration change, the display being folded or resized. An overlay window's
  bounds can end up off-screen or stale. Does the bubble re-anchor on every change,
  and does it remember a position set before rotation?
- **Screenshots and screen recording.** An overlay can end up in a screenshot of the
  app it floats over. Does V1 exclude itself, and at what cost — `FLAG_SECURE` on
  somebody else's window is not available, so the honest options are limited.
- **The accessibility service, and the disclosure that comes with it.** Ticket 08
  settled re-resolving the target field by package name and window characteristics
  and `BIND_ACCESSIBILITY_SERVICE`. Nothing has been said about how that service is
  surfaced to the user, which is a Play-policy matter, not only a technical one.
- **The notification question the manifest currently has no answer for.** Ticket 03's
  permission list covers `RECORD_AUDIO`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_MICROPHONE` and `SYSTEM_ALERT_WINDOW`. It does **not** cover
  `POST_NOTIFICATIONS`, and `minSdk` is 26 while `targetSdk` is 36 — so on Android
  13+ the foreground-service notification a bubble app depends on is invisible unless
  that permission is declared and granted. The skeleton's manifest declares exactly
  what ticket 03 listed, which is how the gap became visible. Declaring it is a
  decision, because on this product it must be justified contextually like every other
  permission rather than bundled into a first-launch prompt.

## Answer

**How these were decided, because it changes how they should be read.** This is a `grilling`
ticket, which is supposed to resolve through a live exchange with the owner. It did not: the
owner was asked how to proceed and answered *"decide it for me."* So the decisions below are
**the agent's judgement carrying the owner's sign-off, not decisions the owner made.** They
are recorded that way on purpose. Anything here can be overturned by the owner without
disturbing the rest, and the ones most worth a second opinion are the three deliberate
non-obvious choices in `docs/adr/0011-bubble-overlay-window.md` — no `FLAG_SECURE`, no
accessibility-service keyboard watch, and an Anchor instead of coordinates.

**One correction to the ticket's own premises**, both found while answering it:

- It asks "whether it is focusable", but ADR-0003 settled `FLAG_NOT_FOCUSABLE` before this
  ticket was charted. The question was a decision behind the repo.
- `WindowManager`, `LayoutParams`, `Gravity` and `FLAG_NOT_TOUCH_MODAL` had **zero
  occurrences** anywhere in the repository, so nothing here was constrained by existing code.
  That is a clean slate, not an oversight to worry about.

**Its Play framing is moot.** The bullet calls the accessibility disclosure "a Play-policy
matter, not only a technical one". The map rules a Play release out of scope (ticket 33), so
the Play Console declaration and its demo video are not V1 gates. What survives is the
disclosure itself, and it is kept — not because Play wants it, but because the honesty rule
does.

### Lifecycle — three states, and the ticket never named this

The root of the tree was missing from the ticket body. V1 ships:

- **Disabled** — no window, nothing running.
- **Idle** — window present. No service, no notification.
- **Dictating** — window plus the microphone foreground service and its notification.

Enabled/idle is what makes ticket 07's settings coherent: position memory, shape, size and
opacity all configure a window that is not present in the Disabled state, and in an
ephemeral design they would configure nothing at all. The toggle lives in the Bubble group
ticket 10 already settled. Enabled state survives process death, and the window is
re-attached by a component observing that flag — not by an Activity.

### Flags

`FLAG_NOT_TOUCH_MODAL` **set** — without it the window is touch-modal, and with the live
transcript sharing the window's bounds that would swallow every touch aimed at the app
underneath the transcript. `FLAG_NOT_FOCUSABLE` **set** (ADR-0003). `FLAG_NOT_TOUCHABLE`
**never**. `FLAG_WATCH_OUTSIDE_TOUCH` **not set** — nothing needs it, and not asking widens
what the window is entitled to observe. `TRANSLUCENT`. Size follows content.

The transcript lives in the **same window**, not a second one: one window means one z-order
and one lifecycle, which is exactly what OEM hostility punishes, and `FLAG_NOT_TOUCH_MODAL`
makes the dead zone harmless rather than broken. Bounds follow the content through
`updateViewLayout`, **coalesced per frame rather than per partial** — otherwise a fast
recogniser drives hundreds of relayouts a second. The transcript extends *inward* from the
Anchored edge and flips to whichever side has room.

### Anchor — an edge plus a margin, never coordinates

Stored as **which edge or corner, plus a margin in dp**. On drag release it snaps to the
nearest edge or corner. Bounds are re-derived from current display metrics on every attach
and every configuration change, and on a fold the same rule applies — clamp to the display
in front of you, and do not get clever about the hinge.

This dissolves the ticket's own question, *"does it remember a position set before
rotation?"* — the stored value is rotation-invariant by construction, so there is nothing to
remap and no stored position to migrate.

**The cost, stated rather than discovered: the bubble cannot rest at an arbitrary interior
position in V1.** Dragging snaps to an edge. That is the price of a position that means the
same thing on three device classes, and `Device Class` exists precisely because those three
disagree.

### Rotation, and the gate's open question

Dictation **survives** rotation. Rotation is not a cancellation trigger: ADR-0002's states
are payload-carrying and not orientation-scoped, the microphone service keeps running, and —
the part that matters — **insertion cannot break on rotation**, because ADR-0003 captures the
target as package plus characteristics and re-resolves it that way, never as screen
coordinates. G12's rotation variant is now written into `docs/quality/acceptance-gate.md`
and ADR-0008's "unspecified" note is corrected.

### Keyboard

The bubble **rides above the keyboard**, because a push-to-talk control hidden behind the
keyboard is unusable at the exact moment it is needed — the user is composing text.

The mechanism is the overlay reading **its own** IME insets. The accessibility service is
deliberately **not** used to watch the keyboard: every reference app does it that way, and it
would widen a service we promise is "for text insertion only", behind the same permission the
user grants to write into a field. Same reasoning retires OpenWhispr's periodic focus poll.

Where a skin does not deliver insets to an overlay, the fallback is **stay put** — not hide,
not jump. Whether insets arrive at all is a per-OEM unknown, so **it is now a fourth Device
Class discriminator**, and G12 records which behaviour the run saw instead of choosing
between them.

### Screen capture — accepted and disclosed, and this is the surprising one

No `FLAG_SECURE`, ever, on the bubble. `FLAG_SECURE` suppresses capture for the display
region our window touches, so on most OEM implementations the bubble would degrade **every
other app's** screenshots and recordings while it was up. We cannot secure another app's
window — that is not ours to do — so the only outcome available is making everyone else
worse. It extends a rule the repo already holds: do not suppress or mislead the mic privacy
indicator, so do not defeat capture either.

Two consequences: the **idle** bubble must carry nothing sensitive, and the disclosure has to
exist so nobody can claim "OpenFlow never appears in screenshots" on our behalf. Now in
`docs/privacy/privacy-policy.md`.

### Accessibility observation scope

`TYPE_VIEW_FOCUSED` **+** `TYPE_WINDOW_STATE_CHANGED`, not `typeAllMask`. Neither alone
suffices: WebViews and custom editors commonly signal only the window change, so
`TYPE_VIEW_FOCUSED` alone would miss a supported field and push the user to the copy
fallback. Node from `event.source`; `rootInActiveWindow.findFocus(SEARCH_FOCUS_INPUT)` only at
insertion time. Captured at dictation start is package, view id, input type and selection —
never content.

The consequence that changed a document: `ACTION_SET_TEXT` needs an `AccessibilityNodeInfo`,
so the service **structurally can** read the focused field's text. "We do not read your
text" is a **code-level discipline, not a platform guarantee** — so `privacy-policy.md` now
says the capability exists and we decline it, instead of implying the platform prevents it.

### Disclosure surface

Built, even with Play out of scope — the honesty rule is not a Play artifact. Its **content**
is settled here; its **place in the flow** is handed to
[47 First-launch download and onboarding order](47-first-launch-download-and-onboarding-order.md),
which owns onboarding order. `isAccessibilityTool` stays unset (ticket 03).

The health screen gains two rows it did not have: the **Android 13+ restricted-settings
state** (a sideloaded accessibility service is hidden from Settings until the user allows
restricted settings — precisely when someone concludes the service is not there), and the
bubble's own enabled/disabled state, since a bubble the user switched off makes "ready to
dictate" false for a reason that is not a permission.

### `POST_NOTIFICATIONS`

**Declared**, and deliberately *not* folded into the microphone ask. With `targetSdk` 36 the
foreground-service notification is suppressed on Android 13+ unless it is granted, and the
service still runs — so the failure mode is an invisible microphone service, which is the
one outcome this project will not ship, and it contradicts both ticket 03's "visible
notification" and a claim **already published** at `website/privacy.html`.

It is a different kind of ask from `RECORD_AUDIO`: we want the microphone because we want to
listen, and we want this because **our own foreground service cannot exist visibly without
it**. Nothing is read, nothing is sent. **The channel carries the recording indicator and
nothing else, ever** — V1 posts no other notification, and that commitment is what makes the
ask honest once it is granted.

Requested at the moment of first dictation, the action that brings the service into
existence — never at first launch. **Denial does not block dictation**: the service runs and
the bubble is the recording indicator anyway, which is rather the point of a bubble product.
The health screen marks the row as not required for dictation. No re-ask, no nagging.
Residual risk, deliberately left open: a skin may kill a service whose notification is
suppressed. That is the gate's to measure, not ours to decide.

### Landed

`docs/adr/0011-bubble-overlay-window.md` (new) · `docs/privacy/privacy-policy.md` (two new
sections) · `docs/privacy/permissions-policy.md` (§2, §3, §4, §5) · `GLOSSARY.md` (**Anchor**
added; **Bubble** rewritten to drop the implementation detail and name the three states and
the Anchor; **Device Class** extended) ·
`docs/quality/acceptance-gate.md` (G12 rotation + keyboard) ·
`docs/adr/0008-acceptance-gate-two-bars.md` ("unspecified" corrected) ·
`app/src/main/AndroidManifest.xml` (`POST_NOTIFICATIONS` declared, scope comment corrected).

`website/get-involved.html` already published "across rotation and keyboard resize"; that
claim is now backed rather than outstanding.