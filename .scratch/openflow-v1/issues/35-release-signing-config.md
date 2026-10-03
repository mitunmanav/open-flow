# Wire the release signing config

Type: task
Status: open
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