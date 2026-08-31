# Install Open Flow

Android 8+ (API 26). Speech language in Settings (default en-US).

**Primary build = release-signed APK** (not the debug cert). Debug APK is for developers on a cable only.

**INTERNET** is declared in the APK; **unused** until you pick a net ear/brain or download a model. The phone’s speech engine may still use Google — that is not an Open Flow upload.

## 1. Get the APK

| Source | Where |
|--------|--------|
| GitHub Releases (users) | Latest **release-signed** APK → [Releases](https://github.com/mitunmanav/open-flow/releases/latest) |
| Local release build | `source ~/.openflow/env` → `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk` |
| Dev only (USB) | `app/build/outputs/apk/debug/app-debug.apk` — not for strangers |

## 2. Allow this one installer

Settings → Apps → Special access → **Install unknown apps** → Files (or Chrome) → Allow.

## 3. Install

Open the file. If Play Protect warns, tap **More details** → **Install anyway**.

## 4. First open

Five short screens: what it is, how to talk, dictionary vs snippet, privacy, ready.

## 5. Accessibility (the bubble)

Settings → Accessibility → **Open Flow** → On.

**If the switch is grey (Android 13+ sideload):**

1. Open the grey switch once so Android shows “restricted”.
2. Settings → Apps → Open Flow → top **⋮** → **Allow restricted settings** → unlock.
3. Go back and turn Open Flow **On**.

Samsung: turn off **Auto Blocker** if it blocks the install.  
Xiaomi / Oppo / Vivo: allow autostart and set battery to **Unrestricted** if the bubble dies.

## 6. Microphone

Allow when asked. Open Flow only listens when you tap the bubble.

## 7. Battery (OEM — if bubble dies)

App info → Battery → **Unrestricted**. Then reopen a text field.

## 8. Try it

Open Notes (or the practice field in the app). Tap the bubble. Speak. Tap again. Text should appear.

Password, PIN, and bank apps: we stay out.

## Updates

Install the new APK over the old one. History should stay.

If Accessibility turns grey again after an update, repeat **Allow restricted settings**.

## Uninstall

App info → Uninstall. That deletes local history.

## Copy

There is no copy on the bubble. Open Open Flow → **History** → copy or share.
