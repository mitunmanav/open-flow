# Permissions & Play Policy Checklist — Floating-Bubble Dictation App

Checked authors: Android developer docs + Google Play policy pages (primary sources).
Sources are linked per item. Last reviewed: 2026-10-02.

## 1. `RECORD_AUDIO` (runtime, mic)

- [ ] Declare `android.permission.RECORD_AUDIO` in the manifest. It is a `dangerous`
  runtime permission — must be requested at runtime before any mic capture, with a
  contextual rationale tied to the user action that starts dictation.
  (Android runtime permissions overview: https://developer.android.com/guide/topics/permissions/overview)
- [ ] Expect while-in-use restrictions: mic FGS cannot start while the app is in the
  background or from a `BOOT_COMPLETED` receiver (with narrow exemptions).
  (https://developer.android.com/develop/background-work/services/fgs/service-types#microphone)
- [ ] Android 12+ shows the mic privacy indicator while recording; do not try to
  suppress or mislead it.
- [ ] Audio lifetime rules (delete-by-default etc.) are covered by ticket 09 —
  this doc only covers the permission mechanics.

## 2. Microphone foreground service (Android 14+, API 34)

- [ ] `FOREGROUND_SERVICE` permission declared.
- [ ] `FOREGROUND_SERVICE_MICROPHONE` permission declared.
- [ ] `<service android:foregroundServiceType="microphone">` in the manifest.
- [ ] Pass `FOREGROUND_SERVICE_TYPE_MICROPHONE` to `startForeground()`.
- [ ] `RECORD_AUDIO` runtime permission granted *before* `startForeground()`.
- [ ] Persistent, accurate status-bar notification for the whole recording session.
  (https://developer.android.com/develop/background-work/services/fgs)
- [ ] Play Console declaration (App content → monitor/improve): description of the
  dictation feature, user impact if deferred/interrupted, use case `TYPE_MICROPHONE`
  ("Background Audio Access"), and a video showing the user flow that triggers it.
  (https://support.google.com/googleplay/android-developer/answer/13392821)
- [ ] Declared declaration matches actual behavior; changing usage requires
  resubmitting the form.

## 3. Overlay bubble (`TYPE_APPLICATION_OVERLAY`)

- [ ] Declare `android.permission.SYSTEM_ALERT_WINDOW`.
- [ ] It is a *special* permission, not runtime-prompted: check
  `Settings.canDrawOverlays(context)`, and if false launch
  `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. On Android 11+ the intent always
  lands on the top-level settings page; the `package:` deep-link is ignored.
  (https://developer.android.com/about/versions/11/privacy/permissions)
- [ ] No Play Console declaration form for overlay windows; still describe the
  bubble's purpose in the store listing and prominent disclosure where relevant.
- [ ] Request overlay access at the moment the user first enables the bubble —
  not at first launch — and explain why first.

## 4. `AccessibilityService` (text insertion into the focused field)

- [ ] Extend `AccessibilityService`; declare with
  `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`,
  `android:exported="true"`, an intent-filter for
  `android.accessibilityservice.AccessibilityService`, and meta-data pointing to
  `res/xml/accessibility_service_config.xml`.
  (https://developer.android.com/guide/topics/ui/accessibility/service)
- [ ] Config XML: narrow `accessibilityEventTypes` (only what insertion needs,
  not `typeAllMask`), least-privilege flags, meaningful `description`,
  `canRetrieveWindowContent="true"` only if needed.
- [ ] **Do NOT set `isAccessibilityTool="true"`** — a dictation helper is not an
  accessibility tool (voice-activated assistants/general assistants explicitly do
  not qualify), and a wrong claim can trigger suspension.
  (https://support.google.com/googleplay/android-developer/answer/10964491)
- [ ] Prominent disclosure (required, separate screen, before enabling):
  shown inside the app, in normal usage flow (not only in settings/privacy
  policy), describes the data accessed via the API, explains use/sharing,
  requires affirmative consent (tap to accept / checkbox), and is a standalone
  disclosure not bundled with other data disclosures.
  (same source, "Prominent disclosure and consent requirements")
- [ ] Play Console Accessibility declaration (targetSdk 31+): why the API is
  needed, whether you collect/share data with it, and a video showing app open →
  disclosure → user consents → accessibility permission granted → service used.
  (same source)
- [ ] Deterministic, rule-based automation only ("If trigger X, do Y"); no
  autonomous agentic behavior via the Accessibility API.

## 5. Permission-health screen (in-app requirements)

One screen in V1 that shows live status for every capability the bubble needs,
each row with status (granted / denied / not enabled) and a deep link:

- [ ] **Microphone** — runtime `RECORD_AUDIO` granted? → link to app permission.
- [ ] **Overlay** — `Settings.canDrawOverlays()`? → `ACTION_MANAGE_OVERLAY_PERMISSION`.
- [ ] **Accessibility service** — service enabled in system settings? →
  `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
- [ ] **Microphone FGS** — `FOREGROUND_SERVICE_MICROPHONE` declared & granted
  (install-time) and last service start didn't hit while-in-use restriction?
- [ ] **Battery optimization** — not ignoring battery optimizations for the app
  (surface state; link to `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is
  Play-sensitive — link to system screen instead if unsure).
- [ ] **Notifications** — FGS notification channel allowed? → app notification
  settings (mic FGS requires a visible notification).
- [ ] Overall "ready to dictate" summary derived from the rows above; tapping any
  denied row launches the corresponding system screen. Re-check all statuses in
  `onResume` (user returns from settings).

## 6. Play Console declarations summary

- [ ] Accessibility API declaration + prominent-disclosure video.
- [ ] Foreground service types: `microphone` (+ `specialUse`/others only if used).
- [ ] Data safety form: mic audio collection, retention, sharing — consistent
  with ticket 09 privacy rules.
- [ ] Store listing mentions overlay, accessibility-based text insertion, and
  microphone use prominently.
