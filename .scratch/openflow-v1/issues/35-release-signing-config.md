# Wire the release signing config

Type: task
Status: resolved
Blocked by: none

## Question

The release path is broken in a way that only a real release run would have revealed,
and ticket 27 deliberately left it alone. Its `verify` job checks the four secret
names; its `build` job decodes the keystore and writes a `signing.properties`, then
runs `assembleRelease bundleRelease` and verifies the APK carries the release
signature.

**Nothing reads that file.** `app/build.gradle.kts` has no `signingConfigs` block, so
the release build is unsigned and the verification step fails. This was ticket 27's
gap — the map assigned it there, and 27 landed without it, which orphans it. It
belongs here rather than in 27's record, because 27's scope was deliberately
"skeleton, versions, and build configuration" and a signing config needs decisions
that are not the skeleton's:

- **Where the config lives and how it stays out of git.** `signing.properties` names
  a keystore path and three passwords. `*.jks` and `keystore.properties` are already
  gitignored, but `signing.properties` is **not** — and it is written by CI into
  `$RUNNER_TEMP`, which is why it has never been committed and never been caught. The
  gitignore needs to cover it, whatever the wiring ends up being.
- **Whether signing applies to the AAB as well as the APK.** Both are built, and Play
  wants the bundle signed with the upload key.
- **What happens when the secrets are absent.** The right answer is that a release run
  without them fails loudly at `verify`, which it already does — so the build must not
  silently fall back to the debug key. There is a step in `release.yml` whose comment
  says it exists to catch exactly that fallback; the wiring must not defeat it.

Partly human-in-the-loop: the keystore itself is the owner's to create and back up,
and where that backup lives is not an agent's decision. The wiring around it is.

## Answer

Wired, verified on every path, and recorded in `docs/adr/0012-release-signing.md`.
**The ticket's premise about the failure mode was wrong, and correcting it is the
most useful thing here.**

### What the build actually did without a signing config

Measured on AGP 8.13.0, in this tree, before changing anything:

- `:app:assembleRelease` **succeeded** and wrote `app-release-unsigned.apk` (128,850,102 bytes).
- `:app:bundleRelease` **also succeeded**, and the `.aab` had **no signature at all** —
  36 entries, not one `META-INF/*.SF`/`.RSA` pair. `signReleaseBundle` runs and writes nothing.
- `apksigner verify app/build/outputs/apk/debug/app-debug.apk` **exits 0**.

So there is **no debug-key fallback to catch**. The failure is that nothing is
signed, which AGP reports as a success. And the step written to catch a fallback
**cannot catch one**, because `apksigner verify` accepts a debug-signed APK exactly
as readily as a release-signed one: it answers "is this signed?", not "is this
signed with *our* key?". The step's comment claimed the stronger property and the
code did not have it — the same defect family as the `paths:` filter on
`docs-check` and as requiring a check nobody has seen pass.

The harm is specific. An unsigned APK cannot be installed on a device at all, so
the artifact is a published release nobody can use. A *debug*-signed release is
worse: Android refuses to upgrade an app whose signature changes, so a user who
installed it would have to uninstall and lose their History. Nothing in the build
distinguished these.

### What was done

- **`app/build.gradle.kts`** resolves a properties file from
  `-Popenflow.signingProperties=<path>`, else `signing.properties` at the repo
  root when it exists; a relative `storeFile` resolves beside it. The `release`
  build type gets a signing config **only** when material is present — never
  defaulted, never pointed at debug.
- **Release artifacts now fail closed.** `assembleRelease`, `packageRelease` and
  `bundleRelease` depend on a `requireReleaseSigning` task that fails unless
  material is present or `-Popenflow.allowUnsignedRelease=true`. The flag is the
  way out, so an unsigned build is something a person asks for by name.
- **`.gitignore` covers `signing.properties`**, which it did not. The reason this
  was never caught is the same one the ticket guessed: CI writes into
  `RUNNER_TEMP`, so no file in the tree was ever at risk.
- **`release.yml`** passes `-Popenflow.signingProperties`, and its verification
  step now derives the expected SHA-256 from the keystore with `keytool -exportcert`
  and compares it to the signer's digest. A debug-signed APK now fails it.

### The AAB question

Answered by measurement: **it is signed automatically, with no extra work**,
because it is the same build type. With the properties supplied the `.aab` gains
`META-INF/OPENFLOW.SF` and `META-INF/OPENFLOW.RSA`. Whether the bundle should
exist at all is not this ticket's — ADR-0010 rules it out and **45** applies that,
so the wiring neither depends on it nor entrenches it. Left in place deliberately,
with a comment in the workflow saying why.

### Verified

Throwaway keystore in `/tmp`, never in the tree: no material → the guard fires with
the message above; `-Popenflow.allowUnsignedRelease=true` → succeeds, output still
`-unsigned`; with material → `app-release.apk`, and the digest comparison matches
while the machine's debug certificate correctly does not. A partial properties file
fails at configuration time naming file and key, an absent keystore fails by path, a
wrong password surfaces as a keytool error from `packageRelease`. `lint test
assembleDebug` — the `CONTRIBUTING.md` gate — is **green with no signing material in
the tree**, and neither `ci` nor `android-test` builds a release variant, so
fail-closed costs CI nothing. `docs-check` and `check_attribution.py` both pass.

### Still the owner's

The keystore and its backup, which is **18**'s checklist and not an agent's call.
Worth repeating from there, because the wiring makes it load-bearing: a lost
keystore means no future release can be signed with the same key, and Android will
not accept an upgrade signed by a new one. Back it up before the first tag.

### One new gap, ticketed

Looking at the release build's inputs for the first time turned up that the **tag
and the artifact's version are independent**: `versionCode = 1` /
`versionName = "0.1.0"` are hardcoded and the workflow passes no version, so
tagging `v0.2.0` publishes an artifact claiming `0.1.0`, and two releases at
`versionCode 1` means the second will not install as an upgrade. Not fixed here —
it is a policy decision (derive from the tag, monotonic counter, or a workflow
input) with a real trade-off around rebuilding a tag. **50** owns it.