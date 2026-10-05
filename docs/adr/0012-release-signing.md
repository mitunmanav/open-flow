# 0012: Release signing

Date: 2026-10-03

## Status

Accepted

## Context

`.github/workflows/release.yml` has decoded a keystore and written a
`signing.properties` into `RUNNER_TEMP` since ticket 23, and the file has never
been read by anything. `app/build.gradle.kts` had no `signingConfigs` block at
all, so the `release` build type had no signing config, and the release path
could only ever have produced unsigned artifacts. ADR-0007's toolchain work left
this gap deliberately — a signing config needs decisions the skeleton does not.

The interesting part is what the build does without one, because the reason
`.github/workflows/release.yml` gives for its verification step is wrong in a way that mattered.
The step's comment says it "proves the build consumed the keystore rather than
silently falling back to the debug key". Measured, on AGP 8.13.0:

- `./gradlew :app:assembleRelease` with no signing config **succeeds** and writes
  `app-release-unsigned.apk` (128,850,102 bytes) — unsigned, and reported as a
  success.
- `./gradlew :app:bundleRelease` with no signing config **also succeeds**, and the
  resulting `.aab` has **no signature at all**: 36 entries, none of them a
  `META-INF/*.SF` or `*.RSA` pair. `signReleaseBundle` runs and writes nothing.
- `apksigner verify app/build/outputs/apk/debug/app-debug.apk` **exits 0**.

So there are two separate defects, and the first hides the second. There is no
debug-key fallback to catch — the failure is that nothing is signed, which AGP
reports as success. And the step written to catch a fallback cannot catch one,
because `apksigner verify` accepts a debug-signed APK exactly as readily as a
release-signed one. It asserts that *something* signed the APK, not that the
right key did.

The harm is specific rather than theoretical. An unsigned APK cannot be installed
on a device at all, so a release built this way is published and uninstallable;
and a *debug*-signed release, the failure the step was aimed at, is worse still,
because Android refuses to upgrade an app whose signature changes — a user who
installed it would have to uninstall, losing their history. Nothing in the build
distinguishes these, and the only check in the release path cannot.

## Decision

- **Signing material is a properties file** setting `storeFile`, `storePassword`,
  `keyAlias`, `keyPassword`. It is found at `-Popenflow.signingProperties=<path>`,
  or at `signing.properties` in the repository root when that file exists. The
  keystore it names is never in the repository, and a relative `storeFile`
  resolves beside the properties file so the two can live together outside the
  tree.
- **`signing.properties` is gitignored.** It was not, which is the same reason
  this was never caught: the workflow writes its copy into `RUNNER_TEMP`, so no
  file in the tree was ever at risk.
- **The `release` build type gets a signing config only when material is
  present.** The config is never defaulted and never pointed at the debug key.
- **Release artifacts fail closed.** `assembleRelease`, `packageRelease` and
  `bundleRelease` depend on a `requireReleaseSigning` task that fails unless
  signing material is present or `-Popenflow.allowUnsignedRelease=true` was
  passed. An unsigned build is possible and must be asked for by name.
- **`.github/workflows/release.yml` passes `-Popenflow.signingProperties`.** The app bundle is
  signed by the same wiring with no extra work, because it is the same build
  type — verified: with the properties supplied, the `.aab` gains
  `META-INF/OPENFLOW.SF` and `META-INF/OPENFLOW.RSA`.
- **The verification step compares certificates.** It derives the expected
  SHA-256 from the keystore with `keytool -exportcert` and compares it against
  the signer's digest from `apksigner verify --print-certs`, so a debug-signed
  APK fails. Verified against a throwaway keystore, including the negative
  control that the machine's debug certificate does not match.

## Why these

**Fail closed rather than default.** The alternative — falling back to the debug
config — produces a green build and an artifact signed by a key every developer
on earth shares, published under a release tag. It is also the one failure the
project cannot undo cheaply, because Android will not accept an upgrade signed
by a different key. Refusing costs a flag on the rare occasion someone measures
an unsigned build; being wrong the other way costs every future install.

**A Gradle property rather than an environment variable.** `providers.gradleProperty`
is a declared build input, so it is visible in the invocation, in `--info`
output, and to the configuration cache. An environment variable would be
invisible in all three, which is the property this project has repeatedly wished
for on its own signals — see ADR-0008 on a check that could not report.

**An explicit opt-out rather than "unsigned is fine".** `-Popenflow.allowUnsignedRelease`
is greppable. Every occurrence in a transcript is a person who meant it, and the
guard's error message names the flag, so the way out is discoverable from the
failure rather than from this document.

**Certificate comparison rather than `apksigner verify`.** `apksigner verify`
answers "is this signed?", which is not the question. The question is "is this
signed with *our* key?", and the only place that answer exists is the keystore
the secrets came from. This is the same move as ADR-0007's advice to verify a
vendor claim against the resolved artifact: the check is now able to discriminate
the thing it claims to test.

**Relative `storeFile` resolution.** It keeps the keystore and the secrets that
name it in one directory outside the working tree, which is the arrangement
`.github/workflows/release.yml` already uses and the one a password manager can be told about.

## Consequences

- **`./gradlew assembleRelease` fails for anyone without signing material**, which
  is the intended behaviour and is easy to mistake for breakage. The failure
  message names both ways out. `lint test assembleDebug` — the gate
  `CONTRIBUTING.md` asks for — is unaffected, verified green with no signing
  material in the tree, and so are the `ci` and `android-test` workflows, neither
  of which builds a release variant.
- **Anything that needs to measure a release APK must pass the opt-out.** The
  Size Ceiling task does, and it is ticket 45's to write; this is why ticket 45
  follows this one.
- **The guard covers three named lifecycle tasks.** `installRelease` is not
  guarded: it publishes nothing and cannot install an unsigned build.
- **A malformed properties file fails at configuration time**, naming the file
  and the missing key, and a keystore that does not exist fails by path. A wrong
  password surfaces from `packageRelease` as a keytool error. All three were
  exercised; none of them can produce an artifact.
- **The keystore itself remains the owner's**, and so does where its backup
  lives: `ticket 18` holds the checklist. A lost keystore means no future release
  can be signed with the same key, and Android will not accept an upgrade signed
  by a new one — so it must be backed up before the first tag, not after it is
  needed.
- **What ships is unchanged for V1's artifact list except one removal.** ADR-0010
  still decides the artifact's shape; the `.aab` it ruled out was removed from
  `release.yml` in ticket 45, and nothing here depends on the bundle existing.