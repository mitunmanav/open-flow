# What a shipped OpenFlow APK is

Type: grilling
Status: open
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