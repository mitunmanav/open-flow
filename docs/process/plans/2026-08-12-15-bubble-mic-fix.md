# F15 Bubble + mic insert fix

**Goal:** Speech → field insert works reliably; bubble drag/visuals less janky.

**Architecture:** Pure `SessionText` builds commit string from finals + last partial. `SttEngine.stopAndFlush` waits for onResults/onError before destroy (Android API contract). Service commits on user stop **and** engine death. Visual: one pulse path (RMS), no alpha blink; clamp Y; save XY on UP only; soft-mute STT beeps.

**Tech:** Kotlin, SpeechRecognizer, AccessibilityService overlay.

**Security:** No new permissions. No INTERNET. Soft mute only STREAM_MUSIC volume briefly (existing pattern).

## Files

| Path | Change |
|------|--------|
| `app/.../bubble/SessionText.kt` | NEW pure commit merge |
| `app/.../bubble/BubbleGeometry.kt` | maybe position helpers only if needed |
| `app/.../stt/SttEngine.kt` | stopAndFlush; keep listener until drain |
| `app/.../bubble/FlowAccessibilityService.kt` | session partial, flush stop, visuals, drag |
| `app/src/test/.../SessionTextTest.kt` | NEW |
| `app/src/test/.../BubbleGeometryTest.kt` | clamp used / covered |

## TDD

1. RED/GREEN `SessionText.commitRaw` (finals only, partial only, both, dedupe)
2. Wire service + engine
3. Unit tests green + assembleDebug
4. Device: inject + speak smoke

## Device test (Mitun)

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# a11y ON, mic ON, focus field, tap bubble, speak, tap stop → text in field
```

## Root cause (locked)

- Partials never buffered; stop nulls listener before final; `stop()` destroy races results; engine end no save.
