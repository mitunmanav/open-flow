# Reference apps teardown

Architecture notes on the five Android voice-dictation references we measured
OpenFlow against. Sources: each repo's README, LICENSE, and visible file tree on
GitHub (accessed 2026-10-02). We read these to learn architecture, not to copy
code — license notes below say what we may and may not take.

## Comparison at a glance

| App | Surface (bubble?) | STT engine | Cleanup | Injection | License | Copy code? |
| --- | --- | --- | --- | --- | --- | --- |
| vox-android | Floating bubble overlay | whisper.cpp (on-device) | On-device Gemma 3 1B via MediaPipe | AccessibilityService | MIT | Yes (keep notices) |
| OpenWhispr | Floating overlay button | Groq Whisper cloud, or local via sherpa-onnx | Groq chat (gpt-oss-120b), optional | AccessibilityService (clipboard fallback) | Apache-2.0 | Yes |
| phone-whisper | Floating overlay button | OpenAI cloud, or local via sherpa-onnx | OpenAI chat, optional | AccessibilityService (clipboard fallback) | Apache-2.0 | Yes |
| Sayboard | IME keyboard + RecognitionService (no bubble) | Vosk (on-device) | None | IME as input method | **GPL-3.0** | **No** — design only |
| WhisperInput | Voice keyboard / input panel / assistant (no bubble) | whisper.cpp (on-device) | Punctuation via Kõnele components | IME / recognition service | MIT (deps Apache-2.0) | Yes |

## vox-android (hadencain/vox-android)

MIT license. Android port of "Vox for Windows".

- **Flow**: floating bubble → AudioRecord at 16 kHz → whisper.cpp via JNI →
  Gemma 3 1B cleanup via MediaPipe LLM → text injected through an
  AccessibilityService into the focused app's text field.
- **Cleanup**: on-device LLM removes fillers, punctuation fixes,
  self-corrections; adapts tone per app context (chat vs email vs notes). Long-press
  the bubble for an "AI edit mode" that rewrites selected text. Raw mode (tap the
  caption mid-take) skips cleanup and is never written to history.
- **State handling**: a single-threaded state machine, `IDLE → RECORDING →
  PROCESSING`. Models lazy-load on first take and unload after an idle timeout to
  return RAM. Voice commands like "scratch that" cancel a take.
- **Permissions**: microphone, display-over-other-apps (`TYPE_APPLICATION_OVERLAY`
  bubble), and accessibility service — the app walks the user through setup.
- **Teach us**: bubble as the only UI surface; lazy model load / idle unload;
  raw-vs-cleanup mode split; haptics on take start/stop/done; cleanup is a
  separate stage after STT.
- **Avoid / adapt**: Vox uses an on-device Gemma LLM for cleanup, but
  OpenFlow V1's refiner is deterministic-only (map.md) — treat the LLM step as
  future work, not V1 scope.

## OpenWhispr (EdiBianco/OpenWhispr)

Apache-2.0 (LICENSE file), fork of phone-whisper.

- **Flow**: small floating overlay button → tap to record → tap to stop →
  transcribe (cloud Groq `whisper-large-v3`, or local sherpa-onnx) → optional Groq
  chat cleanup (filler removal, punctuation, grammar, email formatting) → insert
  into the focused field → clipboard copy if insertion fails.
- **State handling**: foreground service with a persistent low-priority
  notification; "Background service" toggle pauses the overlay without revoking
  the accessibility permission. Hardened so one bad accessibility event or model
  failure can't kill the service process.
- **Overlay visibility**: three redundant signals — accessibility focus events, a
  periodic focus poll, and system keyboard visibility — show the mic only while a
  text field is focused (works in WhatsApp/Telegram-style custom composers).
- **Permissions**: RECORD_AUDIO, Accessibility Service (scoped to text insertion
  only; not a keyboard replacement, no background automation), battery
  optimisation exemption with a one-tap fix, POST_NOTIFICATIONS, and a walkthrough
  for Android 13+ restricted-settings blocking of sideloaded accessibility.
- **Teach us**: the three-signal focus detection; clipboard fallback on insertion
  failure; battery-exemption prompt; restricted-settings help; pause-without-revoke.
- **Avoid**: it is cloud-first (Groq) with per-app cleanup prompts; OpenFlow is
  local-first and V1-deterministic. Its in-app APK self-update is out of V1 scope.

## phone-whisper (kafkasl/phone-whisper)

Apache-2.0. The ancestor of OpenWhispr; contrast the two to see what a fork
changed.

- **Flow**: floating button → tap/tap → cloud OpenAI Whisper or local sherpa-onnx
  → optional OpenAI cleanup (punctuation/grammar) → insert into focused field →
  clipboard fallback.
- **State handling**: same simple tap-to-start / tap-to-stop flow; no published
  state machine diagram; models downloaded into
  `files/models/` from sherpa-onnx release archives (Parakeet 110M/0.6B, Whisper
  Base, Moonshine Tiny).
- **Permissions**: RECORD_AUDIO, Accessibility Service, plus INTERNET and the
  standard battery/notification handling inherited by OpenWhispr.
- **Teach us**: the canonical cloud-or-local provider split; model catalog and
  on-disk model layout; clipboard fallback; the "which apps can we insert into"
  compatibility table (standard fields vs Termux).
- **Avoid**: OpenAI-specific cleanup prompt text and any cloud-default flow —
  OpenFlow providers must be adapter-driven, not vendor-baked.

## Sayboard (ElishaAz/Sayboard)

**GPL-3.0 (LICENSE.txt)** — source-first copyleft. **Do not copy or vendor its
code.** Use it as an architecture reference only.

- **Flow**: it is a **voice IME keyboard** (input method), not a bubble. It
  registers an Android `RecognitionService` backed by Vosk; the system keyboard
  shows a mic key; recognition results commit into whatever field has focus via
  the IME. Based on Felicis/vosk-android-demo.
- **State handling**: standard Android IME lifecycle plus a background
  download/import service for Vosk models with progress notifications.
- **Permissions**: RECORD_AUDIO, INTERNET (model download only — app never sends
  user data out), POST_NOTIFICATIONS, FOREGROUND_SERVICE +
  FOREGROUND_SERVICE_MICROPHONE + FOREGROUND_SERVICE_SPECIAL_USE, and
  QUERY_ALL_PACKAGES (a documented Android quirk for recognition services).
- **Teach us**: only if/when we consider an IME fallback: how a RecognitionService
  integrates with the system dictation UI, the model-download UX, and why
  QUERY_ALL_PACKAGES sneaks in.
- **Avoid**: all code. GPL-3.0 is incompatible with OpenFlow's Apache-2.0 intent;
  copying it would force the whole app to GPL-3.0. Also avoid the IME-as-primary
  surface: OpenFlow V1 is explicitly a floating bubble, not a keyboard.

## WhisperInput (alex-vt/WhisperInput)

MIT. Built on Kõnele (Apache-2.0), speechutils (Apache-2.0), whisper.cpp (MIT),
OpenAI whisper model (MIT).

- **Flow**: on-device recognition via whisper.cpp wrapped in a Kõnele-style
  RecognitionService; usable three ways — voice keyboard (IME), voice input panel,
  or the system assistant (long-press Home). Auto-start/auto-stop with optional
  audio cue.
- **State handling**: recognition-service callback model; known pitfalls
  documented — first take may be inconsistent while the model loads, and the
  permission model from Kõnele can fall behind new Android requirements (workaround:
  grant permissions manually).
- **Permissions**: standard mic permission via the Kõnele permission model.
- **Teach us**: the third integration surface (assistant app) as a possible future
  OpenFlow entry point; the explicit "model-not-ready" UX pitfall (we should gate
  the bubble on model readiness); Kõnele/speechutils as an alternative on-device
  integration path.
- **Avoid**: copying its permission flow naively (documented as lagging Android
  requirements); don't adopt IME-first UX for V1.

## Cross-app synthesis — what to teach us vs avoid

**Adopt (primary teaching sources):**

1. **Bubble-driven dictation** (vox-android, phone-whisper, OpenWhispr): overlay
   bubble/button, tap-to-start/tap-to-stop, live captions, haptics. This matches
   OpenFlow's Bubble definition.
2. **Permission triad**: microphone + display-over-other-apps + Accessibility,
   with a guided setup walkthrough and a clear "why accessibility" explanation
   scoped to insertion only.
3. **Insertion contract**: try accessibility text actions at insertion time;
   verify the field is still editable; fall back to clipboard copy/paste
   recovery when the target app blocks injection (Termux, custom terminals).
4. **Overlay visibility**: redundant focus detection (accessibility events +
   poll + keyboard visibility) so the bubble tracks real text fields.
5. **Battery/OEM reality**: foreground service with a minimal persistent
   notification, battery-optimisation exemption prompt, and a pause switch that
   doesn't revoke the accessibility permission.
6. **Model lifecycle**: on-disk model catalog, lazy load, unload on idle timeout,
   and surface "model not ready" state in the UI (WhisperInput's documented
   pitfall).

**Avoid / deliberately not copy:**

1. **Sayboard's codebase — GPL-3.0.** Architecture read only; no code, no
   vendoring, no asset reuse. Design reference for a future IME fallback at most.
2. **IME-first UX** (Sayboard, WhisperInput): OpenFlow V1 is bubble-only.
3. **Cloud-default flows and vendor-baked cleanup prompts** (phone-whisper,
   OpenWhispr): our providers are adapter-driven; V1 refinement is deterministic
   (no cloud LLM, no local Gemma yet). Borrow the cleanup *slot* in the pipeline,
   not the implementation.
4. **In-app self-updating APK installs** (OpenWhispr) and APK-sideload update
   channels — out of scope for V1 release mechanics.
5. **QUERY_ALL_PACKAGES**-style workarounds unless a hard Android requirement
   forces it later.
6. **Whisper model weight assets**: all references rely on downloadable or
   user-supplied models; OpenFlow likewise avoids bundling model binaries.

## License compatibility summary

| Project | License | Usable in Apache-2.0 OpenFlow? |
| --- | --- | --- |
| vox-android | MIT | Yes — keep copyright notice |
| OpenWhispr | Apache-2.0 | Yes — compatible with Apache-2.0 |
| phone-whisper | Apache-2.0 | Yes |
| Sayboard | GPL-3.0 | **No code reuse** — would copyleft the app |
| WhisperInput | MIT (deps Apache-2.0) | Yes — keep notices |
| whisper.cpp / OpenAI whisper | MIT | Yes |
| Vosk (Sayboard's engine) | Apache-2.0 | Engine is fine; Sayboard's wrapper code is not |
| sherpa-onnx | Apache-2.0 | Yes (V1's first real provider) |
