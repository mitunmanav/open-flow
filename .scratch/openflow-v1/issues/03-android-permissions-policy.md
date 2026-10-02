# Android permissions & Play policy for a dictation bubble

Type: research
Status: resolved
Blocked by: none

## Question

What exactly must V1 do for overlay (`TYPE_APPLICATION_OVERLAY`), AccessibilityService, microphone foreground service (`FOREGROUND_SERVICE_MICROPHONE`), `RECORD_AUDIO`, and Play Console Accessibility declarations/prominent disclosure? Produce a checklist in `docs/privacy/` and the permission-health screen requirements.

## Answer

Produced `docs/privacy/permissions-policy.md` — a checklist covering:

- **RECORD_AUDIO**: dangerous runtime permission, request contextually at bubble start; while-in-use restrictions apply to the mic FGS.
- **FOREGROUND_SERVICE_MICROPHONE / mic FGS**: declare `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` + `android:foregroundServiceType="microphone"`, pass `FOREGROUND_SERVICE_TYPE_MICROPHONE`, require RECORD_AUDIO before start, visible notification, Play Console FGS declaration with video.
- **TYPE_APPLICATION_OVERLAY**: SYSTEM_ALERT_WINDOW special permission via `Settings.canDrawOverlays()` + `ACTION_MANAGE_OVERLAY_PERMISSION` (Android 11+ ignores `package:` deep link); no Play declaration form.
- **AccessibilityService**: BIND_ACCESSIBILITY_SERVICE, narrow event types; must NOT claim `isAccessibilityTool`; prominent disclosure screen (in-app, normal flow, separate, affirmative consent) + Play Console accessibility declaration with disclosure/consent video; deterministic rule-based automation only.
- **Permission-health screen**: per-capability rows (mic, overlay, accessibility, mic FGS, battery optimization, notifications) with deep links, re-checked in onResume, plus overall "ready to dictate" state.

Key sources: developer.android.com FG service types doc, Play Console "Use of the AccessibilityService API" (answer/10964491), "Understanding foreground service and full-screen intent requirements" (answer/13392821), Android 11 permission changes doc. Note: `docs/privacy/` did not exist before; created to match the requested path.
