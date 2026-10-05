# Validate model setup storage on Android ext4 and F2FS

Type: task
Status: blocked:human
Reason: Physical Android devices with ext4 and F2FS storage are required; no adb device is connected.
Blocked by: 62

## Question

Obtain and review the Android storage evidence required by
[What verified storage requirement should model setup enforce?](62-model-setup-storage-requirement.md)
before its candidate becomes an enforced requirement. This task unblocks approval of
the storage allowance; it does not implement production preflight or deliver the app.
Consult GLOSSARY.md and the parent ticket's full resolution.

Current verified payload is 171,536,457 bytes; initial candidate is 188,743,680 bytes
(180 MiB), calculated as payload plus 10%, rounded up to a whole MiB. The allowance
is a test hypothesis. [Model storage evidence](../assets/model-storage-evidence.md)
contains exact sizes, hash, source links and lifecycle caveats.

Required evidence/checklist:

- Physical Android app-internal storage covering ext4 and F2FS; record device,
  Android version, filesystem, code revision, archive hash and selected-file metadata.
  Missing coverage stays pending. No requirement to cover all three Device Classes.
- Measure logical and allocated bytes and allocatable capacity at each download,
  verification, extraction, rename and cleanup phase. Distinguish metadata/transient
  overhead from file allocation, and record measurement limits.
- Exercise clean installation, interrupted download retry, interrupted extraction
  retry, incomplete installed files, failed stale cleanup, already-present model,
  and constrained capacity near the candidate (below, at and above).
- Confirm cleanup before crediting reclaimed space; current unchecked deletion
  results do not establish this. Separate installation demand from other writers.
- Test real model writes near the boundary, not merely a simulated preflight
  comparison. Record runtime storage failures and cleanup/retry outcomes.
- Review whether the allowance suffices on tested configurations. If evidence
  requires changing the candidate, bring the revised value and rationale to the
  owner for a decision; do not silently promote the initial hypothesis.

On resolution link the evidence and record the owner-reviewed requirement or any
remaining specific gap. Then update the approximate setup-storage copy to reflect
the approved requirement, retaining distinct download and installed amounts.
A passing preflight is never a reservation or a guarantee against later space loss.
