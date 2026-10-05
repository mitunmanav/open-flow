# The acceptance gate

The protocol for running and recording OpenFlow's acceptance gate: what it covers, how a
run is performed, what counts as a pass, and what a failure has to produce. Decided in
`.scratch/openflow-v1/issues/28-acceptance-gate-protocol.md`; the reasoning is in
[0008-acceptance-gate-two-bars.md](../adr/0008-acceptance-gate-two-bars.md).

**There are no results yet.** No run has been performed, so this document describes how a
run will be done and nothing more. Any number appearing anywhere in this project will come
from a run recorded below, or from the model benchmark results document once that exists.

## Two bars

The gate has two thresholds, and they are never the same claim.

| Bar | Applies to | Requires |
| --- | --- | --- |
| **Releasable** | a prerelease tag (`v0.y.z`) | every required scenario green on **at least one** device class |
| **Shipped** | `v1.0.0` | every required scenario green on **all three** device classes |

A prerelease is not a smaller claim than `v1.0`; it is a different claim, and the tag says
which one is being made. The hardware reality behind this is recorded in the map's Notes:
the project has one or two real devices, so coverage is `N/3` classes until more hardware
exists. Coverage is reported in the `## Gate status` block below at all times, so the gap
between the promise and the evidence is public rather than private.

The three classes are Pixel-like, Samsung-class and Xiaomi-class. They differ in this app
because of **OEM hostility to a floating overlay and a background microphone**, which is
why the class definition below is behavioural rather than a list of brands.

**Crowdsourcing is the sanctioned way to close the gap.** This is an open-source project;
a user on a device class the project cannot afford to buy is not an inconvenience, they are
the missing coverage. The kit in `## How a run is performed` is written for them as much as
for the owner.

The cost of the second bar is a release cadence: a full pass covers every required
scenario on all three classes, with three runs per scenario/class cell. The number of
cells is the required-scenario total × device classes; the number of runs is that product
× three. The planning estimate is roughly 2–3 hours per class. **Prereleases are therefore expected a few times a year, not
weekly.**

## What the gate tests

A scenario passes when **the pipeline behaved** — the dictation state machine
([`0002`](../adr/0002-dictation-state-machine.md)), safe insertion
([`0003`](../adr/0003-safe-text-insertion.md)), the router's degradation and fallback
([`0004`](../adr/0004-adaptive-dictation-router.md)), and the recovery paths.

A scenario is **never** failed for transcription accuracy. Word error rate is a model
property and is measured separately, against
[`docs/providers/model-selection.md`](../providers/model-selection.md) and its results
document. Mixing the two would make this gate slow, subjective, and a duplicate of a
measurement that already has a home.

## The scenarios

Each scenario has a stable ID. The live [Gate status](#gate-status) block's
`required_scenarios` lists the required IDs; derive the total from that list. The
documentation-validation requirement is exact agreement between that list and the
scenario table IDs in this section, rather than matching a repeated prose numeral.
`docs-check` enforces it, as the `gate-scenario*` rules in
`.github/scripts/check_docs.py`.

### Content, on the bundled English default

| Id | Scenario | Passes when |
| --- | --- | --- |
| G1 | Short chat | One sentence into a messaging app's compose field, text lands at the cursor. |
| G2 | Long paragraph | ≥60 s of continuous dictation in one insertion, no mid-stream stall, no truncation. |
| G3 | Names & jargon | A dictionary entry or a snippet is applied to the spoken term. It passes on the entry being applied, **not** on the spelling or the expansion being right. |
| G4 | Numbers & dictated formatting | The refiner's style stage converts as configured, and the setting is honoured. |
| G5 | Self-correction | "send it to Sarah no wait Mark" resolves to Mark, per the backtracking rule in [`docs/architecture/refiner.md`](../architecture/refiner.md). |
| G6 | Lists | Spoken enumeration is detected and formatted as a list. |
| G7 | Noisy room | Live reading in real background noise. VAD endpoints the utterance correctly and nothing is dropped. This is the one scenario that must be read aloud rather than played. |

### Download tier

These three were originally "weak connection", "no connection" and "multiple languages".
OpenFlow transcribes on-device, so the network is not on the path those scenarios were
written to stress — they passed or failed for reasons that had nothing to do with the app.
Their real content was the model store and a second language, and that is what they test now.

| Id | Scenario | Passes when |
| --- | --- | --- |
| G8 | Model download in the clear | A non-default model downloads, verifies and loads. This is the precondition for G9 and G10. |
| G9 | Model download with a bad or absent network | The download fails **visibly and specifically**, stored state is not corrupted, and the app remains usable. Silence is a failure here; a clear failure is a pass. |
| G10 | Mixed-language on the bilingual tier | The zh+en download tier handles a mixed utterance. This is a download-tier test and must never be reported as evidence about the bundled default, which is English-only. |

### Mechanism

The original ten were all *content* scenarios — what you say. The mechanism scenarios are about what the
app does under stress, which is where the risk in
[`0002`](../adr/0002-dictation-state-machine.md) and
[`0003`](../adr/0003-safe-text-insertion.md) actually lives.

| Id | Scenario | Passes when |
| --- | --- | --- |
| G11 | Cancel mid-dictation | Nothing is inserted, the recording is deleted, and the app returns to idle. |
| G12 | Interruption mid-dictation | Focus loss, screen off, or an inbound interruption mid-session is survived; the bubble is not stranded and the state reaches a terminal outcome. **Rotation** mid-dictation is survived too: the dictation continues rather than cancelling, the bubble re-anchors to the same screen edge on the new display metrics, and the text still lands in the field the dictation started in. **Keyboard**: the bubble stays reachable and tappable while the keyboard is up. Riding above the keyboard or staying put is a contextual observation recorded in G12 evidence, not a Device Class input. Both can pass when the bubble remains reachable and tappable. A bubble left behind the keyboard, or one that jumps somewhere else unexpectedly, fails. |
| G13 | Hostile insertion target | A sensitive field refuses **before recording starts**; a field that rejects the paste leaves the clipboard as it was found. |
| G14 | Recovery re-insert | After a recoverable failure the entry resurfaces in History and re-inserts, or is cleanly abandonable. This is where "recover cleanly" is exercised. |
| G15 | Router degradation and latency guard | The router's degradation path runs: provider-reported health changes the chosen provider or ranks it last, and a dictation whose Final Latency crosses the provisional guard threshold narrows that provider's verdict to `DEGRADED` for the next dictation. It passes on the transition, not on the threshold's value — whether 1 s is right is the benchmark's question. Injected via `FakeProvider` (ticket 43), which must be able to script a deliberately slow outcome; the guard's verdict and reason are in the router decision log. |

### G12 keyboard evidence

Record the keyboard's name/version, docked or floating mode, display orientation and
Bubble Anchor with the G12 prose evidence. For each of the three runs, record the
observed motion and whether the Bubble remained reachable and tappable. Include any
context changes during the run, such as rotation. Report an unavailable detail as
unknown instead of guessing it; a movement observation alone does not establish that
the window did or did not receive usable IME insets.

This observation explains the tested configuration; it neither assigns a Device Class
nor changes the existing three-run/two-pass rule. Keep the context and observations
in the prose evidence; Gate Status retains the existing G12 pass/fail results. No
keyboard field or schema-version change is introduced. The decision is recorded in
[Is the keyboard's insets behaviour a fourth Device Class question or a recorded observation?](../../.scratch/openflow-v1/issues/59-is-the-keyboards-insets-a-fourth-device-class-question.md).

## What "recover cleanly" requires you to see

The phrase in the destination was the least objective wording in it, so it is split in two.

**How a failure is injected** — a documented, repeatable procedure:

- force-stop the app mid-dictation;
- revoke a permission while a session is open;
- start a dictation with a sensitive field focused;
- make the target app reject the paste;
- take an inbound call mid-session;
- turn the screen off mid-session;
- fill the storage the model store writes to, then attempt a download.

**What you check afterwards** — the post-conditions, and every one of them must hold:

- no stranded bubble: the overlay is gone, or sitting in a defined idle state;
- no orphaned recording left on disk;
- the transcript is either discarded or preserved exactly as
  [`docs/privacy/privacy-policy.md`](../privacy/privacy-policy.md) says it should be;
- the clipboard holds whatever it held before;
- re-insert from History works, or the entry is cleanly abandonable;
- no duplicated text in the target field;
- the state trace reached one of the terminal outcomes in ADR-0002.

## How a run is performed

The verdict is always a human's. The scenarios dictate into apps this project does not
control, and no amount of tooling can decide whether Gmail's compose field received the
right text. `adb` removes tedium and produces evidence; it never renders the verdict.

The same checklist serves the owner and an external tester. A crowdsourced run and an owner
run are the same kind of artifact, which is what makes them comparable.

Setup, repeatable every run:

1. Install the exact build under test and clear app state first.
2. Fix the content: read from a fixed sentence script, so differences between runs are the
   app's and not the speaker's. G7 must be read aloud in a real room — a clip played
   through a speaker is a worse microphone test than live reading, and the two are not
   interchangeable.
3. Note the device: the Hostility Profile from [Device class](#device-class) when this is
   a new class assignment, plus OEM skin, Android major version, model, and
   the ABI the build ran on — see [What counts as a run](#what-counts-as-a-run).
   The probes are run **once per class assignment**, not once per run.
4. Grant permissions as a first-time user would, through the contextual flow, not by
   pre-granting in `adb shell pm grant`.
5. Record the full commit SHA of the reviewed protocol used for testing. All active runs
   use that protocol; substantive requirement changes require fresh runs.
6. Run each scenario, and record each run.

Evidence worth capturing, on failure at minimum: `adb logcat -d` around the failure, and
`adb exec-out screencap -p` of the target app.

## Device class

Class is decided by behaviour, because that is what differs between an overlay and a
background microphone. Brand is recorded but is not decisive.

**The class is derived from a recorded Hostility Profile, not assigned beside it.** The
profile is three three-valued answers plus the configuration they were observed in; the
class is a published function of the answers. Nothing here asks you to judge how
aggressive an OEM is — each answer is one observable event on your own device.

### The three probes

Run these once per class assignment, not once per scenario run. Each answer is `yes`
(hostile), `no` (not hostile) or `unknown`.

1. **Background survival.** Start a dictation, send OpenFlow to the background, turn the
   screen off, wait **10 minutes**, return and complete a dictation **without restarting
   OpenFlow**. `yes` if it did not survive. The interval is a provisional threshold, not a
   calibrated one — a labelled provisional number beats an invented constant, and beats
   leaving the axis unmeasured.
2. **Overlay persistence.** Grant "display over other apps" through the app's own flow,
   **reboot the device**, then check the permission is still granted and the bubble draws
   over another app. `yes` if either failed to survive or to draw.
3. **Background microphone.** Start a dictation, background OpenFlow, then resume and
   finish it. `yes` if it returned no usable transcript while the same dictation works in
   the foreground. **This one cannot be answered by reading settings** — it is discovered
   by running, which is why it is a probe and not a question.

An inconclusive probe is retried; only then is it recorded `unknown`.

### How the answers become a class

Count the hostile answers:

| Hostile answers | Device Class |
| --- | --- |
| 0 | `pixel-like` |
| 1 | `samsung-class` |
| 2–3 | `xiaomi-class` |

The three classes are the least-, middle- and most-hostile profiles. Count rather than
worst-axis, so one flaky probe moves a device one step instead of reassigning it
wholesale. The names are historical: each is justified by reference-device behaviour,
measured when that hardware exists. **The suffix carries no meaning** — none of these is a
brand, and a device of any brand lands in whatever tier its behaviour earns.

Record the OEM skin (`HyperOS`/`MIUI`, `One UI`, stock/AOSP), Android major version,
device model and ABI alongside. Keyboard behavior belongs to G12 evidence and does not
add a fourth classification question or reassign the class.

### An incomplete profile is not a class

A profile with any `unknown` yields **no class at all**. The device is excluded from
coverage and reported as unresolved — never a fourth tier, never a defaulted answer.
Defaulting `unknown` to non-hostile would credit a device nobody measured; defaulting it to
hostile would narrow the gate silently. A checker must **refuse to derive a class from an
incomplete profile** rather than guess, exactly as it refuses an unknown `gate_version` or
an unrecognised `abi`.

### Configuration is part of the identity

The profile describes a **device in a configuration**, not a bare model — the same move
that made the Bubble an `Anchor` rather than a coordinate. Record the four settings that
can move an answer: battery-optimisation exemption, autostart, per-app microphone
permission, and unrestricted battery mode.

There is no purity rule about defaults. A tuned Xiaomi honestly lands in a lower tier and
the values that put it there are visible, which is the point of recording them: a reader
sees *why* a device reached the tier it did. There is deliberately no rule excluding a
device for having been tuned, because that would exclude the handsets most likely to
volunteer a run.

**Any change to one of those four settings, or any Android major-version change, requires
re-probing. If the derived class changes, that device's earlier cells no longer count
toward coverage.** Results bind to the conditions they were produced under — the same
discipline the record already applies to APK identity and protocol revision.

The rule is settled in
[How do the three behavioral answers select a Device Class?](../../.scratch/openflow-v1/issues/64-device-class-assignment.md),
which also records the rejected alternatives. The earlier brand/settings examples did not
supply a rule and are superseded as classification guidance.

## What counts as a run

Three different acts get conflated here — and **run** means one specific thing in this
document, so the other two need different words. The gate needs all three kept apart:

- **Recorded** — a fact about the device, written down unjudged. OEM skin, Android version,
  model, ABI. None of them says anything about how OpenFlow behaves, so none of them is
  scored.
- **Counted** — a run earns a place in the two-of-three cell that decides a scenario's
  verdict. This is the only place the ABI has any effect.
- **Judged** — the scenario passes or fails, on the pipeline and on nothing else.

**The ABI is recorded, and an ABI OpenFlow does not ship is not counted.** ADR-0010 ships
`arm64-v8a`, `armeabi-v7a` and `x86_64`. A device on any other ABI cannot install the build
at all — the installer fails with `INSTALL_FAILED_NO_MATCHING_ABIS` — so such a run is not a
run that failed, it is not a run. It is deliberately **not** a scenario failure either:
failing G1 because the phone is the wrong architecture would be the same error as failing it
for transcription accuracy, and this gate does not do that to anything.

An `abi` the gate does not recognise **voids the cell** rather than defaulting to
acceptable. `arm64`, `aarch64` and `64-bit` are exactly the inputs where a
tolerate-anything check would quietly pass, and ADR-0010's list is the authority rather than
the spelling of what somebody typed. Read it off the device with
`adb shell getprop ro.product.cpu.abilist` and record the first entry OpenFlow ships.

**The three runs of a cell are the same device.** The cell is the unit this record keeps, so
one ABI describes all three; swapping phones mid-cell is a second cell, not a fourth run.

### The emulator is not a Device Class run

`.github/workflows/android-test.yml` runs the instrumented suite on an `x86_64` emulator
against a **debug** build, and that workflow is the reason `x86_64` is in ADR-0010's list at
all. It is not coverage, and it must never be written into the Gate Status block: it has no
Device Class, because no OEM skin is being tested, and it runs the **`pixel_6` emulator
profile**, so it is shaped to look like the one class a reader would most want the gap
closed with.

That is the whole reason this section exists. The ABI OpenFlow ships with no real device
behind it is the ABI an emulator supplies, so the two facts sit exactly where the coverage
gap sits. **`x86_64` is retained for a CI workflow, which is a different claim from a device
class tested.**

## Runs, results and flake

**Three runs per cell. Pass is two of three. All three are recorded.** This is the same
discipline the model benchmark already uses (≥5 runs, take the median), so the project does
not hold two rules for measuring things.

Recording all three is what makes flake visible as a property rather than noise. A scenario
green 2/3 forever is visibly shaky. A scenario that flakes on one class and not another is
itself a finding. **Unlimited retries are not a substitute** — they measure nothing.

A failing cell records the three runs, not one. A single unexplained failure is not yet a
failure.

### Identity and payload

Every run carries the **identity of the exact signed APK under test**: its intended
release tag, `versionCode`, source commit SHA and APK SHA-256. The active record stores
that identity once; every device, scenario cell and run in it inherits those values.
Identity must match the installed APK. A result from another APK cannot fill a gap,
even if it has the same version or source commit.

External testers may use an official signed candidate before publication, or a published
release APK, with that identity supplied alongside it. Ordinary debug builds are not
gate evidence. The candidate is available for testing before the gate permits release;
tag pushes supply signed candidates as Actions artifacts retained for 90 days.
Download requires GitHub sign-in; candidate links carry their identity and expiry.
Publication is a separate manual promotion of the tested bytes. The settled plan is in
[How does publication select reviewed gate evidence and the tested APK?](../../.scratch/openflow-v1/issues/63-select-reviewed-gate-evidence.md).

On failure, the artifact is the **on-screen transcript, the router decision log, and the
state trace**. ADR-0004 records a decision on every dictation — including, when rule 6
narrowed the verdict, the guard's verdict and why — and ADR-0002 records the state, so
both already exist and the tester only has to paste them.

**Never the audio.** OpenFlow deletes recordings after processing and this project has no
telemetry; a gate that retained raw recordings would be a second retention path in the one
place nobody was looking. And because this gate is public — runs are committed, and
crowdsourced runs are filed in public issues — the checklist tells testers in their hands to
dictate **text they wrote themselves**. A real message pasted into a public issue would
contradict the privacy policy with nothing to stop it.

## Where runs live

One canonical document, with one live status JSON describing one active APK. Append
the results and their evidence in a PR, with a table row per scenario and detail blocks
below it. When selecting another APK, preserve earlier records in dated historical
sections with their APK identities and original requirement context. Historical records
do not contribute to active coverage; changing the APK clears its active results.
GitHub issues are the **intake** channel for crowdsourced runs; the owner folds results
for the same APK into its record in the same PR. No generator and
no metrics database — a gate whose results live in a system that needs maintaining is a
gate that stops being run.

## Gate status

The block below is the machine-readable half of this document, and it is the **only** input
the release checker parses. It is written for a parser and embedded in the human record on purpose: one file,
so a coverage number and its prose cannot drift apart, and a reworded paragraph cannot
change the gate. The release workflow reads it; it never reads the tables above.
Documentation validation separately checks the required IDs against the scenario tables.
Both checks are implemented: `.github/scripts/check_gate.py` resolves coverage for a
release, and the `gate-scenario*` rules in `.github/scripts/check_docs.py` compare the
registry with the tables. The contracts they enforce are settled by
[Should the scenario count be checked or derived?](../../.scratch/openflow-v1/issues/57-should-the-scenario-count-be-checked.md).

```json
{
  "gate_version": 6,
  "required_scenarios": [
    "G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8",
    "G9", "G10", "G11", "G12", "G13", "G14", "G15"
  ],
  "as_of": null,
  "tag": null,
  "commit": null,
  "version_code": null,
  "apk_sha256": null,
  "protocol_commit": null,
  "protocol_reviewed": false,
  "classes": {}
}
```

No active APK has been selected, so `as_of` and the identity fields are `null`, and
`classes` is empty because no run has been performed. Selecting a candidate records
its identity before results arrive; empty `classes` still qualifies for neither bar.
An empty `classes` is not a pass; it is an absence of evidence, and the
enforcement in ADR-0008 treats it as a block for both bars.

### The shape of a populated block

`gate_version` is **6**. Version 1 had no defined per-class entry; version 2 defined
scenario result cells, version 3 added the required-scenario registry, version 4 added
exact APK identity, version 5 adds the testing protocol revision and owner-reviewed
compatibility, and version 6 adds the **Hostility Profile** a class is derived from. A
checker must
read the version and refuse a version it does not know rather than guess at a shape.
The example below carries version 6 for the same reason: a tester filling a block by
hand from this shape would otherwise hand the checker a record it refuses.

**Build, device and result values in the example are placeholders. This is the shape of
a populated block, not a result.** The schema version and required IDs describe the
protocol; no run has been performed and no result is a measurement.

```json
{
  "gate_version": 6,
  "required_scenarios": [
    "G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8",
    "G9", "G10", "G11", "G12", "G13", "G14", "G15"
  ],
  "as_of": "<YYYY-MM-DD>",
  "tag": "<vMAJOR.MINOR.PATCH>",
  "commit": "<full APK source commit SHA>",
  "version_code": 0,
  "apk_sha256": "<64 lowercase hexadecimal characters>",
  "protocol_commit": "<full reviewed testing protocol commit SHA>",
  "protocol_reviewed": true,
  "classes": {
    "<device class>": {
      "oem_skin": "<recorded skin>",
      "android_major": 0,
      "device_model": "<recorded model>",
      "hostility": {
        "background_survival": "<yes | no | unknown>",
        "overlay_persistence": "<yes | no | unknown>",
        "background_microphone": "<yes | no | unknown>"
      },
      "config": {
        "battery_optimisation_exempt": "<yes | no>",
        "autostart_allowed": "<yes | no>",
        "per_app_mic_allowed": "<yes | no>",
        "unrestricted_battery_mode": "<yes | no>"
      },
      "scenarios": {
        "<Gid>": {
          "abi": "<one of arm64-v8a, armeabi-v7a, x86_64>",
          "runs": ["<result>", "<result>", "<result>"]
        }
      }
    }
  }
}
```

- `tag`, `commit`, `version_code` and `apk_sha256` identify the one active APK and
  are required when recording evidence. `tag` and version code follow
  [The release version must come from the tag, and it does not](../../.scratch/openflow-v1/issues/50-release-version-from-tag.md).
  `commit` is the full source SHA used to build that APK, not the commit containing
  the test results. `apk_sha256` is the SHA-256 of the signed APK bytes. Version/source
  identity alone does not prove two APKs are identical.
- `protocol_commit` identifies the reviewed protocol used for all active runs.
  `protocol_reviewed` is the owner's confirmation that these runs apply to the protocol
  at the selected evidence revision. The evidence PR explains wording-only differences;
  substantive scenario, pass-criteria or Device Class rule changes require clearing
  active results and fresh runs. Automation validates the attestation, not semantic
  equivalence. A zero-coverage record identifies the intended testing protocol too.
  With no active candidate, protocol identity is null and review is false.
- `as_of` is the record's update date. The evidence revision is separate from APK
  source identity and testing protocol identity; select a full reviewed main-history
  evidence SHA explicitly during manual promotion. The lifecycle is settled in
  [How does publication select reviewed gate evidence and the tested APK?](../../.scratch/openflow-v1/issues/63-select-reviewed-gate-evidence.md).
- A class key is one of the three in [Device class](#device-class), spelled
  `pixel-like`, `samsung-class` or `xiaomi-class`. Never an emulator. **The key is
  derived, not asserted**: a checker recomputes it from `hostility` by the published
  count and **refuses a record whose key disagrees** with its own answers. A class entry
  with no `hostility` is incomplete, not merely terse — the key would be unfalsifiable.
  The `<device class>` and `<yes | no | unknown>` values in the example above are
  placeholders and **do not** derive to each other; a real record's key and answers must
  agree.
- `hostility` holds the three three-valued answers from
  [the probes](#device-class), and is what the class is derived from. Any `unknown` means
  the entry carries **no class at all**: the device is excluded from coverage and reported
  as unresolved. A checker must refuse to derive a class from an incomplete profile rather
  than default the unknown — crediting an unmeasured device would invent coverage, and
  treating it as hostile would narrow the gate silently.
- `config` holds the four settings that can move an answer, so a reader can see **why** a
  device reached the tier it did. There is no defaults-only purity rule: a tuned device
  honestly lands in a lower tier with its configuration visible. Changing one of these
  settings, or changing the Android major version, requires re-probing, and a **changed
  class invalidates that device's earlier cells**.
- `required_scenarios` is a non-empty list of unique stable G-IDs. It is protocol
  metadata, maintained when the required scenarios change, not evidence a tester invents.
  Documentation validation must reject duplicate IDs or any mismatch with the tables
  under [The scenarios](#the-scenarios); it does not scan historical prose for numerals.
- A recorded scenario key must belong to `required_scenarios`. A class is complete only
  when every required ID has a qualifying passing cell; equal counts alone do not prove
  completeness. Missing results leave coverage incomplete; unexpected IDs invalidate
  the record rather than replacing a missing requirement.
- `runs` holds the three results in the order they were run, each the string `pass` or
  `fail`. Two of three is the pass. The failure's artifacts — transcript, router decision
  log, state trace — stay in the prose below the table, where they are already specified;
  this block carries the verdict and not the evidence.
- `abi` is required on every cell, for the reasons in
  [What counts as a run](#what-counts-as-a-run). A cell without one is a cell that cannot
  be checked, and here too the absence of evidence is not a pass.

**Only one `json` fence is the active record.** The live
block is the first `json` fence after the `## Gate status` heading, before any `###`
subsection; the one below is the shape. A parser takes the first and must not take the last.
Historical sections, even when they preserve earlier JSON, are excluded too. This is
stated here rather than left to be discovered because a checker that silently
reads the wrong fence is a checker that reports on the example.

The shape stays this small on purpose. Every value is typed in **by hand**, by a tester
following a checklist, because there is no generator and no metrics database — a gate whose
results live in a system that needs maintaining is a gate that stops being run, and a schema
nobody can hand-fill reliably is a schema that stops being filled. So there are no per-run
timestamps and no per-run notes, and the ABI is recorded once per **cell** rather than once
per run, which is what lets the three runs of a cell stay three plain strings.

## Whether a red gate blocks a tag

A red gate blocks publication, not signed candidate creation. Tag pushes build
candidates only; manual publication enforces the gate. **Tags bypass branch
protection entirely**, so a check that exists only as a required status context can never
gate a tag.

The workflow reads the block above and requires coverage for the bar that the tag's own
major version implies: `v0.y.z` needs one class complete, `v1.0.0` needs three. The tag is
already parsed for semver, so choosing the bar needs no separate threshold configuration.

Release validation also matches the record's tag, source SHA, version code and APK
SHA-256 against the artifact selected for publication. Publish the exact signed APK
assessed by testers, verifying its checksum immediately before publication. A byte-
different rebuild needs fresh evidence or an explicit waiver; old coverage does not
transfer. A waiver reports insufficient coverage truthfully; it never bypasses an identity
mismatch, invalid record, unavailable artifact, or unreviewed protocol compatibility.
It does not override the immutable published-artifact rule.

The record's source SHA remains the APK's source even when a later commit adds results.
Manual publication runs from protected main with explicit release tag, full reviewed
evidence SHA, candidate run ID and artifact ID. Read this document and its required IDs
at that evidence revision, verify candidate provenance/signature/version/checksum, and
promote its exact bytes. Reruns retain pinned inputs; expiry or mismatches fail without
an automatic rebuild. Coverage and pinned identities appear in release notes; blocked
attempts report in the workflow summary. The full plan is settled in
[How does publication select reviewed gate evidence and the tested APK?](../../.scratch/openflow-v1/issues/63-select-reviewed-gate-evidence.md).

A `gate_waiver_reason` input exists for the case where a build must ship without the
hardware. It must be non-empty, and it is recorded in the release notes — a waiver somebody
had to write a sentence about is worth something; a silent bypass is not. It is wired into
`.github/workflows/release.yml`'s promotion inputs and into `check_gate.py`.

Note what that means for the very first release: the current record has null identity and no
reviewed protocol, and **neither is waivable**, so a waiver alone cannot unblock it. Record
the candidate's identity and the `protocol_commit` you tested under first; after that an
empty `classes` is a coverage gap, which is exactly what a waiver is for.

**This is implemented, and it is currently blocking everything.** The checker is
`.github/scripts/check_gate.py`; the `gate` job in `.github/workflows/release.yml` runs it
before `publish` can start, and the coverage report lands in the workflow summary whether it
passes or fails. The verifier is
`.github/scripts/test_check_gate.py`, run on every pull request, and it asserts the case that
matters most here: **the current record blocks a release**, because `classes` is empty.

The checker reads this document and the candidate's own `app/build.gradle.kts`, at the two
revisions named by the promotion, and nothing else. It refuses an unknown `gate_version`, a
missing or duplicated registry, a class key that disagrees with its own Hostility Profile, a
malformed `runs` list, and any identity that does not match the artifact being published. A
cell on an ABI the build does not ship, and a class whose profile holds any `unknown`, are
**excluded from coverage without being judged** — the third verb, and neither of the other
two.

**What is not yet true: the job has never run against a real tag in either direction.**
Nothing is tagged, so there is no candidate artifact and no `run ID` to promote. The checker
is verified against fixtures and against the real empty record, and the workflow is not
exercised. Until a `v0.y.z` tag exists and one promotion is dispatched, the honest claim is
that the enforcement is written, reviewed and locally verified — not that it has ever gated a
release. Verifying that a job runs is not the same as verifying that it gates, and the second
is the only one that counts.
