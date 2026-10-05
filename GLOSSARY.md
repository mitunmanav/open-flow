# Glossary

## Adaptive Dictation Router

The provider-neutral layer that chooses or falls back between STT providers using privacy, language, device capability, connectivity, latency, reliability, and cost signals. In V1 it ships as manual preference + health checks + fallback + logging; automatic ML routing is out of scope.

## Bubble

The floating control the user taps, holds, drags, or cancels to drive dictation. It is the only UI surface during a dictation session, and the product's whole privacy claim rests on it: a floating control that reads nothing is what makes a dictation app trustworthy. It has three states — **disabled** (not present), **idle** (present, not listening), **dictating** — and only the third runs a service. Its position is an Anchor, never a coordinate.
_Avoid_: overlay, overlay window, floating widget

## Anchor

Where the Bubble sits, expressed as a screen edge or corner plus a margin, rather than as a position. An Anchor means the same thing on any display, so it survives rotation and a change of device without being remapped, and it keeps the Bubble out of the content the user is reading. It is also why the Bubble cannot rest at an arbitrary interior spot: dragging snaps to the nearest edge or corner.
_Avoid_: position, coordinates, screen location

## Dictation

One full capture-to-insertion cycle: start a recording, transcribe it, refine the transcript, insert it at the cursor, or recover it. A Dictation has a lifecycle (state machine, see `docs/adr/0002-dictation-state-machine.md`) and exactly one terminal outcome (`Done`, `Cancelled`, `ErrorRecoverable`, `ErrorFatal`).

## Target Snapshot

A chosen insertion destination: its app and the editable field's identifying characteristics. A Dictation captures it before recording; its app determines which per-app settings apply. History retry chooses a fresh destination without changing the stored text. In both cases, the field characteristics determine whether insertion can safely return to the selected destination.

## Dictation Controller

The component that owns the Dictation state machine and sequence: IDLE → PREPARING → RECORDING → TRANSCRIBING → REFINING → INSERTING → DONE, with CANCELLED / ERROR_RECOVERABLE / ERROR_FATAL from any state.

## SpeechProvider

The stable contract for STT engines. Reports `capabilities` (streaming, offline, languages, timestamps, confidence, cost), `prepare()`, `transcribe(request)` as a stream of normalized events (Preparing, Listening, Partial, Final, Failure), `health()`, `close()`. Implementations include FakeProvider (tests), SherpaOnnxProvider (V1), and future cloud adapters.

## TranscriptRefiner

The provider-independent cleanup stage: deterministic normalization → spoken commands → backtracking → filler removal → list detection → dictionary → snippets → app-style formatting. The advanced-refiner slot is reserved (identity in V1). Failure degrades to raw transcript marked unprocessed. See `docs/architecture/refiner.md`.

## Snippet

A user-defined spoken shorthand and its authored text expansion. The expansion may be a phrase, paragraph or multiline block. App style preserves its authored capitalization, punctuation, spaces, line breaks and blank lines exactly.
_Avoid_: dictionary correction, template

## Dictionary Entry

A user-defined spoken trigger and the correct spelling or wording it becomes — `kubernetis` → `Kubernetes`. Its counterpart to a Snippet: the body repairs what was said, whereas a Snippet's body says something the user had not. Both are matched by the same rule, both may span several words, and both bodies are authored, so app style never rewrites them.
_Avoid_: correction, replacement word, autocorrect

## Origin Segment

A run of a refined transcript's text carrying whether it was **dictated** or **authored**. Origin decides one thing only: whether app style may write to that run. It does not decide whether a later stage may match inside it — stage 6's authored bodies are still scanned by stage 7, while stage 7's own insertions never are. A match spanning an existing authored run replaces it wholly, so a segment is never half one and half the other.
_Avoid_: styled segment, protected text, markup

## Safe Text Inserter

The component that finds the target editable field again at insertion time, verifies it is still editable and not sensitive, inserts at the selection (SET_TEXT → PASTE → copy+prompt), restores cursor, and falls back to copy/paste recovery. Sensitive fields refuse before recording. See `docs/adr/0003-safe-text-insertion.md`.

## SpeechEvent

A normalized event from a `SpeechProvider` during a transcription: `Preparing`, `Listening`, `Partial(text)` (cumulative snapshot, conflated), `Final(text, startedAtMs, endedAtMs)`, `Failure(reason, recoverable)`.

`recoverable` means the Dictation can still complete by another route — not that the same call will succeed. A missing model is recoverable because another provider can serve the request.

## Provider State

Lifecycle of a `SpeechProvider` instance: `NOT_PREPARED`, `PREPARING`, `READY`, `CLOSED`. One instance serves one model configuration; language is per request. A `READY` instance is warmed up, not necessarily able to serve — see Provider Health.
_Avoid_: readiness, health, ready

## Provider Health

The readiness verdict `health()` returns about whether a provider can serve a dictation right now, and if not, why: `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, `MODEL_MISSING`. A separate concept from Provider State, which tracks an instance's lifecycle; `READY` and `HEALTHY` are not two values of one enum, and no health value mirrors a lifecycle value. Cheap local truth — V1 does no active network probing.
_Avoid_: ProviderState, health state, ready

## Final Latency

The interval between the end of the user's speech and the `Final` event arriving — what makes a dictation feel instant or late. Derived from `Final.endedAtMs` against the last audio timestamp, so it needs no support from the provider. This is the quantity the Latency Guard watches, and what `docs/providers/model-selection.md` budgets as "Final within ~1 s of endpoint".
_Avoid_: RTF, real-time factor, response time

## RTF

Real-time factor: inference time divided by audio duration — how much faster than real time an engine can decode a file. A **benchmark** quantity, measured against a fixed eval set. OpenFlow never computes it at runtime: a user experiences a duration, not a ratio, and the two are not comparable.
_Avoid_: final latency, speed, performance

## Latency Guard

The rule that folds each dictation's Final Latency into a smoothed per-provider window and, while it sits above a threshold, narrows that provider's Provider Health to `DEGRADED` so the next Dictation ranks it lower. It acts only *between* dictations and steers nothing mid-utterance. Its threshold ships provisionally at 1 s and is explicitly uncalibrated until real measurements exist.
_Avoid_: performance monitor, speed mode, watchdog

## Declared Rate

What a provider charges for audio, declared as micros of USD per second. `null` means it cannot estimate, which is not the same as free — free is `0`, and local providers declare `0`.
_Avoid_: estimated cost availability, pricing flag

## Cost Ceiling

The most a single Dictation may cost, in micros of USD, and no ceiling at all when unset. Compared against the worst case a request could reach, not the cost it turns out to have, so the ceiling holds from the moment a provider is chosen.
_Avoid_: cost limit, budget

## Provider Adapter

A concrete `SpeechProvider` implementation, as distinct from the contract itself. V1 has two, both written by us: FakeProvider for tests and SherpaOnnxProvider for real dictation. Future third-party adapters land as pull requests, not plugins.
_Avoid_: plugin, extension, backend

## Capabilities Honesty

The rule that a declared capability is binding. A provider advertising `partialTranscripts` must emit Partial, a declared language must not fail `UnsupportedLanguage`, and a Declared Rate must be the rate actually charged. Enforced by Contract Tests, because SpeechProvider capabilities drive all app behavior and nothing else verifies them.
_Avoid_: feature detection

## Contract Test

A test that runs a provider's own behaviour against the shared suite in core's test fixtures, asserting its events and capabilities hold together. What makes Capabilities Honesty enforceable rather than aspirational.
_Avoid_: integration test, end-to-end test

## RouterContext

Inputs to one routing decision: `privacyMode` (local-only / cloud-allowed), `offline`, `costCeilingMicrosUsd`, `preferredProviderId`.

## RoutingDecision

The router's output: chosen provider, candidates considered, deciding rule, and the eligible fallback chain — or, when nothing survives, which rule excluded everything and why each candidate went. Recorded on every dictation.

## Distribution Channel

Where a user obtains a build of OpenFlow. It decides which artifacts are meaningful: a GitHub Release can carry an installable APK, while an app bundle is a Play-delivery format that no user can install and only Play can split. Choosing a channel is therefore also choosing who does per-device delivery. V1 is GitHub Releases; see `docs/adr/0010-release-artifact-shape.md`.
_Avoid_: store, storefront, publishing target

## Model Delivery

How a model's weights reach the device: **bundled** inside the APK, or **downloaded** after install. A bundled model works offline at first launch and costs download size forever; a downloaded one costs a first-launch fetch and needs its absence to be reportable. V1 bundles the VAD model and downloads the streaming ASR model, so a Dictation can begin before its ASR model is present — which is what Provider Health's `MODEL_MISSING` is for. Distinct from which model is used, which is `docs/providers/model-selection.md`'s business.
_Avoid_: model size, asset bundling, model loading

## Model Store

The component that supplies a **downloaded** model: fetches the model's pinned archive, verifies it against the SHA-256 pinned in the repository, extracts it into `noBackupFilesDir`, and hands the provider a filesystem path. The archive as a whole is the unit of integrity, while extraction is selective — only the entries the provider loads are written, and that whitelist doubles as the path-traversal guard. A missing model is reported as `MODEL_MISSING`, whose fix is a download prompt rather than a retry. V1: `providers/sherpa`'s `ModelStore`, fetching the streaming ASR model on first launch (ADR-0010); the bundled VAD never passes through it.
_Avoid_: model loader, download manager

## Size Ceiling

The largest release APK OpenFlow commits to shipping, checked by the build and failing it when exceeded. The counterpart to Cost Ceiling on the bytes axis: both are promises the project makes about a resource it cannot fully control, and both exist to be violated loudly rather than quietly. V1 holds 50 MB. It is not asserted against a debug build, which is larger by construction.
_Avoid_: size limit, APK budget, size check

## Signing Material

What a release build needs in order to sign: a keystore, plus the properties file naming it and its passwords. Neither is ever in the repository — `signing.properties` is gitignored and the keystore lives outside the working tree — so a build without Signing Material **fails** rather than producing an unsigned or debug-signed artifact. The counterpart to Size Ceiling in the signing axis: a promise about an artifact nobody may re-sign.
_Avoid_: keystore, signing key, release credentials

## FakeProvider

A test SpeechProvider implementation used to run the whole dictation pipeline without a real speech engine.

## Acceptance Gate

The protocol that decides whether OpenFlow may claim a release. Every required scenario is judged per device class on whether **the pipeline behaved** — state machine, insertion, router degradation, recovery — and never on transcription accuracy, which belongs to the model. Two bars: *Releasable* (a prerelease tag, one class) and *Shipped* (`v1.0.0`, all three). See [the acceptance-gate protocol](docs/quality/acceptance-gate.md).
_Avoid_: test suite, QA pass, release checklist

## Device Class

One of **Pixel-like**, **Samsung-class**, or **Xiaomi-class**, **derived** from a Hostility
Profile rather than assigned beside it: zero hostile answers, one, or two-or-three. Each
name is a hostility tier and the suffix carries no meaning — none of the three is a brand,
and any device lands in whatever tier its measured behaviour earns. Keyboard behavior is
recorded separately and never selects the class; an unsupported ABI excludes a run from
coverage without failing a scenario. See [the classification protocol](docs/quality/acceptance-gate.md#device-class).
_Avoid_: device model, phone type, OEM tier

## Hostility Profile

The three recorded three-valued answers — background-app survival, overlay persistence,
background-microphone restriction — plus the configuration they were observed in. Unjudged,
and deliberately not a judgement: each answer comes from one observable probe on the
device rather than from an opinion about the OEM. It describes a **device in a
configuration**, so changing a probed setting requires re-probing; and a Device Class is
computed from it, so a profile with any `unknown` yields no class at all rather than a
guessed one. See [the probes](docs/quality/acceptance-gate.md#device-class).
_Avoid_: device class, OEM tier, risk score

## Recover Cleanly

The post-conditions a Dictation must satisfy after a deliberately injected failure: no stranded Bubble, no orphaned recording on disk, the transcript discarded or preserved exactly as the privacy policy requires, the clipboard holding what it held before, re-insert from History working or the entry cleanly abandonable, no duplicated text in the target field, and a state trace that reached a terminal outcome. A gate cannot judge this without splitting it from *how* the failure is injected.
_Avoid_: handles errors, recovers gracefully, fails safe

## Gate Status

The machine-readable acceptance-gate record for one specific signed APK: its identity, required scenarios and evidence per device class. Its runs share that APK identity and a testing protocol whose applicability the owner reviews; evidence from other APKs remains historical and cannot fill gaps. Complete coverage requires every required scenario; absent evidence blocks both bars.
_Avoid_: gate results, coverage report, test status
