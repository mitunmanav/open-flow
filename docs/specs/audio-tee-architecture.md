# Audio Tee Architecture — M3 Spec

## Status
PARKED — no code change. Research + design only.
Source guidance: Android `AudioRecord` docs (single recorder owns mic, app polls via `read()`), `VOICE_RECOGNITION` source, `getMinBufferSize`/`STATE_INITIALIZED`/`startRecording` fail-soft pattern, and existing `SessionAudioCapture`/`AudioRecordPcm`/`AndroidPcmMic` implementations.

## Current ownership (as of 2026-08-27, post Block A/B)

| Owner | Source | Format | Thread | Buffer | Consumers |
|-------|--------|--------|--------|--------|-----------|
| `SessionAudioCapture` | `MIC` | `ByteArray` PCM16 16k mono | `openflow-session-pcm` | `ArrayList<ByteArray>` + `CaptureCap 8MB (~4min)` | WAV file for retry (`WavPcm.wrapPcm16leMono`) only |
| `AudioRecordPcm` (on_phone) | `VOICE_RECOGNITION` | `FloatArray` -1..1 16k mono | `openflow-whisper-pcm` | `PcmChunker` (240k samples =15s) + `TranscriptParts` + internal WAV `wavChunks` 8M cap | Whisper `JniWhisperRuntime` + retry WAV |
| `AndroidPcmMic` (cloud) | `VOICE_RECOGNITION` | `ByteArray` PCM16 16k mono | `openflow-pcm` | none (fan-out via `onChunk` → `CloudEar.writeAudio` → `CloudSocket.send`) | `CloudEar` WebSocket |
| `SttEngine` (system) | `SpeechRecognizer` internal | opaque | main `Handler` | `SpeechRecognizer` internal | `FlowAccessibilityService` `onPartial/onFinal` |

`FlowAccessibilityService.startListening` picks one ear via `SttRouter`/`EarGate` and starts exactly one of the three recorders:

- `on_phone` → `AudioRecordPcm` + internal WAV buffer (no `SessionAudioCapture`) — single owner, honest replay via `transcribeWavFile`.
- `system` → `SpeechRecognizer` + `SessionAudioCapture` — **dual** (recognizer + `MIC` recorder).
- `cloud` (`openai`/`deepgram`/`assemblyai`/`sarvam`) → `AndroidPcmMic` + `SessionAudioCapture` — **dual** (`VOICE_RECOGNITION` + `MIC`).

`EarMicPolicy.bubbleCapturesWav(earId) = earId != "on_phone"` was intended to avoid dual for whisper, but cloud/system still dual. M3 is to eliminate all dual.

## PCM flow today

```
MIC/VOICE_RECOGNITION
  → AudioRecord.read(buf)
  → copyOf(n) on worker thread
  → SessionAudioCapture: chunks.add(copy) under synchronized + CaptureCap.admit
  → stopAndWrite: join chunks → WavPcm.wrap → File(filesDir/audio/<id>.wav)

VOICE_RECOGNITION (whisper)
  → AudioRecord.read → toFloat → onRms → PcmChunker.push → runWork{ transcribeWindows } → TranscriptParts
  → stopAndFlush: pcm.take() → chunker.push(rem) + flush → transcribeWindows → join → onFinal
  → WAV tee: same onSamples callback also floatToPcmBytes → wavChunks (same cap) → writeWav

VOICE_RECOGNITION (cloud)
  → AudioRecord.read → onChunk → CloudEar.writeAudio → CloudSocket.send(bytes)
  → stopAndFlush: pcm.stop() → session.close() → pendingFinish + Handler timeout 5s, early finish on final/error
```

Threading: each recorder owns `AtomicBoolean running` + `Thread` + `AudioRecord`; `stop()` joins 500-800ms, then `stop()`+`release()`. Main thread owns `SpeechEngine.Listener` hops via `MainThreadHop`/`Handler(Looper.getMainLooper())`.

## Problems with dual

- Two `AudioRecord` instances contend for mic. On many OEMs (Samsung/Xiaomi) only one gets consistent frames; the other gets `ERROR_INVALID_OPERATION` or silence, causing “mic busy” or empty WAV.
- Different `AudioSource` (`MIC` vs `VOICE_RECOGNITION`) apply different AEC/NS; mixing sources yields inconsistent quality.
- Two buffers double heap (2×8M) and two threads double CPU/wake, hurting battery and increasing ANR risk during `join(800)` on UI thread.
- `SttEngine` already masks mic with `SpeechRecognizer`; an extra `SessionAudioCapture` adds a second tap without benefit — system path could reuse recognizer’s audio via `onBufferReceived` (API 23) but that is undocumented/unreliable.

## Lifecycle / flush / failure today

- `startListening` sets `listening=true`, `listenGeneration++`, captures `fieldPrefix`, picks ear, starts the chosen recorder.
- `stopListening(save=true)` calls `ear.stopAndFlush(timeout)`:
  - cloud: `stopAndFlush` closes `PcmSource`, schedules `Handler.postDelayed(finish, 5s)`, early finish on `onFinal`/`onError` via `pendingFinish` atomic.
  - on_phone: `stopAndFlush` with `FLUSH_TIMEOUT_MS 120s`, `AtomicBoolean` guard, timeout vs `runWork` transcribe race; chunker remainder + tail transcribed; `TranscriptParts.clear()` after.
  - system: `SttEngine.stopAndFlush` with `Handler` + `flushCallback`, early via `signalFlushIfNeeded` on `onResults`/`onError`.
- Failure: `onError` fatal → `stopListening(save=true)` → `persistFailedSession` writes WAV (or `writeWav` for on_phone) and sets `retrySessionId`; next `startListening` with `retrySessionId` can replay WAV for on_phone (no mic) or re-speak for cloud/system.
- Cancellation: JNI `WhisperLib.fullTranscribe` is blocking and not cancellable; bounded timeout delivers partial `parts.join()` honestly, does not fake `interrupt`.
- Concurrency: `listenGeneration` guards stale `onFinal` during flush; `stopInProgress` blocks re-entrant `startListening`/`stopListening`.

## Battery / latency

- Single `AudioRecord` at 16k mono PCM16 ~32KB/s; 8M cap = 4min head, typical 30s dictation ~1M.
- Cloud streaming needs sub-300ms first-byte latency: `minBuffer = getMinBufferSize().coerceAtLeast(sampleRate/5*2)` (~6400 bytes =200ms) matches recommended `PROPERTY_OUTPUT_FRAMES_PER_BUFFER` multiple.
- Extra recorder doubles wakeups and `read()` syscalls,measurably increasing CPU on low-end devices (see Android latency docs: single path with minimal processing is lowest latency).

## Proposed tee architecture (single owner)

### Single recorder

- One `AppAudioCapture` class owning a single `AudioRecord` with `VOICE_RECOGNITION` at 16k mono PCM16 (canonical for streaming models; `MIC` fallback if `VOICE_RECOGNITION` unavailable).
- Config: `sampleRate=16000`, `channel=CHANNEL_IN_MONO`, `encoding=PCM_16BIT`, `source=VOICE_RECOGNITION`, `bufSize = getMinBufferSize().coerceAtLeast(sampleRate/5*2)`.
- Fail-soft: `try/catch` on construction, `STATE_INITIALIZED` check via `PcmStartPolicy`, `startRecording` try/catch, return `false` → service shows `needMic`.

### Tee / fan-out

- `AppAudioCapture` holds a thread-safe subscriber list: `CopyOnWriteArrayList<Consumer>`.
- Consumers:
  - `WhisperConsumer`: receives `FloatArray` (converted via `WavPcm.pcm16ToFloat` or direct `toFloat`), owns `PcmChunker` + `TranscriptParts`, runs `transcribeWindows` on `whisperExec`.
  - `CloudConsumer`: receives `ByteArray` PCM16, forwards to `CloudEar.writeAudio` → `CloudSocket.send`.
  - `WavFileConsumer`: receives `ByteArray`, buffers under `CaptureCap` (8M) synchronized, writes via `WavPcm.wrapPcm16leMono` on `stopAndWrite`.
- Single `read` loop: `while(running) { n=ar.read(buf); if(n>0) dispatch(copyOf(n) to all consumers) }` — one copy per consumer (`copyOf` already), no extra copies between consumers.
- Backpressure: consumers must not block `read` loop; `WhisperConsumer` enqueues to `whisperExec`, `CloudConsumer` enqueues to socket’s send queue, `WavFileConsumer` does bounded `synchronized` add (fast).

### Ownership

- `FlowAccessibilityService` owns lifecycle: `startListening` → `appAudioCapture.start(subscribersFor(earId))`, `stopListening` → `appAudioCapture.stop()` (single `stop` joins one thread 500ms, not two).
- `EarMicPolicy` removed or becomes `teeEnabled=true` for all ears; `SessionAudioCapture` deleted after migration (or kept as `WavFileConsumer` wrapper for transition).
- `AudioRecordPcm` and `AndroidPcmMic` deleted; their `toFloat`/`rmsDb` logic moves into `AppAudioCapture` or consumers (`WavPcm.pcm16ToFloat` already exists, `rmsDb` for pulse can be computed from `ByteArray` in one place).

### Buffers / caps

- One `CopyOnWriteArrayList` + per-consumer buffer; total heap still capped per consumer via `CaptureCap.admit` for WAV (8M), `PcmChunker` for whisper (15s windows), cloud has no buffer (streaming).
- No global heap growth: wav consumer drops overflow head-kept policy already (admit false → drop incoming, keep head).

### Lifecycle / cancellation / flush

- `AppAudioCapture.start()` clears previous wav buffer and whisper `parts`/`chunker`.
- `stopAndFlush` per ear delegates to consumer flush:
  - whisper: same `AtomicBoolean` + `Handler` timeout vs `runWork` transcribe, but now `take()` returns both `FloatArray` remainder and `wavChunks` for file.
  - cloud: same `pendingFinish` pattern, but cloud consumer already stopped sending on `stop()`, early finish on final/error still via `pendingFinish`.
  - wav: `stopAndWrite` joins wav consumer buffer, writes file, clears.
- JNI still not cancellable; timeout delivers partial, honest.

### Failure / concurrency

- Single `AtomicBoolean running` + `listenGeneration` guard; `stopInProgress` unchanged.
- `AudioRecord.read` returning `-ERROR_*` breaks loop, consumers get `onError` via ear’s listener.
- On `stop()` failure to start (`bufferOk` false, `STATE_INITIALIZED` false, `startRecording` throw) → service shows `needMic` and does not claim listening.

### Battery / latency

- One recorder → one thread, one `read` syscall per buffer, ~50% CPU reduction for cloud/system paths.
- Keep `bufSize` at 200ms (6400 bytes) for sub-300ms cloud first-byte; whisper can tolerate larger chunks (15s windows) because it batches via `PcmChunker`.
- `setListeningAwake(true)` + `FLAG_KEEP_SCREEN_ON` unchanged; no extra wakelock for tee.

### Rollback

- Keep `SessionAudioCapture` and `AudioRecordPcm`/`AndroidPcmMic` classes behind feature flag `useTee` (default false) until tee verified on `of_win` + real devices (Samsung/Xiaomi).
- Migration steps: 1) add `AppAudioCapture` + consumers, 2) gate `startListening` to use tee when earId in set, 3) verify with `gate.sh --quick` + manual dictation 30s/4min + kill + retry, 4) flip flag, 5) delete old classes when `Gate` 3 consecutive passes and no `ERROR_INVALID_OPERATION` in logcat.
- Rollback: flip flag false, old dual paths remain.

## Tests

- Unit: `CaptureCap.admit` boundary, `WavPcm.wrap/unwrap/ pcm16ToFloat` round-trip, `PcmChunker` never drops, `TranscriptParts.join` hallucination, `AppAudioCapture` fan-out delivers same bytes to N consumers (FakeConsumer), `stopAndFlush` atomic guard, `listenGeneration` stale drop.
- Instrumented: no new instrumented needed; existing `FlowAccessibilityService` instrumented 3 tests still pass. Add one instrumented check: start on_phone 5s, stop, verify wav exists and replay yields non-empty.
- QA: `gate.sh --quick` must stay PASS; manual: cloud 30s dictation → stop → verify `filesDir/audio/<id>.wav` exists; on_phone retry replays WAV without mic (verify `transcribeWavFile` log); kill app mid-listen → no leak `dumpsys media.audio_flinger` shows no orphan `AudioRecord`.

## Open questions

- Should `WavFileConsumer` be optional per retention policy `never_store` (skip buffering entirely to save heap)?
- For system ear, can we avoid wav capture entirely when retention is `wipe_24h` and immediately delete after insert?
- `VOICE_RECOGNITION` vs `MIC` choice: test on Samsung S23 (VOICE_RECOGNITION applies extra NS that may hurt whisper accuracy) — A/B with `MIC` fallback flag.

## References

- Android `AudioRecord` API: `getMinBufferSize`, `STATE_INITIALIZED`, `startRecording`, `read(byte[],int,int)` polling, `VOICE_RECOGNITION` source.
- Android latency best practices: single path minimal processing is lowest latency (developer.android.com/ndk/guides/audio/audio-latency).
- Existing implementations: `SessionAudioCapture.kt:26`, `AudioRecordPcm.kt:22`, `AndroidPcmMic.kt:22`, `CloudEar.kt:45`, `OnDeviceEar.kt:55`, `EarMicPolicy.kt:5`.
