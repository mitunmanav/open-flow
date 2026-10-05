# What a shipped OpenFlow APK is

Type: grilling
Status: resolved
Blocked by: none

## Question

ADR-0007 left three release-build questions open on purpose, because they are
decisions rather than facts:

- **Does V1 minify?** `isMinifyEnabled = false` today, and the reason is specific:
  sherpa-onnx ships an **empty** `consumer-rules.pro` while its JNI layer resolves
  classes and methods by name (`Java_com_k2fsa_sherpa_onnx_*`). An R8 build would
  strip or rename them and fail at runtime, in a way nothing in the build catches.
  So the question is not "minify or not" but **what keep rules does V1 need, and how
  do we prove they are correct?** A release build that only ever runs unminified is
  shipping a materially larger APK than it needs to.
- **How is the APK split by ABI?** The AAR carries native libraries for all four ABIs
  (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`). `arm64-v8a` alone covers the
  acceptance gate's hardware; the other three are weight. Does V1 ship one
  universal APK, an app bundle with Play doing the splitting, or per-ABI APKs?
- **What is the size ceiling, and who enforces it?** The debug APK is **129 MB**
  because a debug build strips nothing. The release figure is unknown and unmeasured.
  A size ceiling nobody checks is not a ceiling, and the number belongs somewhere a
  build can fail on it.

These are one decision — *what artifact does V1 actually ship* — and they interact:
ABI splitting changes what minification has to strip, and both change the size number.

The version is **not** open: `v1.13.8` is pinned in
`gradle/libs.versions.toml` and must stay a tag, because upstream's JitPack build
configuration hardcodes the version it mirrors. See ADR-0007.

## Answer

**The ticket's premise was wrong, and that turned out to be the decision.** It asked
whether V1 minifies on size grounds, assuming "a release build that only ever runs
unminified is shipping a materially larger APK than it needs to". Measured, R8's entire
reachable surface is the **1,057,351-byte `classes.dex` — 0.82% of a 128,850,066-byte
APK** — because **99.1% of the artifact is native libraries**, which minification cannot
touch. So the question was never a minification question.

Seven decisions, all recorded in `docs/adr/0010-release-artifact-shape.md`:

| # | Decision |
|---|---|
| Q1 | **GitHub Releases is the distribution channel; no Play dependency; no `.aab` shipped.** An `.aab` is a Play-delivery format — unopenable by any user, and only Play can split it. `release.yml` was publishing one on every release. |
| Q2 | **Hybrid model delivery.** Bundle the 629 KB Silero VAD; download the ~45 MB streaming ASR model on first launch. `model-selection.md` had recorded bundling before anything was measured; int8 ONNX deflates poorly, so bundling costs ~40 MB of compressed APK — more than every other decision here combined. |
| Q3 | **`useLegacyPackaging = true`.** Measured 128,850,066 → **50,977,462** bytes; native libraries deflate to 39.1%. Not recorded as a free win: compressed native libraries must be extracted at install, so peak on-device footprint rises. Download size was judged the number a sideloader actually feels. |
| Q4 | **Minification stays off — and ADR-0007's reason was corrected.** The JNI hazard is real but was never the reason; "there is nothing to strip" is. |
| Q5 | **Ship `arm64-v8a` + `armeabi-v7a` + `x86_64`; drop `x86`.** One universal APK, not per-ABI splits — without Play there is no per-device delivery, so splits would ask a sideloader to know their own ABI. |
| Q6 | **50 MB ceiling on the release APK**, Gradle task, hard fail in CI. Not asserted against debug, which is ~123 MiB by construction. |
| Q7 | **Drop the `.aab`** from `release.yml`. |

### What the measurements settled that no document had recorded

- **Minification with no keep rules deletes `classes.dex` entirely** — an APK of 127.8 MB of
  native libraries and no bytecode — **and `assembleRelease` still succeeds.** Nothing in the
  build catches it. This is the strongest argument for leaving it off, and the reason ADR-0007
  needed amending rather than just pointing at.
- **The JNI hazard is bigger than "sherpa-onnx ships no consumer rules."** The AAR's
  `proguard.txt` is 0 bytes, all 133 exported symbols in `libsherpa-onnx-jni.so` are `Java_*`,
  and there is **no `JNI_OnLoad` and no `RegisterNatives`** — so every binding is a lazy name
  lookup. Cross-referencing symbols against `FindClass`-style name strings: **82 of 122 classes
  (67.2%) are name-addressed from native code** (22 via symbols, 62 via strings, 2 overlapping).
  A one-line keep rule would cover them. **`OfflineDiacritization`, `WaveWriter` and
  `tts/engine/TtsEngine` are named by the native layer but absent from `classes.jar` in this
  version** — unprotectable, because there is nothing there to keep.
- **`libonnxruntime.so` is 69.7% of the `arm64-v8a` payload** (22,249,560 of 31,927,176 bytes).
  The inference runtime, not sherpa's code, is what a user downloads — which is why the size
  lever that mattered was packaging, not code size.
- **Release is 0.27% smaller than debug** (128,850,066 vs 129,197,862). The "debug strips
  nothing" explanation was already the whole story; there was never a release saving hiding
  behind minification.
- **`assets/` is empty only because there is no application code.** `core/` has no `src/`; the
  whole Kotlin codebase is one `internal` probe file. Every projection in this ticket is
  arithmetic on that fact, so the first figure worth publishing comes from an APK built after
  ticket 42 lands, and **it will be larger.**

### Two prerequisites the ticket never asked for, which turned out to be load-bearing

**The distribution channel** was undecided anywhere in the repo, and the ticket offered "an app
bundle with Play doing the splitting" as an option — which presupposes a Play Store exists.
**Model delivery** was never on the ticket at all, yet it is worth ~40 MB compressed and
dominates everything else here. A ticket can ask the wrong question and still be the right
ticket; these two had to be answered before its own three questions could be.

### Graduated, and left as work rather than folded in

- **[45 Apply the release artifact shape to the build](45-apply-the-release-artifact-shape.md)**
  — the build changes for Q3/Q5/Q6/Q7, unblocked. It also carries a trap: `x86_64` must survive
  in **debug**, since `android-test.yml` runs the instrumented suite on an x86_64 emulator.
- **[46 A ModelStore that fetches, verifies and hands over a model by path](46-modelstore-fetch-verify.md)**
  — the mechanism behind Q2, unblocked. Its real decision is **where the expected checksum
  comes from**: one we compute ourselves proves nothing about authenticity, one pinned in the
  repository proves the bytes have not changed since a human read them. Also carries the
  deliberate judgement that **46 and 42 are not blocked on each other**, with instructions for
  reversing that if the assets-vs-path seam proves awkward.
- **[47 Where the first-launch model download sits in the onboarding flow](47-first-launch-download-and-onboarding-order.md)**
  — **this graduates the map's last deliberately-open fog item.** Q2 put a mandatory, slow,
  fallible 45 MB step into first launch that did not exist when ticket 19 left the order open,
  so the order question now has a new term and can be asked.
- **[48 Record the device ABI in the acceptance gate](48-record-device-abi-in-the-gate.md)**
  — the gate records OEM skin and Android version but **no ABI**, which was harmless while one
  universal APK carried all four. Now wired as blocking 36, which parses the Gate Status block.

Nothing in the map's fog remains open, and one decision was ruled **out of scope**: a Play
Store release returns only as a fresh effort, not a resumption.