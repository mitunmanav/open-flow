# Audio Tee Architecture — M3 Spec (rev2)

## Decision

- **STATUS:** PARKED — revised. **DO NOT IMPLEMENT** until decisions below are approved and device measurements are recorded.
- **DECISION REQUIRED:** Choose **M3-A** vs **M3-B** before any code. See §1.
- **M3-A (recommended):** One app-owned `AudioRecord` tee for `cloud` + `on_phone` (PCM consumers: cloud WebSocket, Whisper, WAV). Keep **platform-owned** `SpeechRecognizer` for `system` ear with **no tee**. `system` audio stays inside the recognition service.
- **M3-B (deferred):** Replace `SpeechRecognizer` entirely with app-owned PCM → STT (e.g., whisper/cloud). Only if product decides to drop platform STT. Not in scope for this revision.
- **DO NOT IMPLEMENT UNTIL:** (1) M3-A vs M3-B signed off, (2) device matrix measures native rates + `getMinBufferSize` + latency on `of_win` + Samsung/Xiaomi, (3) generation invariant reviewed, (4) Android 14 `microphone` foreground-service policy confirmed for any background capture.

## Material changes from rev1 (2026-08-27)

1. Resolved contradiction: `SpeechRecognizer` owns its audio; app `AudioRecord` cannot tee it (§1). Removed claim that one tee serves `system`.
2. Replaced single-tee-for-all decision with explicit **A vs B** (§1, §5).
3. Removed `RecognitionListener.onBufferReceived()` as PCM tee/input (§1.1 note + §2).
4. Changed 16 kHz from “native capture rate for all devices” to **STT normalization target**; native rate is device-dependent (§4).
5. Replaced fixed `200 ms = 6400 bytes` with **device-aware sizing**: `getMinBufferSize()` + `AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER` + measurement (§5.1).
6. Removed unsupported quantitative CPU claim “~50% reduction” — replaced with “no claimed %; measure via systrace” (§8).
7. Added **audio-generation invariant** to prevent stale PCM reaching new session (§6).
8. Added **Android 14+ microphone foreground-service lifecycle** requirements and current Open Flow posture (§7).
9. Updated ownership table, PCM flow diagram, lifecycle/concurrency/failure/performance/testing/rollback to reflect A vs B (§5-§12).
10. Added authoritative citations and removed truncated latency link; cited `AudioRecord` guarantee language.

## 1. The SpeechRecognizer contradiction

`SpeechRecognizer`/`RecognitionService` owns the microphone internally. App code cannot open a second `AudioRecord` on the same stream and expect coherent frames, and cannot reliably steal the recognizer’s stream.

- Framework: `RecognitionListener.onBufferReceived(byte[])` **“More sound has been received. The purpose of this function is to allow giving feedback to the user regarding the captured audio. There is no guarantee that this method will be called. … sample rate is implementation dependent.”** [`RecognitionListener.java`](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/RecognitionListener.java). Same note in `RecognitionService.Callback.bufferReceived()` [`RecognitionService.java`](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/RecognitionService.java).
- Framework: `SpeechRecognizer` **“The implementation of this API is likely to stream audio to remote servers … not intended to be used for continuous recognition”** [`SpeechRecognizer.java`](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/SpeechRecognizer.java)[[search:SpeechRecognizer docs]](https://learn.microsoft.com/en-us/dotnet/api/android.speech.speechrecognizer?view=net-android-35.0).
- Community: `onBufferReceived` not called on Google’s recognizer in practice ([Stack Overflow](https://stackoverflow.com/questions/46088077/onbufferreceived-in-recognitionlistener-is-not-called-when-voice-recognition-is)).

Therefore M3 cannot be “one `AudioRecord` tee that also feeds/fed by `SpeechRecognizer`.” Two honest architectures exist.

### 1.1 Decision

**Pick one before code:**

- **M3-A — App AudioRecord tee (cloud + on_phone) + separate platform SpeechRecognizer (system).** Cloud and on-device paths share one `AudioRecord` with fan-out; `system` keeps `SpeechRecognizer` unmodified, no WAV tee from recognizer, no `onBufferReceived` reliance. This matches current product: `system` = platform STT with `EXTRA_PREFER_OFFLINE`/`onDevice` factory, cloud/on_phone = app PCM.
- **M3-B — PCM-input STT replaces SpeechRecognizer.** All ears become app PCM consumers. `system` ear would be removed or redefined as PCM → server/Whisper. Loses platform biasing (`EXTRA_BIASING_STRINGS`, `EXTRA_ENABLE_BIASING_DEVICE_CONTEXT`), offline pack handling, and vendor tuning. Only choose if product commits to owning STT end-to-end.

**This spec designs M3-A. M3-B is documented as alternative with cost.**

Do not use `onBufferReceived` as input for STT or WAV. It may fire 0 times, at vendor-dependent rate, and cannot be treated as lossless PCM.

## 2. Current ownership (as of 2026-08-27, post Block A/B, pre-M3)

| Owner | Source | Format | Thread | Buffer | Consumers |
|-------|--------|--------|--------|--------|-----------|
| `SessionAudioCapture` | `MIC` | PCM16 16 kHz (requested) mono | `openflow-session-pcm` | `ArrayList<ByteArray>` + `CaptureCap 8M` | WAV only for retry (`WavPcm.wrapPcm16leMono`) |
| `AudioRecordPcm` (on_phone) | `VOICE_RECOGNITION` | float `[-1,1]` derived from PCM16 16 kHz | `openflow-whisper-pcm` | `PcmChunker` 240k samples (15 s) + `TranscriptParts` + internal WAV `wavChunks` 8M | `JniWhisperRuntime` + retry WAV |
| `AndroidPcmMic` (cloud) | `VOICE_RECOGNITION` | PCM16 16 kHz | `openflow-pcm` | none (streams) | `CloudEar.writeAudio` → `CloudSocket.send` |
| `SttEngine` (system) | `SpeechRecognizer` internal | opaque | main `Handler` | internal | `FlowAccessibilityService` `onPartial`/`onFinal` |

`FlowAccessibilityService.startListening` via `SttRouter`/`EarGate` starts exactly one path:

- `on_phone` → `AudioRecordPcm` + internal WAV (single `AudioRecord`, no `SessionAudioCapture`).
- `system` → `SpeechRecognizer` + `SessionAudioCapture` — **dual** (platform + `MIC`).
- `cloud` → `AndroidPcmMic` + `SessionAudioCapture` — **dual** (`VOICE_RECOGNITION` + `MIC`).

`EarMicPolicy.bubbleCapturesWav(earId) = earId != "on_phone"` avoided dual for whisper only. M3-A eliminates dual for `cloud` (share one `AudioRecord`), keeps `system` dual resolved by removing its WAV tee (or accepting no WAV for `system`).

## 3. PCM flow today

```
MIC/VOICE_RECOGNITION
  → AudioRecord.read(buf)
  → copyOf(n) on worker
  → SessionAudioCapture: sync add under CaptureCap.admit
  → stopAndWrite: join → WavPcm.wrap → filesDir/audio/<id>.wav

VOICE_RECOGNITION (whisper)
  → read → toFloat → onRms → PcmChunker.push → transcribeWindows → TranscriptParts
  → take(): clear, flush remainder → join
  → WAV tee in same callback: floatToPcmBytes → wavChunks

VOICE_RECOGNITION (cloud)
  → read → onChunk → CloudEar.writeAudio → CloudSocket.send
  → stopAndFlush: pcm.stop() + Handler pendingFinish 5 s, early on final/error
```

Threading: each recorder owns `AtomicBoolean running` + `Thread` + `AudioRecord`; `stop()` joins 500–800 ms then `stop()`+`release()`. `SpeechEngine.Listener` hops via `Handler(Looper.getMainLooper())`.

## 4. Sample-rate policy (corrected)

**16 kHz is an STT normalization target, not a universal native capture rate.**

- `AudioRecord` constructor javadoc: **“44100Hz is currently the only rate that is guaranteed to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices.”** [`AudioRecord.java`](https://android.googlesource.com/platform/frameworks/base/+/master/media/java/android/media/AudioRecord.java) (and `RATE_UNSPECIFIED` route-dependent path).
- Native output/capture rates are device-dependent; `AudioDeviceInfo.getSampleRates()` or probing `getMinBufferSize()` is needed. Discussed in `google/oboe#95` — test `getMinBufferSize` and pick lowest-buffer viable rate.
- Open Flow today requests `16_000` with `CHANNEL_IN_MONO` `PCM_16BIT` and checks `getMinBufferSize() > 0` + `STATE_INITIALIZED` + `startRecording()` catch. That is honest. **Rev change:** Do not claim 16 kHz works everywhere. Treat it as requested STT rate; if device rejects it (`getMinBufferSize() <= 0` or `STATE_INITIALIZED != true`), probe fallback (e.g., device native 48 kHz/44.1 kHz) and **resample to 16 kHz** before feeding STT (`WavPcm.pcm16ToFloat` path needs resampler, or use `AAudio`/Oboe resampler). Log actual `getSampleRate()` and resample path.

Sampling best practice: avoid unnecessary resampling for low latency, but if you resample, prefer simple ratios and avoid downsampling >6:1 without proper filter ([NDK sampling guide](https://developer.android.com/ndk/guides/audio/sampling-audio)).

## 5. Proposed tee (M3-A) — one app-owned AudioRecord

### 5.0 Scope

Applies only to app-owned ears: `cloud` (`openai`/`deepgram`/`assemblyai`/`sarvam`/`custom_stt`/`laptop`) and `on_phone` (`whisper`). `system` stays `SpeechRecognizer` (see §5.4).

### 5.1 Single recorder — device-aware

- One `AppAudioCapture` owning one `AudioRecord` with **requested** `VOICE_RECOGNITION`, `CHANNEL_IN_MONO`, `ENCODING_PCM_16BIT`, `requestedRate = 16_000`.
- Sizing: probe at runtime
  1. `min = AudioRecord.getMinBufferSize(requestedRate, CHANNEL_IN_MONO, PCM_16BIT)`
  2. `framesPerBuffer = AudioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)` → `bytesPerBufferHint = framesPerBuffer * 2` (PCM16 mono)
  3. `bufSize = max(min, hints, requestedRate/10*2 /* 100 ms floor */)` rounded up to `min` multiple; if `min <= 0` probe native (48000/44100) and plan resample.
  4. No hardcoded “200 ms = 6400 bytes is optimal” claim — **measure** end-to-end latency on `of_win` + Samsung/Xiaomi with `SessionLatency` marks + `adb shell dumpsys media.audio_flinger`.
- Fail-soft: `try/catch` construction, `STATE_INITIALIZED` via `PcmStartPolicy`, `startRecording()` catch → return `false` → service shows `needMic`; log `bufferOk/recordOk`.

Source `VOICE_RECOGNITION` preferred for its AEC/NS tuned for recognition; fallback to `MIC` if `VOICE_RECOGNITION` construction fails (Samsung edge). Record chosen source.

### 5.2 Tee / fan-out

- `CopyOnWriteArrayList<Consumer>` subscribers. One `read` loop: `while(running) { n = ar.read(buf); if(n>0) dispatch(buf.copyOf(n) to consumers)}`. One `copyOf` per dispatch is already one copy per consumer at call site — no extra inter-consumer copies.
- Consumers for M3-A:
  - `WhisperConsumer` (on_phone only): PCM16 → float via `WavPcm.pcm16ToFloat` (or resampled float), owns `PcmChunker` + `TranscriptParts`, enqueues to `whisperExec`.
  - `CloudConsumer` (cloud ears): PCM16 → `CloudEar.writeAudio` → socket queue, never blocks `read` loop.
  - `WavFileConsumer` (when retention `keep`/`wipe_24h` and non-system ear): PCM16 → `CaptureCap 8M` sync add, writes `WavPcm.wrapPcm16leMono` on `stopAndWrite`.
- Backpressure: consumers must not block `read` loop; whisper/cloud enqueue, WAV does bounded synchronized add.
- Resampler lives at consumer edge if native ≠ 16 kHz (e.g., `WavPcm` resample or `AudioResampler`).

### 5.3 Buffers / caps

- WAV: `CaptureCap.admit` (header-preserving head, drop on overflow) 8 M (~4 min at 16 kHz mono). One buffer, not two.
- Whisper: `PcmChunker` 15 s windows, `TranscriptParts` join/hallucination guard.
- Cloud: no buffer beyond socket queue.

### 5.4 system ear handling (resolves contradiction)

`system` (`SpeechRecognizer`) does **not** join the tee. No `AppAudioCapture`, no `SessionAudioCapture` for `system`. Implications:

- No WAV retry for `system` failures unless we accept dual or accept no retry. Recommended: for `system` ear, if `PersistAsk` requires audio, **do not write WAV** — treat retry as “speak again” (same as failed WAV today `toast speak again`). Document in UI.
- Alternative (not recommended): keep `SessionAudioCapture` only for `system` with dual acknowledged as platform limitation. Spec rejects this for now.

### 5.5 Ownership

- `FlowAccessibilityService` owns lifecycle: `startListening` → `appAudioCapture.start(subscribersFor(earId))` for M3-A ears; `stopListening` → `appAudioCapture.stop()` (one `join`, not two).
- `EarMicPolicy` becomes `teeEnabledFor(earId)` = not `system`; `SessionAudioCapture` retained only if `system` dual is kept, else deleted after migration (or kept as `WavFileConsumer` wrapper).
- `AudioRecordPcm`/`AndroidPcmMic` superseded by `AppAudioCapture` + consumers; keep behind flag until verification.

## 6. Lifecycle / generation invariant (new)

**Invariant: No PCM from generation `g` reaches consumers of generation `g+1`.**

- `listenGeneration: AtomicInteger` incremented in `FlowAccessibilityService.startListening` before any `start()`.
- `AppAudioCapture.start(generation)` stores `activeGeneration` under lock; `read` loop tags each dispatch with `activeGeneration`; consumers check `if (tag != activeGeneration) drop`.
- `stop()` clears `activeGeneration` → loop exits, sets `running=false`, `join(500)`; then `wavConsumer.clear()` + `whisperConsumer.clear()` (or `take()` that returns and clears). `stopAndDiscard()` path also clears.
- `stopAndWrite`/`transcribeWavFile` require `generation == activeGeneration` snapshot taken at `start`; if caller generation mismatches, return null and drop WAV.
- `OnDeviceEar.writeWav`/`discardWav` participate in same generation — `discardWav()` on any `MARK_OK`/`SKIP` path even if write failed (avoids unbounded growth, already in `FlowAccessibilityService:1708`).
- Tests must assert: start g=1 → produce PCM → stop → start g=2 → no PCM from g=1 in g=2’s file.

## 7. Android 14+ microphone foreground-service lifecycle

Open Flow today captures mic from `FlowAccessibilityService` (bound accessibility service, not a `startForeground` service). No `FOREGROUND_SERVICE_MICROPHONE` yet (`AndroidManifest` has no `foregroundServiceType`).

Rules to observe for any future background mic via `Service.startForeground()`:

- Target `34+` must declare per-service type: [`microphone`](https://developer.android.com/about/versions/14/changes/fgs-types-required) in `AndroidManifest` `android:foregroundServiceType="microphone"` and request `FOREGROUND_SERVICE_MICROPHONE` permission, plus hold `RECORD_AUDIO` runtime. Combined types use `|` (e.g., `camera|microphone`) ([declare guide](https://developer.android.com/develop/background-work/services/fgs/declare)).
- **While-in-use restriction:** `RECORD_AUDIO` is while-in-use; cannot create a `microphone` foreground service while app is in background, with limited exceptions; also cannot launch it from `BOOT_COMPLETED` on `34+` except exempt cases ([service types – Microphone note](https://developer.android.com/develop/background-work/services/fgs/service-types)). [`Changes for Android 15`](https://developer.android.com/about/versions/15/changes/foreground-service-types) extends `BOOT_COMPLETED` restriction to `microphone` on `34+`.
- Calling `startForeground()` without declared type throws `MissingForegroundServiceTypeException`; without permission throws `SecurityException` ([current behavior issue](https://github.com/invertase/notifee/issues/997) — declare type + permission).
- Full correctness also requires Play Console foreground-service declaration with video justification.

**Implication:** If M3-A ever needs to capture while app is backgrounded beyond accessibility-service lifetime, it must use a `microphone` foreground service with the above manifest/permission changes and must be started while in foreground (e.g., from `FlowAccessibilityService` `onAccessibilityEvent` or user bubble tap). Current spec does **not** add such a service; it records that any future `AppAudioCapture` background work that outlives the accessibility service must follow these rules. Accessibility service itself is system-bound, not a foreground service, so not subject to `foregroundServiceType` but still subject to OS mic privacy indicators and `AppOps` (`RECORD_AUDIO`).

Add to testing: verify no `MissingForegroundServiceTypeException`/`SecurityException` if a `microphone` service is added; verify indicator.

## 8. Battery / latency (claims corrected)

Remove prior “~50% CPU reduction” claim. Single `AudioRecord` does reduce one thread + one `read` syscall + one buffer, but CPU depends on OEM HAL, resampling, and STT. **Measure**, don’t claim.

- Record `SessionLatency` marks: `audio_start`, `first_bytes`, `cloud_first_send`, `whisper_first_chunk`, `stop_flush_ms`.
- Use `bufSize` from §5.1 and measure cloud first-byte latency vs `bufSize`; prefer smallest `min`-compliant `bufSize` that keeps `read` from underrunning, not a fixed 200 ms. Whisper batches via `PcmChunker`, so larger `bufSize` does not hurt whisper.
- Keep `setListeningAwake(true)` + `FLAG_KEEP_SCREEN_ON`; no extra wakelock for tee. Quantify via `BatteryStats`/`perfetto` if pursued.

## 9. Failure / concurrency (updated for M3-A)

- Single `AtomicBoolean running` + `listenGeneration` guard; `stopInProgress` blocks re-entrant `startListening`/`stopListening` unchanged.
- `AudioRecord.read` returning `-ERROR_*` (e.g., `ERROR_INVALID_OPERATION` on contended mic) breaks loop, ear delivers `onError` via listener, service calls `persistFailedSession` with generation-tagged WAV (or no WAV for `system`).
- `AppAudioCapture.start()` returning `false` (buffer/state/start fail) → service shows `needMic` and does not claim `listening`.
- `system` ear failure paths keep existing `SttEngine` `stopAndFlush` with `DEFAULT_FLUSH_TIMEOUT_MS 3 s` bounded, not coupling to JNI. JNI `WhisperLib.fullTranscribe` remains not cancellable; timeout delivers partial `parts.join()` honestly.

## 10. Testing (updated)

- Unit: `CaptureCap.admit` boundary, `WavPcm.wrap`/`unwrap`/`pcm16ToFloat` round-trip (incl. resample path), `PcmChunker` never drops, `TranscriptParts.join` hallucination, `AppAudioCapture` fan-out delivers same bytes to N consumers (`FakeConsumer`), **generation invariant**: start-stop-start does not leak PCM, `stopAndFlush` atomic guard, `listenGeneration` stale drop.
- Probe: instrument test that requests 16 kHz, logs `getMinBufferSize`/`STATE_INITIALIZED`/`getSampleRate()`/actual `bufSize`/`framesPerBuffer`; on fallback assert resampled output rate 16 kHz.
- Instrumented: existing 3 `FlowAccessibilityService` tests still pass. Add: M3-A ear 5 s → stop → verify `filesDir/audio/<id>.wav` exists and `transcribeWavFile` non-empty for `on_phone`/`cloud`; `system` ear verify no WAV written.
- QA: `gate.sh --quick` PASS; manual 30 s + 4 min dictation, kill mid-listen → `dumpsys media.audio_flinger` shows no orphan `AudioRecord`; measure first-byte latency vs `bufSize`.
- Negative: `onBufferReceived` never asserted; if it fires, assert it is ignored for STT/WAV.

## 11. Rollback

- Keep `SessionAudioCapture` + `AudioRecordPcm`/`AndroidPcmMic` behind feature flag `useTee` default `false` until M3-A verified on `of_win` + real Samsung/Xiaomi. Migration steps: (1) add `AppAudioCapture` + consumers, (2) gate `startListening` to use tee for `cloud`/`on_phone` only, (3) verify 3 consecutive `gate --quick` + manual + no `ERROR_INVALID_OPERATION` in logcat, (4) flip default, (5) delete old classes. Rollback flips flag off; `system` untouched.

## 12. Alternatives not chosen

- **Shared Memory / tee from `SpeechRecognizer`:** Rejected — `onBufferReceived` unreliable and rate-implementation-dependent (§1).
- **All ears PCM-only (M3-B):** Deferred — high product cost (lose biasing/offline packs), requires resampler + streaming STT for every language. Can revisit if product decides to drop platform STT.

## 13. Open questions

- For `system` ear, is “no WAV, speak again on fail” acceptable UX vs dual? Product decision.
- Should `WavFileConsumer` be optional per `retentionPolicy = never_store` (skip buffering entirely)?
- `VOICE_RECOGNITION` vs `MIC` A/B on Samsung S23 for Whisper accuracy — measure WER.

## 14. References (authoritative)

- `AudioRecord` constructor javadoc — only 44100 Hz guaranteed, `getState()` after construction, `RATE_UNSPECIFIED` route-dependent ([AOSP](https://android.googlesource.com/platform/frameworks/base/+/master/media/java/android/media/AudioRecord.java)) and NDK sampling best practices ([developer guide](https://developer.android.com/ndk/guides/audio/sampling-audio)).
- `RecognitionListener.onBufferReceived` — “There is no guarantee that this method will be called… sample rate is implementation dependent” ([AOSP](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/RecognitionListener.java)).
- `RecognitionService.Callback.bufferReceived` — same note ([AOSP](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/RecognitionService.java)).
- `SpeechRecognizer` javadoc — likely streams to remote servers, not for continuous ([MS mirroring AOSP](https://learn.microsoft.com/en-us/dotnet/api/android.speech.speechrecognizer?view=net-android-35.0) / [AOSP](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/speech/SpeechRecognizer.java)).
- Stack Overflow confirming `onBufferReceived` not called ([link](https://stackoverflow.com/questions/46088077/onbufferreceived-in-recognitionlistener-is-not-called-when-voice-recognition-is)).
- Foreground service types — `microphone` requires `FOREGROUND_SERVICE_MICROPHONE` + `RECORD_AUDIO`, while-in-use + `BOOT_COMPLETED` restrictions ([req types](https://developer.android.com/about/versions/14/changes/fgs-types-required)), ([service types – Microphone](https://developer.android.com/develop/background-work/services/fgs/service-types)), ([declare](https://developer.android.com/develop/background-work/services/fgs/declare)), ([Android 15 changes](https://developer.android.com/about/versions/15/changes/foreground-service-types)), ([MissingForegroundServiceTypeException discussion](https://github.com/invertase/notifee/issues/997)).
- Oboe discussion of probing native rate via `getMinBufferSize` ([google/oboe#95](https://github.com/google/oboe/issues/95)).
- Existing Open Flow implementations: `SessionAudioCapture.kt:26`, `AudioRecordPcm.kt:22`, `AndroidPcmMic.kt:22`, `CloudEar.kt:45`, `OnDeviceEar.kt:55`, `SttEngine.kt:336`, `FlowAccessibilityService.kt:1703`.

