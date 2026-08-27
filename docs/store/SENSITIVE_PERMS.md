# Sensitive permissions — Play disclosure

## `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`

- **Manifest**: `app/src/main/AndroidManifest.xml` `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- **Play policy**: user must see plain-language rationale **before** system dialog. Doze exemption is not for background work; it is for microphone foreground + bubble surviving.
- **Current UX**: app shows dialog (see `BatteryOptimisationDialog` / `FlowAccessibilityService` battery card) with copy “Open Flow listens while you dictate — allow ignoring battery optimizations? You can skip.” Then `Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)` only if user taps Allow.
- **If rejected**: keep `play-check.sh` BATTERY check as PASS only if code mentions battery rationale. Play Console → App content → Sensitive permissions must declare it.
- **Alternative** (if Play flags): remove perm, rely on foreground service `microphone` and user-directed Battery settings.

## Accessibility (`BIND_ACCESSIBILITY_SERVICE`)

- **Service**: `app.openflow.bubble.FlowAccessibilityService`, `isAccessibilityTool=true`, `accessibilityServiceConfig` subscribes only to `TYPE_VIEW_FOCUSED`/`TYPE_VIEW_TEXT_CHANGED` etc., not `TYPES_ALL_MASK`.
- **Use**: detect focused `EditText` + insert dictated text (Wispr-style bubble). Not for scraping ads, not for automation beyond insert.
- **Disclosure**: Play Console + in-app `Setup` / `a11y_service_description` (`app/src/main/res/values/strings.xml`) states “Accessibility only to detect focused text fields and insert speech-to-text… Password fields skipped.”
- **Minus**: no `FLAG_INCLUDE_NOT_IMPORTANT_VIEWS`; minimal events to avoid review flag.

## `SYSTEM_ALERT_WINDOW` (overlay)

- Not a manifest perm; granted via Settings “Display over other apps” for bubble `WindowManager.TYPE_APPLICATION_OVERLAY`.
- Play does not gate it, but store listing should explain bubble purpose.

## Checklist

- [ ] Battery dialog shown **before** `startActivity(REQUEST_IGNORE...)`.
- [ ] A11y description honest and minimal-event.
- [ ] If Play pre-launch flags battery perm, be ready to drop it and re-test `of_win` gate.
