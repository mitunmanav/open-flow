# 0010: The shape of the artifact V1 ships

Date: 2026-10-03

## Status

Accepted

Factual correction 2026-10-05: the downloaded ASR archive is about 128 MB; about
45 MB is retained after selective extraction. Model choice and hybrid delivery are
unchanged. See [Is 175 MB the headline?](../../.scratch/openflow-v1/issues/56-is-175-mb-the-headline.md).

Amends [0007](0007-build-toolchain-and-sdk-levels.md): its stated reason for
`isMinifyEnabled = false` was incomplete, and the correction is recorded there.

## Context

ADR-0007 left three release-build questions open — whether to minify, how to split by
ABI, and what size ceiling to hold the build to — because each is a decision rather than
a fact. None of them had ever been measured, because no release APK had been produced.

They were measured. The measurements contradicted the question as it had been framed.

A release APK built from this repository today is **128,850,066 bytes (122.9 MiB)**, and
a debug APK is 129,197,862 — so the release build is **0.27% smaller than the debug
one**, which is roughly what "a debug build strips nothing" predicts and nothing more.
The composition explains why:

| Component | Bytes | Share |
| --- | ---: | ---: |
| Native libraries, four ABIs, stored uncompressed | 127,640,652 | 99.1% |
| `classes.dex`, compressed | 1,057,351 | 0.82% |
| `assets/` | empty | — |

`assets/` is empty only because there is no application code yet — `core/` has no `src/`
at all and the entire Kotlin codebase is one `internal` probe file. The dex will grow, but
the native libraries will not shrink, and they are the APK.

**R8's entire reachable surface in that APK is 1,057,351 bytes.** The question "does V1
minify" was framed as a size question, and as a size question its maximum possible answer
is 0.82%.

Turning minification on anyway, with no keep rules, deletes `classes.dex` **entirely** — the
resulting APK contains 127.8 MB of native libraries and no bytecode at all — and
`assembleRelease` still succeeds. Nothing in the build catches it.

Two further measurements shaped the decisions. First, the sherpa-onnx AAR's
`proguard.txt` — AGP's consumer-rules filename — is **0 bytes**, and **82 of its 122
classes (67.2%) are addressed by name from native code**: 22 via `Java_*` symbols and 62
via `FindClass`-style class-name strings, overlapping by two. All 133 exported dynamic
symbols in `libsherpa-onnx-jni.so` are `Java_*`, and there is **no `JNI_OnLoad` and no
`RegisterNatives`**, so every binding is a lazy name lookup against the declaring class.
Second, `libonnxruntime.so` is **69.7%** of the `arm64-v8a` payload, so the inference
runtime — not sherpa's own code — is what a user downloads.

## Decision

- **GitHub Releases is the distribution channel; there is no Play dependency, and an app
  bundle is not shipped.**
- **Minification stays off for V1**, and the reason is that there is nothing to strip.
- **Native libraries are compressed** (`useLegacyPackaging = true`), so the APK is
  downloaded deflated and extracted to disk at install.
- **Three ABIs ship: `arm64-v8a`, `armeabi-v7a`, `x86_64`.** `x86` is dropped. One
  universal APK, not per-ABI split files.
- **Model delivery is hybrid**: the Silero VAD model (629 KB) is bundled; the streaming
  ASR model is downloaded on first launch as a 127,887,156-byte archive (about 128 MB),
  retaining about 45 MB of selected int8 files after extraction.
- **A 50 MB ceiling on the release APK**, enforced by a Gradle task wired into CI, failing
  the build.

## Why these

**No Play.** An `.aab` is a Play-delivery format: no user can install one, and Play is the
only thing that can split it into something installable. `.github/workflows/release.yml` was building and
publishing one, so every release shipped an artifact nobody could use. Play also brings a
developer account, review latency, and a requirement for a *hosted* privacy-policy URL —
`PRIVACY.md` is in this repository, not served — none of which buys a solo-maintained,
no-telemetry project anything. The project's stated route to three-device-class coverage is
crowdsourcing, and the audience for that is people who sideload.

**Minification off, because there is nothing to gain.** Keep rules for the JNI layer are
writable — `-keep class com.k2fsa.sherpa.onnx.** { *; }` covers the 82 name-addressed
classes — and they would be needed if this were worth 0.82% of the artifact. Two classes
are worth noting as a trap: `OfflineDiacritization`, `WaveWriter` and
`tts/engine/TtsEngine` are named by the native layer but **absent from `classes.jar`** in
this version, so no keep rule can protect them, because there is nothing there to keep.
Recording this matters more than the setting does: ADR-0007 gave the JNI hazard as the
reason, which reads as a pending fix worth pursuing. It is a real hazard and it is not the
reason.

**Compress the native libraries.** Deflating them takes 128,850,066 to **50,977,462
bytes**, and they compress to 39.1%. This is not free and is not recorded as a free win:
compressed native libraries must be extracted to disk at install, so peak on-device
footprint rises, where today they are mapped directly out of the APK with no extraction.
Download size is the number a user experiences when sideloading a 123 MB file; installed
disk is cheap on hardware running Android 8.0 or later. This is the pre-AGP-3.3 default
and is thoroughly exercised.

**Drop `x86`, keep the rest.** `x86` has no real user device and no CI use, and it is
13,582,056 compressed bytes of pure weight — dropping it costs no coverage at all.
`armeabi-v7a` is kept deliberately: 32-bit-only Android 8.0-era hardware exists at the
low end, and this project's entire coverage strategy is to run the gate on more real
hardware, so dropping a CPU architecture would be a quiet narrowing of the thing the
destination depends on. 11 MB is not worth it. `x86_64` is **not optional**:
`.github/workflows/android-test.yml` runs the instrumented suite on an x86_64 emulator, so dropping it
silently disables the one workflow that exercises release-blocking behaviour before a tag.

**One universal APK rather than per-ABI splits.** Without Play there is no per-device
delivery, so per-ABI files would mean asking a sideloader to know their own ABI. The
packaging change has already collected the large part of that win.

**Hybrid model delivery.** `docs/providers/model-selection.md` recorded bundling the ~45 MB streaming ASR
model before anything had been measured. Int8 ONNX weights deflate poorly, so bundling
costs roughly **40 MB of compressed APK** — more than every other decision in this ADR
combined — against a local-first product whose download tier already exists for larger
models. Bundling the VAD alone keeps first launch offline-capable for 629 KB, costs
essentially nothing, and puts the ASR model in the one place the project has already
accepted downloads. The honest cost is stated below.

**A 50 MB ceiling, in Gradle.** The ceiling's real job is narrower than "keep the APK
small": it is the thing that **fails the build if someone re-bundles the ASR model**, which
is the mistake most likely to be made and least likely to be noticed. Against a projected
~37.9 MB, 50 MB leaves honest headroom for a real UI while a re-bundled model lands near
80 MB and fails loudly. The measurement is a property of the build output, so the
assertion belongs beside the thing it measures rather than in a script that has to be kept
in sync with it. Hard fail, not a warning — a ceiling nobody checks is not a ceiling.

## Consequences

- **First launch needs the network.** Hybrid delivery means the first dictation cannot
  happen offline, which is a real cost against `local-first` and introduces a
  download-progress surface before the user has dictated anything. This is accepted rather
  than hidden, and it is a new first-run screen rather than a silent delay.
- **`MODEL_MISSING` stops being hypothetical.** ADR-0001 defines it as a Provider Health
  value distinct from `DEGRADED` because its fix is a download prompt rather than a retry.
  With a downloaded default model it becomes the common first-run state.
- **`docs/providers/model-selection.md`'s packaging line is superseded** for the streaming ASR model. The
  matrix's download tiers were already correct; only the default was wrong.
- **The projected ~37.9 MB is a projection, not a measurement.** It is computed from
  measured per-ABI compressed sizes, a measured dex, and an application that has no
  application code. The first figure worth publishing comes from an APK built after the
  provider lands, and it will be larger.
- **The ceiling is on the release APK only.** A debug APK is 123 MiB by construction, and
  asserting against it would be a permanently red gate.
- **Play returns only as a fresh effort.** This ADR rules it out for V1; it is not a
  standing prohibition on the project.
