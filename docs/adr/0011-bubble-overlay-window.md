# 0011: The bubble window — edge-anchored, touch-passthrough, and deliberately unsecured

Date: 2026-10-03

## Status

Accepted

Amended 2026-10-05: [Is the keyboard's insets behaviour a fourth Device Class question or a recorded observation?](../../.scratch/openflow-v1/issues/59-is-the-keyboards-insets-a-fourth-device-class-question.md)
settles keyboard behavior as contextual G12 evidence rather than a Device Class
discriminator. The own-window insets policy and accessibility-service limits remain.

Records the decisions in `.scratch/openflow-v1/issues/34-bubble-overlay-window.md`. Three of
them look like mistakes to a reader who does not know the reasoning, which is why they are
written down here rather than left in the ticket.

## Context

The Bubble is how OpenFlow exists on top of somebody else's app, so its window is a privacy
surface before it is a layout problem. ADR-0003 settled two things about it — `SET_TEXT` →
`PASTE` → copy+prompt for insertion, and no focus stealing via `FLAG_NOT_FOCUSABLE` — and
stopped there. Everything else was undecided, and the questions were genuinely open rather
than cosmetic:

- **When does the window exist?** ADR-0003 needs a focused target field, which implies the
  Bubble floats over another app; but ticket 07 settles position memory and shape, size and
  opacity customisation in settings, which imply a window that persists between dictations.
  Neither reading was written down.
- **What happens when the world moves?** Rotation, the keyboard, a configuration change, a
  fold. An overlay's bounds are absolute pixels in the display's coordinate space, so all of
  these can strand it off-screen.
- **Does it appear in screenshots?** The Bubble floats over other apps, so it can land in
  their captures.
- **How much may the accessibility service see?** `docs/privacy/permissions-policy.md`
  required narrow event types but named none, while every reference app in
  `docs/architecture/reference-apps.md` uses three redundant signals including a periodic
  focus poll.
- **Does the foreground-service notification need `POST_NOTIFICATIONS`?** The manifest
  declared exactly what ticket 03 listed, which is how the omission became visible: with
  `targetSdk` 36 and no such permission the notification is suppressed on Android 13+.

Nothing constrained the answers. `WindowManager`, `LayoutParams`, `Gravity` and
`FLAG_NOT_TOUCH_MODAL` had **no occurrences anywhere in the repository** — there is no
application code yet, so this was a clean slate rather than a set of constraints nobody
noticed.

## Decision

- **One `TYPE_APPLICATION_OVERLAY` window with three lifecycle states**: **disabled** (no
  window, nothing running), **idle** (window present, no service, no notification), and
  **dictating** (window plus the microphone foreground service and its notification). Only
  *dictating* runs a service.
- **`FLAG_NOT_TOUCH_MODAL` is set**, so touches that miss the Bubble reach the app
  underneath. `FLAG_NOT_FOCUSABLE` stays set per ADR-0003. `FLAG_NOT_TOUCHABLE` is never
  set. `FLAG_WATCH_OUTSIDE_TOUCH` is not set. The window is `TRANSLUCENT` and sized to its
  content.
- **The live partial transcript shares the Bubble's window.** Bounds follow content via
  `updateViewLayout`, coalesced per frame rather than per partial. The transcript extends
  inward from the Anchored edge and flips to whichever side has room.
- **Position is an Anchor — a screen edge or corner plus a margin in dp — never a
  coordinate.** Dragging snaps to the nearest edge or corner. Bounds are re-derived from
  current display metrics on every attach, every configuration change, and on a fold.
- **Dictation survives rotation**, which is not a cancellation trigger. On a configuration
  change the window is re-laid-out from the Anchor and nothing is re-persisted.
- **The Bubble rides above the keyboard**, read from the window's own IME insets. Where a
  skin delivers none it stays put — it does not hide and it does not jump.
- **No `FLAG_SECURE`, ever, on the Bubble.**
- **The accessibility service observes `TYPE_VIEW_FOCUSED` +
  `TYPE_WINDOW_STATE_CHANGED`** — not `typeAllMask` — takes the node from `event.source`,
  consults `rootInActiveWindow.findFocus(SEARCH_FOCUS_INPUT)` only at insertion time, and
  **neither watches the keyboard nor polls.**
- **`POST_NOTIFICATIONS` is declared**, requested at the moment of first dictation, on a
  channel that carries the recording indicator **and nothing else, ever**. Denial does not
  block dictation.

## Why these

**Three states, because the settings need a window to configure.** Ticket 07 settles
position memory plus shape, size and opacity customisation. In an ephemeral design — a window
created at dictation start and destroyed at the end — every one of those settings configures
a window that does not exist. Idle is also what the acceptance gate already assumes when its
post-condition allows "the overlay is gone, **or sitting in a defined idle state**". Disabled
is the third state and it is the load-bearing one: a floating control nobody can switch off is
a sharper edge than this product's honesty rule usually takes, so the product has to be able
to be *absent*, and its absence has to be the user's doing.

**`FLAG_NOT_TOUCH_MODAL`, because not-focusable is not not-touch-modal.** They are separate
flags and the second one appears nowhere in the repository. Without it the window is
touch-modal and swallows every touch inside its bounds that misses the Bubble — and since the
live transcript shares those bounds, the swallowed region is much larger than the control.
This flag is what makes the same-window choice safe rather than merely wasteful. Not setting
`FLAG_WATCH_OUTSIDE_TOUCH` is the same instinct in the other direction: nothing needs it, and
not asking keeps the window from being entitled to observe more of the screen.

**One window, not two.** Two windows means two z-orders and two lifecycles to keep alive
over an app we do not own — which is exactly what OEM hostility punishes, and `Device Class`
exists because the three required classes disagree about it. Coalescing relayouts per frame
rather than per partial is the cost of sizing the window to its content: a fast recogniser
emitting conflated partials would otherwise drive hundreds of relayouts a second.

**An Anchor, because a coordinate means nothing on the next display.** Storing pixels or a
screen fraction makes the position correct on the device that set it and arbitrary on every
other one — and running the same gate across three device classes is the whole coverage
strategy. Storing an edge and a margin in dp means the same thing everywhere, which
**dissolves the rotation question this ADR was partly chartered to answer**: there is nothing
to remap and no stored position to migrate. The cost is real and stated in `GLOSSARY.md`
rather than left to be discovered: **the Bubble cannot rest at an arbitrary interior
position**, because an interior position has no meaning once the display changes.

**Rotation does not cancel, and insertion cannot break on it.** ADR-0002's states are
payload-carrying and not orientation-scoped, so there is nothing to unwind. More usefully,
ADR-0003 captures the target as package plus characteristics and re-resolves it that way —
**never as screen coordinates** — so the rotation that moves the window cannot invalidate
the target the dictation started with. `docs/quality/acceptance-gate.md` G12 and ADR-0008
both carried a placeholder here; both are now written.

**Ride above the keyboard, and read it ourselves.** The Bubble is a push-to-talk control used
while composing text, so a bottom-anchored Bubble behind the keyboard is unusable at the one
moment it exists for. Every reference app watches the keyboard through the accessibility
service, and we deliberately do not: it would put keyboard observation behind the same
permission the user grants to write text into a field, and
`docs/privacy/privacy-policy.md` promises that API is for text insertion only. A promise
about scope is worth more than a slightly better target capture.

**No `FLAG_SECURE`, and this is the one that looks wrong.** The flag suppresses screen
capture for the display region our window touches, so on most OEM implementations the Bubble
would degrade **every other app's** screenshots and recordings while it was on screen. We
cannot secure another app's window — that is not ours to do — so the only outcome available
to a floating overlay is making everyone else worse. The principle was already in the repo:
`docs/privacy/permissions-policy.md` says not to suppress or mislead the microphone privacy
indicator. This extends it. Do not defeat capture either. Two consequences follow: the idle
Bubble must carry nothing sensitive, and the disclosure exists so that nobody can claim
"OpenFlow never appears in screenshots" on our behalf.

**Both event types, and neither extra signal.** `TYPE_VIEW_FOCUSED` alone is too narrow —
WebViews and custom editors commonly signal only `TYPE_WINDOW_STATE_CHANGED`, and missing a
supported field pushes the user to the copy fallback for no reason. A periodic poll is the
"background automation" `docs/privacy/permissions-policy.md` forbids, so it goes too, despite being the
third of OpenWhispr's three redundant signals. What is captured at dictation start is
package, view id, input type and selection — never content.

**`POST_NOTIFICATIONS` is a different kind of ask, and the difference justifies it.** We
want the microphone because we want to listen. We want notifications because **our own
foreground service cannot exist visibly without them**: with `targetSdk` 36 and no such
permission the service still runs and the notification is suppressed, so the failure mode is
an invisible microphone. Nothing is read and nothing is sent. The channel carrying the
recording indicator and nothing else is what keeps the ask honest once it is granted, and
requesting it at first dictation rather than at first launch keeps it contextual — the action
that brings the service into existence is the moment the permission starts to mean something.
Denial does not block dictation, because the service runs and the Bubble is the recording
indicator regardless; that is rather the point of a Bubble product. The residual risk is left
open on purpose: a skin may kill a service whose notification is suppressed, and that is the
gate's to measure rather than ours to decide.

## Consequences

- **A capability we decline must be disclosed as ours, not the platform's.**
  `ACTION_SET_TEXT` needs a handle on the focused field, so the service **can** read that
  field's text. `docs/privacy/privacy-policy.md` previously said we do not read surrounding
  field text, which reads as a platform guarantee and is not — it is a code-level discipline.
  The disclosure now says the capability exists and we decline it.
- **The accessibility disclosure ships even though Play is out of scope.** The map rules a
  Play release out, so the Play Console declaration and its demo video are not V1 gates, and
  ticket 34's framing of this as "a Play-policy matter" was moot. The disclosure is kept
  because the honesty rule is not a Play artifact. Its *content* is settled here; its place
  in the onboarding flow belongs to
  `.scratch/openflow-v1/issues/47-first-launch-download-and-onboarding-order.md`.
- **The permission-health screen gains two rows**: the Android 13+ restricted-settings state,
  because a sideloaded accessibility service is hidden from Settings until the user allows
  restricted settings — precisely when someone concludes the service is not there — and the
  Bubble's own enabled state, because a Bubble the user switched off makes "ready to dictate"
  false for a reason that is not a permission.
- **Keyboard behavior is a contextual G12 observation.** Record the keyboard, mode,
  orientation and Anchor with each run's motion and reachability; it does not select
  Device Class. Riding above the keyboard and staying put can both pass when the Bubble
  remains reachable and tappable. Occlusion or an unexpected jump fails G12. A stationary
  Bubble alone does not establish that usable IME insets were absent.
- **Disabled means absent, which creates a way back that does not exist yet.** If the only
  route to re-enabling is Settings, a user who switched the Bubble off has to launch the app
  first. That gap is
  `.scratch/openflow-v1/issues/49-how-a-disabled-bubble-gets-switched-back-on.md`.
- **`GLOSSARY.md` gains Anchor**, and **Bubble** no longer names a window type — a glossary
  is not a spec. The Bubble definition also now carries the three states, because they are
  domain language and not implementation detail.
