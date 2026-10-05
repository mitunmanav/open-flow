# What verified storage requirement should model setup enforce?

Type: grilling
Status: resolved
Blocked by: 56

## Question

What validated storage requirement and headroom should OpenFlow use to identify a
known shortfall before downloading the pinned V1 voice model?

[Is 175 MB the headline?](56-is-175-mb-the-headline.md) settled the policy: download
cost leads, storage is checked before fetching, a known shortage stops the fetch, and
an unavailable query permits an explicitly initiated download with an explanation.
The displayed ~175 MB is an estimate, not the enforced requirement.

Before proposing a numeric requirement, verify the pinned archive's byte count and
the exact extracted whitelist sizes. Document the expected peak for the current
download/verify/extract/rename/cleanup lifecycle, including retry and partially
installed states, and distinguish payload arithmetic from measured disk allocation.
Use the existing pinned archive and selective extraction as the delivery constraint;
do not silently redesign delivery to make a smaller number true.

With that evidence, settle:

- The requirement's computation, its headroom and justification, and how each stays
  tied to the pinned model when its archive or selected files change.
- Which relevant storage measurement supports a known-shortfall verdict, and what
  validation evidence is sufficient before enforcing the chosen requirement.
- Whether the validated value requires updating the displayed approximate setup
  amount, while keeping download, retained files and temporary storage distinct.

If device evidence is needed and unavailable, record the specific missing evidence;
do not turn an estimate into an exact guarantee. Passing preflight cannot rule out
space being consumed later, so runtime storage errors remain required.

This ticket resolves the numeric requirement and its validation basis. It does not
implement the production preflight or reopen the settled presentation and unknown-
query behavior. Consult GLOSSARY.md, grilling and domain-modeling.

## Comments

### 2026-10-05 — Verified evidence and first decision round

Claimed as the first open, unblocked, unclaimed child of the map. Consulted grilling,
domain-modeling, GLOSSARY.md, and the settled presentation/storage policy.

[Model storage evidence](../assets/model-storage-evidence.md) records a fresh download
whose SHA-256 matches the pin: archive 127,887,156 bytes; selected files 43,649,301
bytes; clean logical peak 171,536,457 bytes. Host tmpfs allocation is 171,548,672
bytes, excluding metadata; it is not Android validation. No adb device is connected.
Stale cleanup results are unchecked in the current lifecycle, so retry arithmetic
requires confirmed cleanup and one installation at a time. No numeric headroom is
validated, and no production change or ticket resolution has been made.

First round presented to owner:

1. Require Android lifecycle/near-cutoff validation before enforcing a numeric
   headroom allowance, or permit a labelled provisional cutoff? Recommend validation
   first; logical payload is known, sufficient allowance is not. A later round will
   choose the candidate headroom and validation scope.
2. Use StorageManager.getAllocatableBytes for the volume hosting noBackupFilesDir,
   after checking for an already-present model and confirming stale cleanup?
   Recommend yes: do not count deleted bytes until removal succeeds, do not infer a
   shortage from raw free space when reclaimable capacity exists, and keep the
   settled unknown-query behavior and runtime storage errors.

API basis: [Android storage guidance](https://developer.android.com/training/data-storage/app-specific#query-free-space)
and [StorageManager reference](https://developer.android.com/reference/android/os/storage/StorageManager#getAllocatableBytes(java.util.UUID)).
Allocatable capacity can include reclaimable cache; it is not currently free space
and does not reserve capacity against other writers.

### 2026-10-05 — First round accepted

The owner agreed with both recommendations: Android lifecycle/near-cutoff validation
is required before enforcing a numeric headroom allowance; capacity is measured with
StorageManager.getAllocatableBytes for the actual model-root volume after recognizing
an already-present model and confirming stale cleanup. Query failure and runtime
storage errors retain the previously settled behavior.

No numeric cutoff has been approved. Next round concerns a candidate margin for
validation and the minimum evidence coverage; neither is a measured guarantee.

### 2026-10-05 — Second round accepted

The owner agreed to test 180 MiB as the initial candidate, computed from verified
payload plus 10% rounded up to a whole MiB, and to require physical Android validation
covering ext4 and F2FS. Tests cover clean installation, interrupted download/extraction
retries, incomplete installs, cleanup failure and capacity near the candidate cutoff.
The allowance is revised if evidence requires it; missing filesystem coverage remains
pending. This completed the shared understanding of the planning decision.

## Answer

Resolved through two live rounds on 2026-10-05. This chooses the computation and
validation basis, **not a device-validated production cutoff**. No connected Android
device was available; the specific missing evidence is owned by
[Validate model setup storage on Android ext4 and F2FS](67-validate-model-setup-storage.md).

### Verified payload and candidate requirement

[Model storage evidence](../assets/model-storage-evidence.md) records a fresh download
matching the repository-pinned SHA-256, exact whitelist sizes and the current lifecycle:

- Archive: **127,887,156 bytes**.
- Retained selected files: **43,649,301 bytes**.
- Clean logical peak: **171,536,457 bytes**. Archive and extracted files coexist;
  the same-volume rename adds no model copy.

Let A be verified archive bytes, S the sum of verified selected-file bytes, and M be
1,048,576 bytes. The initial test candidate is:

`R = ceil(11 * (A + S) / (10 * M)) * M`

For this pin, **R = 188,743,680 bytes (180 MiB)**, leaving **17,207,223 bytes**
over the logical peak. The 10% allowance is an owner-approved starting hypothesis
for testing filesystem allocation/metadata and operational allowance, not measured
Android overhead. Validate it and revise it if evidence requires; **do not enforce
it as a validated requirement before that evidence is reviewed**. Headroom cannot
guarantee success against space consumed later by other writers.

Keep verified A, each selected path and its size, the whitelist, archive SHA-256,
formula and validation evidence together as model-specific metadata. Any change to
archive bytes, pin or selected files requires regenerated sizes and renewed validation;
changes to the installation lifecycle also require reviewing the peak. Rounded UI
amounts and an unverified HTTP Content-Length are not inputs to enforcement.

### Capacity, retry and already-present behavior

Recognize an already-present model before imposing a fresh-install requirement: that
branch needs no new download payload. For a missing/incomplete model, confirm stale
cleanup and query **StorageManager.getAllocatableBytes** using the UUID of the volume
hosting noBackupFilesDir. Do not subtract stale logical bytes from a capacity query or
credit removal until it succeeds. The current code ignores deletion results, so it does
not yet establish this prerequisite; production preflight work must address it.
The calculation assumes one installation at a time, not overlapping Model Store instances.

After validation, capacity below the approved R is a known shortfall under the chosen
requirement and blocks fetching with the settled storage-settings/recheck path. A failed
query remains unknown and allows an explicit Download tap with the settled explanation.
Allocatable space can include reclaimable cache, so it must not be labelled currently
free space. Runtime storage failures retain actionable errors and retry even after a
passing check. API basis: [Android storage guidance](https://developer.android.com/training/data-storage/app-specific#query-free-space)
and [StorageManager reference](https://developer.android.com/reference/android/os/storage/StorageManager#getAllocatableBytes(java.util.UUID)).

### Required validation and presentation

Require real pinned-archive installations on **physical Android storage covering ext4
and F2FS**, recording allocation and capacity through download, verification, extraction,
rename and cleanup. Exercise clean installs; interrupted download and extraction retries;
incomplete installed models; failed cleanup; already-present models; and capacity near
the candidate cutoff. Record device, Android version, filesystem and model/code identity.
The three acceptance-gate Device Classes are not an additional storage-validation
requirement. Missing filesystem coverage is explicit pending evidence, not a pass.
Host tmpfs allocation and tiny fixture tests do not satisfy this requirement.

Keep download, retained payload and temporary setup storage distinct. The settled
128 MB download headline stays. Current approximate installed/setup copy is not an exact
cutoff; update the setup amount to reflect the approved requirement after validation
(for an unchanged 180 MiB requirement, about 190 MB in decimal rounded copy).
No claim that 175 MB is validated is introduced, and no production UI/preflight is
implemented by this planning resolution.
