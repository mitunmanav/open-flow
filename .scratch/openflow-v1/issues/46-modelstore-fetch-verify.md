# A ModelStore that fetches, verifies and hands over a model by path

Type: task
Status: resolved
Blocked by: none

## Question

Not a question — the mechanism is settled, but it has no owner and every downstream
ticket assumes it exists. This is the work ticket 33 exposed.

ADR-0010 makes the streaming ASR model a **first-launch download**: the Silero VAD model
(629 KB) is bundled, the ~45 MB streaming model is fetched. So OpenFlow needs a thing that
fetches a model archive, verifies it, extracts it into app-internal storage, and hands the
provider a **filesystem path** rather than an asset name.

`docs/providers/sherpa-onnx.md` is explicit that no runtime downloader ships in the
Android artifact — "you write the fetch/verify/extract code yourself" — so none of this is
available from the dependency.

Four things this has to get right, and the ones with teeth are the middle two:

- **Where the bytes come from.** The only documented source is the per-model `.tar.bz2`
  release under `k2-fsa/sherpa-onnx`'s `asr-models` GitHub release. Pinning a URL is a
  supply-chain decision, not an implementation detail.
- **Integrity, and where the expected hash comes from.** There is no signed manifest to
  check against unless one is created. A checksum we compute ourselves proves nothing about
  authenticity; a checksum pinned in the repository proves the bytes have not changed since
  a human read them. Which of those V1 is doing is the real decision here.
- **`MODEL_MISSING` wiring.** ADR-0001 already defines it as a Provider Health value
  distinct from `DEGRADED` "because its fix is a download prompt rather than a retry". With
  a downloaded default model this stops being a hypothetical enum value and becomes the
  common first-run state, so `health()` has to report it truthfully from the start.
- **Extraction.** `.tar.bz2` needs bzip2 decompression and tar, neither of which the
  platform gives an app for free. This is a dependency decision (what compresses it) sitting
  inside a task ticket, and it may deserve its own decision if the answer is not obvious.

## Relationship to 42 — read this before starting either

**These two are deliberately not blocked on each other, and that is a judgement call worth
challenging.** The SherpaOnnxProvider has to accept a model from *either* assets or a
filesystem path — `sherpa-onnx.md` names that nullable `AssetManager` constructor argument
as "the decision point for downloaded-vs-bundled models". So ticket 42 must not be written
asset-only, even though nothing has been downloaded yet.

The cheap way to satisfy both is a small seam: the provider takes a resolved model
*source* (assets or path) and does not care which. Ticket 42 builds that seam with the
assets side exercised; this ticket supplies the path side later. Retrofit the load path
after writing the provider and you do the work twice, so 42's body says this explicitly.

If that seam turns out to be awkward — if the two sources need genuinely different handling
rather than one boolean — the right move is to **block 42 on this ticket** and reverse the
order, not to let 42 ship asset-only.

## Answer

**The ModelStore exists** in
`providers/sherpa/src/main/kotlin/dev/openflow/dictation/providers/sherpa/modelstore/`
— four files (`ModelSpec`, `ArchiveExtractor`, `Downloader`, `ModelStore`)
with 15 unit tests in `src/test/…/modelstore/`, all passing, and the
extraction dependency in the version catalogue. The four things the ticket
said had teeth:

**Where the bytes come from.** The pinned per-model `.tar.bz2` on
`k2-fsa/sherpa-onnx`'s `asr-models` GitHub release — the only source
`sherpa-onnx.md` documents. The URL is a constant in `V1Models.STREAMING_EN_20M`,
not a preference, and it names the release asset, so a re-uploaded asset is a
different byte stream that fails the hash.

**Integrity — the real decision: a repository-pinned SHA-256, not a
self-computed checksum.** The ticket's framing was right, and the answer is
its second option: a checksum computed from the bytes just downloaded proves
only that storage did not corrupt them, while a hash pinned in the repository
proves the bytes have not changed since a human read them. The pinned value,
`9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9`, was
computed from the live release asset on 2026-10-03 (127,887,156 bytes,
confirmed by two independent tools) and committed. A mismatch fails closed:
the download is deleted, `ensureModel` returns `Failed("integrity check
failed…")`, and the model stays absent — which is `MODEL_MISSING`, whose fix
is a download prompt, not a retry. The transport is HTTPS; the hash, not the
transport, is the guarantee. The full archive is the unit of integrity even
though extraction is selective.

**`MODEL_MISSING` wiring.** `ModelStore.isModelPresent()` and
`modelDirectory()` are the truth `SherpaOnnxProvider.health()` reports: an
absent model is `MODEL_MISSING` (ADR-0001), and `modelDirectory()`'s null
*is* the signal. The provider is ticket 42's to write; the wiring point is
its `health()`, and the store was deliberately kept free of Android classes
(`root` is a plain `File`) so the seam is a `File?` and the whole thing is
unit-testable. The app passes `context.noBackupFilesDir` — a ~45 MB model
must never enter Auto Backup.

**Extraction — Apache Commons Compress 1.28.0, and selective extraction.**
Android ships neither tar nor bzip2, and a hand-written bzip2 decompressor is
a bug farm; commons-compress is pure Java, needs no NDK, and is Apache-2.0
like this project. The dependency decision is recorded here rather than spun
into its own ticket because the answer was not obscure. **Selective
extraction**: the measured archive carries fp32 and int8 variants plus
`test_wavs/` (~210 MB unpacked); the spec's `requiredEntries` names exactly
what the provider loads — the int8 trio and `tokens.txt`, ~45 MB — and the
whitelist doubles as the path-traversal guard: only names in the spec are
ever opened for writing, so a crafted archive cannot escape the extraction
root. ~175 MB free is needed during a download (the archive is kept until
extraction succeeds), and progress is reported per 64 KB buffer for ticket
47's first-launch screen.

**Measured facts that correct prose assumptions** (ticket 32's discipline,
applied to the artifact itself): the archive's entries are named
`encoder-epoch-99-avg-1.int8.onnx` — not the `encoder.int8.onnx` shorthand —
under one top-level directory named after the model; the 127,887,156-byte
tarball carries fp32 *and* int8; the extracted int8 subset is ~45 MB. The
layout is recorded in `docs/providers/sherpa-onnx.md` and pinned in code,
and the provider should build its config from `modelFiles()` rather than
retyping names — a filename written twice is a filename that drifts.

**The seam with 42 holds, and needs no reversal.** The sherpa API's nullable
`AssetManager` constructor argument *is* the seam: the provider builds its
`OnlineModelConfig` paths against either an asset prefix (non-null
`AssetManager`) or `modelStore.modelDirectory()` (null `AssetManager`). One
boolean at the constructor call, no sherpa-specific branching in app code —
and the archive's top-level directory name equals the model's
asset-relative name, so the config's path suffixes are identical for both
sources. Ticket 42 should not be blocked on this one.

**Verification, and a collision to know about.** 15 unit tests pass
(`ModelStoreTest`, `ArchiveExtractorTest`, `V1ModelsTest`: download →
verify → extract → hand-over; tampered archive fails closed; idempotence;
download failure; archive-missing-entry fails closed; stale staging and
partial models replaced; whitelist-only extraction including `..` and
absolute-path escape attempts; spec validation) and `lint test assembleDebug`
is green. Because ticket 40's session left `providers/sherpa/src/benchmark/`
mid-write — its sources do not compile, and its build-file wiring compiles
them into the `test` source set — the shared tree's `:providers:sherpa:test`
cannot compile through no fault of this ticket's code, so verification ran in
a scratch copy of the tree with the in-flight benchmark sources excluded. The
commit therefore stages only this ticket's hunk of
`providers/sherpa/build.gradle.kts` (the commons-compress dependency and the
unit-test dependency); ticket 40's uncommitted changes to the same file
remain in the working tree untouched, for that session or the owner to land.
One pre-staged change did ride along in the commit, and it is recorded
here rather than left silent: ticket 31's session had already staged
the rename `31-runtime-rtf-guard.md → 31-latency-guard.md` in the
index before this ticket's `git add`, so the commit includes that
rename — a 100%-similar rename of a resolved ticket's file, staged by
the session that resolved it.
The map's Decisions-so-far pointer for this ticket is likewise left in the
working tree, beside the other sessions' uncommitted map edits.