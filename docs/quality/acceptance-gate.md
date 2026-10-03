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
| **Releasable** | a prerelease tag (`v0.y.z`) | all 14 scenarios green on **at least one** device class |
| **Shipped** | `v1.0.0` | all 14 scenarios green on **all three** device classes |

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

The cost of the second bar is a release cadence: a full pass is 14 scenarios × 3 classes,
roughly 2–3 hours per class. **Prereleases are therefore expected a few times a year, not
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

Fourteen, with stable ids so the record and this list cannot drift apart.

### Content, on the bundled English default

| Id | Scenario | Passes when |
| --- | --- | --- |
| G1 | Short chat | One sentence into a messaging app's compose field, text lands at the cursor. |
| G2 | Long paragraph | ≥60 s of continuous dictation in one insertion, no mid-stream stall, no truncation. |
| G3 | Names & jargon | A dictionary entry is applied to the spoken term. It passes on the entry being applied, **not** on the spelling being right. |
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

The original ten were all *content* scenarios — what you say. These four are about what the
app does under stress, which is where the risk in
[`0002`](../adr/0002-dictation-state-machine.md) and
[`0003`](../adr/0003-safe-text-insertion.md) actually lives.

| Id | Scenario | Passes when |
| --- | --- | --- |
| G11 | Cancel mid-dictation | Nothing is inserted, the recording is deleted, and the app returns to idle. |
| G12 | Interruption mid-dictation | Focus loss, screen off, or an inbound interruption mid-session is survived; the bubble is not stranded and the state reaches a terminal outcome. **The rotation variant is pending** `.scratch/openflow-v1/issues/34-bubble-overlay-window.md` and is not specified here. |
| G13 | Hostile insertion target | A sensitive field refuses **before recording starts**; a field that rejects the paste leaves the clipboard as it was found. |
| G14 | Recovery re-insert | After a recoverable failure the entry resurfaces in History and re-inserts, or is cleanly abandonable. This is where "recover cleanly" is exercised. |

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
3. Note the device: class answers below, plus OEM skin, Android major version, and model.
4. Grant permissions as a first-time user would, through the contextual flow, not by
   pre-granting in `adb shell pm grant`.
5. Run each scenario, and record each run.

Evidence worth capturing, on failure at minimum: `adb logcat -d` around the failure, and
`adb exec-out screencap -p` of the target app.

## Device class

Class is decided by behaviour, because that is what differs between an overlay and a
background microphone. Brand is recorded but is not decisive.

Answer three questions:

1. Does your OEM kill background apps aggressively, even for a foreground-service mic?
2. Can you draw over other apps, and does the permission survive a restart?
3. Is background microphone access restricted or revocable?

Record the OEM skin (`HyperOS`/`MIUI`, `One UI`, stock/AOSP) and the Android major version.
A Samsung with "never sleeping apps" enabled is still testing what a Samsung is, and that is
the point of the class. A Xiaomi-branded phone with aggressive killing turned off may well be
recorded as Pixel-like — record what you observed.

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

Every run records its **build identity**: `versionCode` and the commit SHA. A run without it
cannot be reproduced or attributed. External testers run a **released APK only** — that is
what makes a crowdsourced run re-runnable by anyone.

On failure, the artifact is the **on-screen transcript, the router decision log, and the
state trace**. ADR-0004 records a decision on every dictation and ADR-0002 records the
state, so both already exist and the tester only has to paste them.

**Never the audio.** OpenFlow deletes recordings after processing and this project has no
telemetry; a gate that retained raw recordings would be a second retention path in the one
place nobody was looking. And because this gate is public — runs are committed, and
crowdsourced runs are filed in public issues — the checklist tells testers in their hands to
dictate **text they wrote themselves**. A real message pasted into a public issue would
contradict the privacy policy with nothing to stop it.

## Where runs live

One canonical record: this document. Append a run per pass, in a PR, with a table row per
scenario and the detail blocks below it. GitHub issues are the **intake** channel for
crowdsourced runs; the owner folds each into this document in the same PR. No generator and
no metrics database — a gate whose results live in a system that needs maintaining is a
gate that stops being run.

## Gate status

The block below is the machine-readable half of this document, and it is the **only** thing
CI parses. It is written for a parser and embedded in the human record on purpose: one file,
so a coverage number and its prose cannot drift apart, and a reworded paragraph cannot
change the gate. The workflow reads it; it never reads the tables above.

```json
{
  "gate_version": 1,
  "as_of": null,
  "commit": null,
  "version_code": null,
  "classes": {}
}
```

`as_of`, `commit` and `version_code` are `null` and `classes` is empty because no run has
been performed. An empty `classes` is not a pass; it is an absence of evidence, and the
enforcement in ADR-0008 treats it as a block for both bars.

## Whether a red gate blocks a tag

Yes, inside the release workflow — not in branch protection. **Tags bypass branch
protection entirely**, so a check that exists only as a required status context can never
gate a tag.

The workflow reads the block above and requires coverage for the bar that the tag's own
major version implies: `v0.y.z` needs one class complete, `v1.0.0` needs three. The tag is
already parsed for semver, so this needs no new configuration and no new input.

A `gate_waiver_reason` input exists for the case where a build must ship without the
hardware. It must be non-empty, and it is recorded in the release notes — a waiver somebody
had to write a sentence about is worth something; a silent bypass is not. It is not wired
up yet: see
`.scratch/openflow-v1/issues/36-release-workflow-enforces-the-gate.md`.

That job is only trustworthy once it has been **exercised end to end on a real tag, in both
directions** — a tag that blocks, and a tag that is waived. Verifying that the job runs is
not the same as verifying that it gates.