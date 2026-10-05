# Correct the false claims in the sherpa-onnx provider docs

Type: task
Status: resolved
Blocked by: none

## Question

`docs/providers/sherpa-onnx.md` and `docs/providers/model-selection.md` describe an
API that does not exist. Verified against the upstream source while building the
Gradle skeleton, where the wrong claims were load-bearing: a reader following them
writes code that does not compile.

Four corrections:

- **The class is `OnlineRecognizer`, not `SherpaOnnxRecognizer`.** The old name is
  gone from the tree; `sherpa-onnx/kotlin-api/SherpaOnnxRecognizer.kt` is a 404.
  The real types are `OnlineRecognizer`, `OnlineStream`, `OfflineRecognizer`,
  `OfflineStream`, `Vad`, all in package `com.k2fsa.sherpa.onnx`.
- **The Kotlin API ships *inside* the AAR.** The docs say the sources are "copied
  from `sherpa-onnx/kotlin-api/` into the consuming app". That was true before an AAR
  existed. Upstream now symlinks those files into the AAR module, so the classes are
  in the dependency and copying them is both unnecessary and wrong.
- **Silero VAD is 629 KB, not ~2 MB** (`silero_vad.onnx`, 643,854 bytes; the int8
  variant is 208 KB). Overstated by ~3× in both documents.
- **The version is `v1.13.8`, not `v1.13.5`.** Three releases stale. Upstream's own
  docs page is stale too, which is presumably where the number came from.

Also worth correcting while in there: `OnlineStream.acceptWaveform` takes **two**
arguments (`samples`, `sampleRate`), and the asset-vs-file choice is made by whether
the `AssetManager` constructor argument is null — `newFromAsset` and `newFromFile` are
private externals, not methods you call.

Do not touch the endpoint defaults (2.4 s / 1.4 s / 20 s) — those were checked against
the real `EndpointConfig` and are correct.

The model figures in `model-selection.md` are **right**: the four files that actually
ship are 45,202,074 bytes, which is the "~45 MB" the doc claims. Leave that alone.

## Answer

Done. Every claim in this ticket was **re-verified against the artifact itself**, not
against upstream's written docs — the AAR that ticket 27 already resolved sits in the
Gradle cache, so `jar tf` + `javap` settles signatures that prose cannot.

**One of the four corrections was already satisfied.** `SherpaOnnxRecognizer` appears
nowhere in `docs/`; both documents already said `OnlineRecognizer` / `OfflineRecognizer`
/ `Vad`. The bad name survives only in this ticket and in 27's record of finding it, so
the ticket's premise was one step stale on that point. Nothing to change, so nothing changed.

The three that were wrong, now fixed in `docs/providers/sherpa-onnx.md`:

- **The Kotlin API ships inside the AAR.** `classes.jar` holds **125 classes** under
  `com/k2fsa/sherpa/onnx/` — `OnlineRecognizer`, `OnlineStream`, `OfflineRecognizer`,
  `OfflineStream`, `Vad` among them. The "copied from `sherpa-onnx/kotlin-api/` into
  the consuming app" instruction is gone; it described the pre-AAR world.
- **`v1.13.8`, not `v1.13.5`.** Confirmed as the newest upstream release at the time of
  writing, and it is the coordinate `gradle/libs.versions.toml` already pins. The document
  now names the pinned tag, says why it is pinned (ADR-0007's JitPack-mirrors-a-fixed-version
  reason), and records that upstream's docs page was the stale source of the old number.
- **Silero VAD is 629 KB, not ~2 MB.** `silero_vad.onnx` measures **643,854 bytes** on the
  upstream `asr-models` release, and `silero_vad.int8.onnx` is **212,860 bytes** (208 KB).
  Fixed in `sherpa-onnx.md` and in all three places in `model-selection.md` that repeated
  the 2 MB figure. Worth knowing: the release also carries `silero_vad_v4.onnx` (1.8 MB)
  and `silero_vad_v5.onnx` (2.3 MB) — **the ~2 MB figure is roughly the v5 model**, so the
  number was not invented, it was measured against a different file than the one named.

And the two "also worth correcting" items, both confirmed by `javap`:

- **`acceptWaveform` takes two arguments** — `OnlineStream.acceptWaveform(float[], int)`
  and `OfflineStream.acceptWaveform(float[], int)`. There is no one-argument overload on
  either. `Vad.acceptWaveform(float[])` *is* single-argument, which is the trap: the doc's
  VAD loop was right and the recognizer loops were wrong, so the error would have been
  introduced by pattern-matching from working code.
- **Asset-vs-file is the nullable `AssetManager` constructor argument.**
  `RuntimeInvisibleParameterAnnotations` on `OnlineRecognizer(AssetManager, config)` marks
  parameter 0 `@Nullable`, and `newFromAsset` / `newFromFile` are `private final native` in
  the class file — present, greppable, and not callable. Rewrote all four places that
  described them as methods to call, including packaging option 2 and design consequence 4.

Left alone as instructed: the endpoint defaults (2.4 s / 1.4 s / 20 s) and the ~45 MB
zipformer figure. `docs-check` passes (39 files). `CHANGELOG.md` records the correction,
because the practical consequence — a reader following the old text wrote code that would
not compile — is worth a line above the version this ships in.

**The reusable part:** a document's claims about a third-party API decay silently, and the
decay is invisible because prose still reads as prose. The check is cheap when the
dependency is already resolved: the artifact in the build cache is the ground truth, and
`javap` on it answers signature questions that the vendor's own docs page got wrong.