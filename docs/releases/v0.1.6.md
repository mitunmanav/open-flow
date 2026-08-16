# Open Flow 0.1.6

Talk, tap, text — into any app. No keyboard switch, no account.

## What's new

- **No more dead controls** — controls that did nothing are wired or hidden (engine picker, home modules).
- **Light brutal skin** — new look, plus a skin control in appearance.
- **Bubble stays out of the way** — it now shows only in the app you are using.
- **Cloud ear hardening** — safer audio hand-off if you opt in later.
- **Reduced-motion support.**
- **Honest disabled ears** — a switched-off ear says so, instead of sitting silent.
- Target Android 36. 735 unit tests passing.

## Known limits

| Limit | Detail |
|-------|--------|
| **Language** | **English (en-US) only** |
| **Speech engine** | Uses your phone's speech engine, which **may send audio to Google or the OEM** |
| **Install** | **Debug-signed** sideload APK — Play Protect may warn; tap **Install anyway** |
| **Bank apps** | We hide the bubble there, but bank apps **may still warn** about Accessibility |
| **Stores** | **Not on Play or F-Droid yet** — GitHub Releases only |

## Privacy

Open Flow does not run a speech server. Your phone's speech engine may still send audio to Google or the OEM. That is not an Open Flow upload.

No account. No ads. No analytics. History stays on your phone.

## Install

Grab `open-flow-0.1.6-debug.apk` below. Full steps: [INSTALL.md](https://github.com/mitunmanav/open-flow/blob/main/docs/INSTALL.md).

Updating? Install the new APK over the old one. If Accessibility greys out, allow restricted settings again.
