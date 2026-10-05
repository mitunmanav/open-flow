# Pinned model storage evidence

Measured 2026-10-05 while investigating [What verified storage requirement should model setup enforce?](../issues/62-model-setup-storage-requirement.md). Facts and arithmetic only; no Android device validation or numeric headroom decision.

## Artifact and exact payload bytes

Downloaded the [pinned upstream release asset](https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2) afresh to `/tmp/openflow-storage-pinned.tar.bz2` with `curl -L --fail`. Its SHA-256 is `9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9`, matching [V1Models.STREAMING_EN_20M](../../../providers/sherpa/src/main/kotlin/dev/openflow/dictation/providers/sherpa/modelstore/ModelSpec.kt#L79). The full archive is **127,887,156 bytes**. The release asset is the download/integrity unit; this does not change delivery to a smaller artifact.

Python's `tarfile.open(path, 'r:bz2')` enumerated entries, normalized names with `removeprefix('./').lstrip('/')`, and streamed only the four exact spec paths to `/tmp/openflow-storage-whitelist/<model-name>/`. Each selected entry occurred once. Sizes below are the extracted files' `stat().st_size`, matching their tar headers.

| Selected entry | Logical bytes | Host allocated bytes (`st_blocks × 512`) |
| --- | ---: | ---: |
| encoder-epoch-99-avg-1.int8.onnx | 42,845,182 | 42,848,256 |
| decoder-epoch-99-avg-1.int8.onnx | 539,499 | 540,672 |
| joiner-epoch-99-avg-1.int8.onnx | 259,572 | 262,144 |
| tokens.txt | 5,048 | 8,192 |
| Selected total | **43,649,301** | **43,659,264** |
| Archive | **127,887,156** | **127,889,408** |
| Archive + selected files | **171,536,457** | **171,548,672** |

Units: logical peak is 171.536457 decimal MB, or 163.589913 MiB. Selected retained payload is 43.649301 decimal MB. “About 45 MB installed” remains approximate copy, and “About 175 MB needed during setup” is slightly above payload arithmetic, not a validated enforcement threshold.

All regular archive entries together total 136,398,588 logical bytes; unused regular entries total 92,749,287. The extractor's comment saying selective extraction keeps “roughly 160 MB” off-device is not supported by this measured pinned archive. No code was changed here.

Host allocation was measured on **tmpfs**, with a 4,096-byte filesystem fragment size (`stat -f`). It is a host cross-check only: it does not measure Android ext4/F2FS allocation, filesystem metadata, journaling, quotas, cache reclaim, reserved space, or transient filesystem overhead. Directory allocation and non-file metadata are not included in the allocated totals. A 16,384-byte per-file round-up would give 171,573,248 bytes for these five files, but this is hypothetical arithmetic, not an Android measurement.

## Current lifecycle and peak

Source: [ModelStore](../../../providers/sherpa/src/main/kotlin/dev/openflow/dictation/providers/sherpa/modelstore/ModelStore.kt#L128), [Downloader](../../../providers/sherpa/src/main/kotlin/dev/openflow/dictation/providers/sherpa/modelstore/Downloader.kt#L45), [ArchiveExtractor](../../../providers/sherpa/src/main/kotlin/dev/openflow/dictation/providers/sherpa/modelstore/ArchiveExtractor.kt#L43).

1. A per-instance mutex serializes `ensureModel`. If every selected installed path is a file, return `AlreadyPresent` before any cleanup or download. This is a presence check, not a fresh integrity validation; even zero-length selected files meet this predicate. Existing residual staging/archive files are not cleaned on this branch. Additional model-setup payload is zero.
2. Otherwise delete stale staging and any incomplete installed model, then create root. These operations happen before fetching. Their Boolean results are **unchecked**.
3. Download a new full archive into `<root>/<name>.tar.bz2`. The HTTP client opens a truncating `FileOutputStream` only after successful HTTP status and opening the input stream. There is no byte-range resume or reuse of a previously verified archive. Thus a stale archive is replaced rather than doubled, assuming ordinary file semantics; before truncation it can still occupy space.
4. Hash the full archive by streaming in a 64 KiB memory buffer. No second on-disk hash copy is made. A mismatch attempts archive deletion and returns failure.
5. Extract only the whitelist to `<root>/.<name>.downloading/<name>/`; the archive remains intact throughout extraction. Non-selected entries are read/decompressed but never written to storage. The logical file peak for a clean install is archive + selected files: **171,536,457 bytes**.
6. Check selected paths exist as files, then call `renameTo` into `<root>/<name>/`. These paths share the same root filesystem in the intended layout; this is a directory rename, with no fallback copying and no second extracted-model payload. Rename failure attempts staging/archive cleanup and returns failure.
7. Attempt removal of remaining staging and archive. Successful cleanup retains only **43,649,301 logical bytes** of selected model files.

Download `IOException`, extraction `IOException`, incomplete extraction, and rename failure attempt cleanup. SHA read exceptions occur outside the download/extraction catch blocks. Cancellation and process death can leave staging/archive files. Cleanup deletion failures are unchecked throughout; successful return does not itself prove archive removal succeeded. A partial install or retry therefore has the same clean pipeline peak **only if stale deletion/truncation succeeds**. Failed stale cleanup can leave extra non-whitelisted files and invalidate the archive-plus-selected arithmetic. A storage decision should not credit stale bytes as reclaimed until actual removal/remeasurement succeeds.

The mutex belongs to one ModelStore instance, not the entire process or filesystem. Peak arithmetic assumes one model installation operates at a time. Other writers can reduce available capacity during setup; passing preflight cannot guarantee later writes.

## Evidence limits and candidate formula shape

The repository [ModelStore tests](../../../providers/sherpa/src/test/kotlin/dev/openflow/dictation/providers/sherpa/modelstore/ModelStoreTest.kt) exercise tiny fake-downloader archives and successful host cleanup. They demonstrate the intended selective extraction, idempotence, failure paths, and stale partial replacement. They do not validate real-model Android allocation, storage thresholds, or cleanup refusal.

`adb` is installed at `/home/mitun/Android/Sdk/platform-tools/adb`; `adb devices -l` returned an empty device list. No Android device allocation or low-space measurement was possible in this session. [Model benchmark results](../../../docs/providers/model-benchmark-results.md) records no device measurements; those performance results would not substitute for storage lifecycle measurements anyway.

Potential requirement shape, subject to human decision and validation:

`R = allocated-size estimate(pinned archive) + sum(allocated-size estimate(each pinned selected entry)) + H`

Here `H` must cover justified filesystem overhead and chosen operational allowance; neither an exact value nor device-verified basis exists in this evidence. A simpler `R = 171,536,457 + H` starts from logical bytes and must include file allocation rounding within `H`. Bind the archive byte count and each selected path/size to the same archive SHA-256 and whitelist; regenerate evidence when any changes. Do not bind enforcement to rounded UI MB text or to a Content-Length supplied by an unverified download.

For retries, a query after confirmed stale cleanup/truncation measures actual currently available capacity directly. A query before cleanup cannot safely subtract stale logical file sizes: allocation differs from length, reclaim may fail, and the current implementation does not check success. Existing complete installations require no new model download and should be recognized before imposing a fresh-install requirement.

Missing validation: real pinned-archive ModelStore install on app-internal Android storage with phase-by-phase allocation/available-capacity observations; successful and failed cleanup; interrupted archive and interrupted extraction retries; incomplete installed model; already-present model; low-space behavior near any proposed cutoff; and evidence for the chosen headroom on supported filesystem/device configurations. Runtime storage errors still need actionable handling regardless of preflight.
