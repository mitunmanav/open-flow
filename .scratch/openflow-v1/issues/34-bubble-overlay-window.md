# How the bubble overlay window is actually built

Type: grilling
Status: open
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