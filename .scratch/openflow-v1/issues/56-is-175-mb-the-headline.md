# Is 175 MB the headline?

Type: grilling
Status: resolved
Blocked by: none

## Question

Which resource cost should lead on the first-launch Welcome screen and post-setup
Voice model recovery surface, and should storage be checked before fetching?

Three amounts describe the current pinned model delivery:

- **127,887,156 bytes (about 128 MB, decimal)** — the actual network download: the
  full archive containing fp32 and int8 weights. Selective extraction does not reduce
  the bytes fetched.
- **About 45 MB installed** — the int8 files retained after selective extraction.
  This is approximate; exact retained bytes have not been recorded.
- **About 175 MB temporarily needed during setup** — an estimate for the archive
  plus extracted subset coexisting. It is not a measured installation cutoff or a
  guarantee that a phone with this amount available can finish.

[Where the first-launch model download sits in the onboarding flow](47-first-launch-download-and-onboarding-order.md)
settled explicit initiation and disclosure before the tap.
[ADR-0013](../../../docs/adr/0013-first-launch-order.md) left the leading amount open
when this ticket was charted. Its original description of 45 MB as fetched was a
factual error, not a delivery decision; the current guidance is now corrected.

Concrete cases:

- A phone with 60 MB available could retain the installed subset but cannot complete
  installation using the current archive delivery.
- A phone with 640 MB available exceeds the estimated installation requirement;
  storage pressure elsewhere can still change availability during setup.
- A metered user pays for about 128 MB downloaded, even though only about 45 MB remain.

Decisions to settle:

- Which amount leads at the decision point? Download cost, temporary storage need,
  or equal emphasis? Keep all three meanings explicitly labelled; do not imply that
  installed size is the network cost.
- Does the presentation differ between Welcome, recovery and in-progress status?
  The settled permanent cost/source/integrity footer remains visible in every state.
- Should OpenFlow check storage before fetching, reject a known shortfall with a
  storage-settings action, and recheck after the user returns?
- How should the approximate storage estimate be presented without turning it into
  an exact guarantee? Any enforced requirement needs justified model-size metadata
  and headroom; neither a measured peak nor a validated cutoff exists today.

## Comments

### 2026-10-05 — Facts checked before discussion

The original question incorrectly called 45 MB the download and described 640 MB free
as insufficient. The original 4× comparison applies to temporary storage versus the
retained subset, not versus download cost. Corrected above before asking for decisions.

Local evidence: [measured archive and subset](../../../docs/providers/sherpa-onnx.md),
[pinned model specification](../../../providers/sherpa/src/main/kotlin/dev/openflow/dictation/providers/sherpa/modelstore/ModelSpec.kt),
[download/extraction lifecycle](../../../providers/sherpa/src/main/kotlin/dev/openflow/dictation/providers/sherpa/modelstore/ModelStore.kt).
Model Store documentation labels 175 MB approximate. There is no production storage
preflight; the first-launch HTML only simulates a storage failure.

Android supports querying allocatable space before writing. Allocatable space may
include reclaimable cache, so it must not be described as currently free space.
Source: [Android app-specific storage — query free space](https://developer.android.com/training/data-storage/app-specific#query-free-space).
A preflight is a proposed requirement, and cannot guarantee that later writes succeed.

The earlier resolved tickets and first-launch ADR also contain the incorrect 45 MB
network claim; their current guidance needs a factual correction when this resolution
is recorded, preserving the already settled flow and explicit-initiation decisions.

### 2026-10-05 — First round accepted

The owner agreed with both recommendations:

- Welcome and model recovery lead with “128 MB download”, followed by “About 45 MB
  installed · About 175 MB needed during setup”. All amounts remain visible; the
  downloading state shows actual download progress.
- Check storage before fetching. A known shortfall stops the fetch and offers storage
  settings; recheck when the user returns. A passing check does not guarantee later
  writes will succeed, and the displayed storage estimate remains approximate.

Remaining decisions: handling a storage query that fails, and establishing the basis
for an enforced requirement without treating the approximate 175 MB figure as exact.

### 2026-10-05 — Final round accepted

The owner accepted allowing an explicit download when storage cannot be queried,
and deriving the enforced requirement from verified sizes plus justified headroom
instead of using ~175 MB as an exact cutoff. This completed the shared understanding.

## Answer

Resolved through two live rounds on 2026-10-05. The owner accepted all recommendations,
confirming the shared understanding in the second round.

### Presentation

Welcome and post-setup Voice model recovery lead with **“128 MB download”**, followed
by **“About 45 MB installed · About 175 MB needed during setup.”** The download number
uses decimal MB rounded from the pinned archive's 127,887,156 bytes. All three amounts
remain visible alongside the settled source and integrity information in every state.
The running state shows actual download progress with the single settled progress
indicator and named phases. Installed size is never presented as network cost.

The temporary-storage figure remains approximate until validated. It describes space
needed during setup, not space permanently retained, and is not an exact cutoff or a
promise that later writes will succeed. The larger figure does not become the headline
on either entry surface; a known shortage instead gets an explicit actionable message.

### Storage checks and failure behavior

Check storage before fetching. A known shortfall prevents the fetch and offers system
storage settings; preserve the current setup/recovery state and recheck on return.
Fetching still requires an explicit Download tap. A passing check is not a guarantee:
if space runs out during download or extraction, retain the actionable storage error
and retry path rather than reporting success or silently retrying.

If the query is unavailable, show **“Couldn't check available space”**, keep the estimate
visible, and permit an explicit Download tap. Unknown capacity is distinct from a known
shortfall; neither is a successful check. Allocatable space may include reclaimable cache
and must not be described as currently free space.

Derive the enforced requirement from verified archive and extracted-file sizes plus
justified headroom. Do not hardcode the approximate 175 MB display value as an exact
cutoff. No new numeric threshold or headroom amount is chosen by this resolution.
Settling the validated requirement is owned by
[What verified storage requirement should model setup enforce?](62-model-setup-storage-requirement.md).

### Handoff

This resolves presentation and checking policy, not production UI or storage-check
implementation. ADR-0013 records the amendment and links here for the decision detail;
the release-artifact and model-selection docs now distinguish downloaded from retained
bytes. Earlier resolved-ticket guidance is factually corrected, with historical
prototype wording kept as an asset record. The map indexes this resolution once.

Source for storage-query capability and allocatable-versus-free terminology:
[Android app-specific storage — query free space](https://developer.android.com/training/data-storage/app-specific#query-free-space).
