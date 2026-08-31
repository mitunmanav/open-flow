# Architecture gates

One source of truth for non-negotiable invariants. AGENTS.md references this file.

## M3-A — Audio tee (rev2)

`docs/specs/audio-tee-architecture.md` rev2 has the full spec. Invariant summary:

- One app-owned `AudioRecord` tee only: `AppAudioCapture` + `WavFileConsumer` + `PcmResampler`.
- Tee drives `cloud` and `on_phone` providers; `system` STT path uses platform `SpeechRecognizer` (no tee, no `onBufferReceived`, no WAV).
- `M3TeeFlags.USE_TEE = false` until the device matrix passes; keep `SessionAudioCapture` / `AudioRecordPcm` / `AndroidPcmMic` for rollback.
- Hard invariant: no PCM from generation g reaches g+1.
- 16 kHz is the STT normalization target, not guaranteed native (`AudioRecord.getMinBufferSize` + `PROPERTY_OUTPUT_FRAMES_PER_BUFFER`; resample when needed).
- No `FOREGROUND_SERVICE_MICROPHONE` unless the runtime path actually requires it.

## M7 PARKED — Storage / privacy

`docs/specs/storage-privacy-tradeoff.md` rev2. Do not implement encryption.

- `EncryptedFile` is deprecated with `security-crypto 1.1.0`; legacy `android-database-sqlcipher` is replaced by `sqlcipher-android` — do not add either.
- FBE vs app-layer distinction is the only encryption we depend on.
- Backup posture: `allowBackup=false` + `dataExtractionRules` exclude all.

## Bubble

- Window type: `TYPE_ACCESSIBILITY_OVERLAY`.
- Idle pill: 252 x 126.
- Never crash the accessibility service; failures must recover or stay inert.
- Visibility: `prefs.bubbleHidden` flag; never drive via opacity.
- `BubbleVisualPainter`: `pulseRing` only when `bubblePulse` pref is on.
- Drag: `BubbleWindowController` + `BubbleDragCache` + `BubbleMotion.snapX`.
- Hide predicate: `BubbleVisibility.shouldShow` hides on banks, own app, snooze.
- Tap policy: `BubbleTapPolicy` for tap + long-press 420 ms PTT; `hitVisible` for chip targeting.

## Home

`app/src/main/java/app/openflow/ui/home/`

- `HomeFeed` reads `observeRecent(limit = 200)` and grows via Load more.
- Search is debounced 300 ms via `HistorySearchPolicy`.
- Day boundaries rendered as `stickyHeader`.
- Footer height 88 dp.
- Module catalog lives in `HomeModulePolicy`.

## Insights

`app/src/main/java/app/openflow/ui/insights/InsightsScreen.kt`

- Tiles + 12-week heatmap, share card via `FileProvider`.
- All aggregations route through `InsightsAggregatePolicy` (pure, tested).

## Setup

`app/src/main/java/app/openflow/ui/setup/`

- 3 steps: A11Y → MIC → BATTERY.
- Driven by `FirstRunPolicy`.
- Battery delegate: `OemBatteryHint` (OEM-specific copy).
- Visual: `SetupProgressDots` + `SetupStepCard`.

## Emulator path (WSL2 + Windows)

- WSL has no `/dev/dri` → `-gpu host` crashes; SwiftShader crawls. Use the Windows AVD `of_win` with Intel Arc host GPU.
- Start: `of-emu` → `scripts/qa/emu-up.sh up`. Quick Boot only; no `-no-snapshot-load`; never `pkill qemu`.
- Bridge: `scripts/qa/wrap-adb.sh` + `scripts/qa/adb-bridge.sh` (WSL `:5037` → Windows `adb -a`).
- Image policy: 16 KB page alignment requires NDK 28 + `ANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON` + `graphics-path:1.1.0`.
- Do not tap "Don't Show Again" on compat dialog.
