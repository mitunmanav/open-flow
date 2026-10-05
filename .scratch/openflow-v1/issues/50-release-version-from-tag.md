# The release version must come from the tag, and it does not

Type: grilling
Status: resolved
Blocked by: none

## Question

`app/build.gradle.kts` hardcodes `versionCode = 1` and `versionName = "0.1.0"`,
and `.github/workflows/release.yml` passes nothing about the version — it resolves
the tag, checks it is semver, and builds. So **the tag and the artifact's version
are independent**: tagging `v0.2.0` publishes an APK that says `0.1.0`, and
tagging `v0.1.0` twice publishes two byte-different artifacts claiming the same
version.

Found while wiring release signing in ticket 35, which is the only thing that has
looked at the release build's inputs. Three things make it a decision rather than
a one-line fix:

- **Distinct releases must increase `versionCode`** as a project policy.
  Android prevents installation of a lower code; an equal code does not inherently
  prevent reinstalling. The original question overstated this restriction; see
  the fact correction in Comments.
- **A rebuild of the same tag must not move the version.** `workflow_dispatch`
  exists to rebuild a tag on hardware nobody had, and ADR-0008 records the gate's
  result against a `version_code` — so a rebuild that reports a different one
  makes an existing Gate Status entry unverifiable.
- **`v1.0.0` is a gate bar, not just a string.** ADR-0008 selects the bar from the
  tag's major version. A hardcoded `0.1.0` means the shipped artifact's own
  metadata can contradict the bar it was released under, which is the same class
  of defect as an artifact whose signature nobody can verify.

So the question is what the policy is: `versionCode` derived from the tag
(`major*10000 + minor*100 + patch`, or similar), a monotonic counter kept in the
repository, or a `versionCode` supplied to the workflow. Each has a different
answer to "what happens when a tag is deleted and re-cut", and this project has
no Play to arbitrate, so nothing external will catch a mistake.

Wiring in ticket 35 makes this reachable — a signed release is now buildable, so
the next tag is the first one where the mismatch can actually ship.

## Comments

### 2026-10-04 — First decision round

The owner agreed with all three recommendations:

- V1 release tags use plain `vMAJOR.MINOR.PATCH`; no prerelease suffixes or build metadata.
- V1 has one release line. Fixes advance the latest version rather than introducing lower-version maintenance releases.
- Published tags and APKs are immutable. A rebuild retains the version and commit; changed published content requires a new version.

Fact correction: Android's downgrade protection rejects a **lower** `versionCode`; equal codes do not inherently prevent reinstalling. Distinct releases should still increase the code as a project policy. Source: [Android app versioning](https://developer.android.com/studio/publish/versioning). Published-version immutability follows [SemVer](https://semver.org/).

Still to settle: the bounded tag-to-code formula and how local/debug/unsigned-CI builds receive their version. No implementation has been authorized by this planning ticket.


### 2026-10-04 — Second decision round

The owner agreed with the bounded formula and explicit release-tag input, including the debug-build defaults. This completes the decision rounds.

## Answer

Resolved through live exchange with the owner, who accepted the recommendations in both rounds. This is the versioning policy for V1:

1. **Plain, canonical tags.** Release tags are `vMAJOR.MINOR.PATCH`, with decimal components, no leading zeroes except `0` itself, and no suffixes or build metadata. Each component is in `0–999`; `v0.0.0` is invalid. Malformed or out-of-range inputs fail explicitly rather than being truncated or normalised.
2. **One release line.** A new release advances the latest published version. Fixes after `v0.3.0` advance that line; they do not publish a lower-version maintenance release such as `v0.2.1`.
3. **The tag determines both APK version fields.** `versionName` is the tag without its initial `v`. `versionCode = major * 1_000_000 + minor * 1_000 + patch`. The bounds prevent overlapping digit ranges, keep every valid code positive, and cap the code at `999_999_999`. Examples: `v0.0.1 → 1`, `v0.1.0 → 1000`, `v0.2.1 → 2001`, `v1.0.0 → 1000000`. Codes increase with version order. There is no workflow-run counter or separately supplied version code.
4. **Explicit release input.** Every release build requires an explicit tag, including local signed builds and unsigned CI checks. Missing or invalid version input fails the release build. Signing permission and version identity are separate inputs: allowing an unsigned artifact does not waive version validation. Ordinary debug builds without release input use `versionName = "0.0.0-dev"` and `versionCode = 1`; these defaults are not release identity or Acceptance Gate evidence for a released APK.
5. **Build the selected tag.** The publishing workflow checks out the exact selected tag and passes that tag to Gradle. The same tag determines the Acceptance Gate bar and the APK metadata; a dispatch input naming an old tag must not build the dispatch branch instead.
6. **Published identity is immutable.** Published tags retain their original commit, and published APKs retain their original bytes. A same-tag rebuild retains its commit, name, and code; rerunning a workflow is not a new release. An identical rebuilt APK may be verified against the published checksum. A different rebuilt APK must not overwrite the published asset: changed content requires a new version. Stable version identity does not promise byte-for-byte reproducible builds.

### Why this policy

A deterministic formula makes the code reproducible from the tag without a repository counter or workflow input that can drift. The trade-off is deliberate: bounded components, no suffix ordering, and one release line. Published Android version codes cannot later be lowered without breaking the upgrade path, so these constraints must be validated before publication.

The Acceptance Gate's existing major-version rule remains in effect: major zero selects Releasable; major one and above select Shipped. In that protocol, “prerelease” means the major-zero maturity bar, not a SemVer suffix.

### Implementation handoff

This ticket resolves policy; the build and publishing workflow still need to implement it. Use one deterministic parser/calculation for explicit release input, check the produced APK's name and code against the selected tag before publication, and exercise both tag pushes and manual dispatch.

The current workflow has two additional concrete defects to address while wiring this policy: `verify` tries to fetch/check out a dispatch tag without first checking out a repository, and the separate `build` job uses an unqualified checkout, so it does not inherit the tag selected by `verify`. Passing a tag to Gradle alone would label the wrong source correctly. The current secret-presence check also has no signing secrets mapped into that step's environment; versioning work must not mistake its failure for invalid version input. These are implementation observations, not additional decisions.

Verification should cover valid examples and boundaries, rejection of suffixes/leading zeroes/missing tags/out-of-range components, debug defaults, unsigned-CI release input, exact source checkout on dispatch, APK metadata matching the tag, and refusal to replace a published artifact with different bytes. Gate record granularity remains owned by [What granularity does a gate record have?](58-what-granularity-does-a-gate-record-have.md); this answer constrains its identity fields without deciding where those fields live.

No newly specifiable decision emerged from the map's remaining recovery-surface fog.
