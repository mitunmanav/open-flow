# 0008: The acceptance gate has two bars

Date: 2026-10-03

## Status

Accepted

## Context

The destination defines "shipped" precisely: three device classes — Pixel-like,
Samsung-class, Xiaomi-class — times ten text-entry scenarios, thirty runs, each going
start → record → transcribe → clean → insert → recover cleanly. It is the single thing
standing between this project and its goal.

It cannot be run. The project has one or two real devices, established while measuring the
model candidates: there is no third device class to run on, and no protocol fixes that. The
same gap already made `docs/providers/model-selection.md`'s threshold — "RTF ≤ 0.3 on the
weakest tier" —
unmeasurable, because there is no weakest tier to measure against.

That leaves three shapes, and the tempting one is the dishonest one. Narrow the gate to the
hardware in hand and the project ships cleanly, but "shipped" now means "shipped on the
devices we happened to have", which is a promise nobody can discover until their phone
misbehaves. Keep three classes and no release ever ships, because every tag would be blocked
on hardware that does not exist.

Two further facts shaped the answer. **Tags bypass branch protection**, so the `main` gate
that ticket 18 built — a PR requirement with three required contexts — cannot gate a tag at
all; any enforcement has to live inside `.github/workflows/release.yml`. And the ten
scenarios were written for a product that does not exist: OpenFlow transcribes on-device, so
"weak connection" and "no connection" test the network rather than the app, and "multiple
languages" is unpassable on the bundled default, which is English-only.

## Decision

- **Two bars, never conflated.** *Releasable* — a prerelease tag `v0.y.z` — requires all 14
  scenarios green on at least one device class. *Shipped* — `v1.0.0` — requires all 14
  green on all three. The tag states which claim is being made.
- **Coverage is reported, always.** The gate status is published whether or not it is green,
  so the distance between the promise and the evidence is public.
- **The bar is selected by the tag's own major version.** The workflow already parses semver;
  no new input and no second configuration choose the threshold.
- **Enforcement lives inside `.github/workflows/release.yml`, before publishing**, because
  branch protection
  never sees a tag. A `gate_waiver_reason` input must be non-empty to bypass it and is
  recorded in the release notes.
- **Device class is behavioural**, from three questions about OEM hostility to overlays and
  background microphones, plus OEM skin and Android version. Brand is recorded, not decisive.
- **Crowdsourcing is the sanctioned route to closing the coverage gap.** An open-source
  project treats a user on an unbought device class as missing coverage rather than an
  inconvenience.
- **The gate tests the pipeline, not the model.** No scenario is failed for transcription
  accuracy; word error rate belongs to the model-selection work.
- **One canonical record**, with a machine-readable status block embedded in it so coverage
  and prose cannot drift, and so a reworded table cannot change the gate.

The scenarios are fourteen: seven content scenarios on the bundled default, three re-homed
from the network and language scenarios onto the model store and the bilingual download
tier, and four mechanism scenarios — cancel, interruption, hostile insertion target,
recovery — because the original ten were all about what you say and none about what the app
does under stress. The full protocol is in
[`docs/quality/acceptance-gate.md`](../quality/acceptance-gate.md).

## Why these

**Two bars rather than one, narrowed.** A gate you cannot run is not a strict gate, it is an
absent one. Narrowing it would also have silently rewritten the destination, which is the one
thing this project cannot afford: its entire value is that a claim is set against what it
cannot do. Two bars keeps the promise intact and makes the shortfall legible instead of
erasing it.

**The tag selects the bar.** The alternative was a manually chosen threshold, which is a
setting someone eventually sets wrong, or a `require_classes` input, which is a second thing
to keep in sync with the destination. A prerelease tag is a claim about maturity in the
version number itself, so deriving the bar from it cannot disagree with the version.

**Enforcement in the workflow rather than branch protection.** Branch protection is the
obvious place to put a gate and it would have looked correct: required contexts, a red
check, everybody satisfied. It gates nothing, because a tag is not a branch and the
protection rules never see it. A required check that cannot fail is worse than no check,
because it looks configured.

**A waiver instead of a hard block.** In an open-source repository somebody will eventually
need to rebuild a tag on hardware nobody has. Hard-blocking that is a problem to be solved by
deleting the workflow; a waiver forces somebody to write down why, and lands the reason in
the release notes where a user can read it.

**Behavioural classes rather than brands.** The three classes exist because of how differently
OEMs treat a floating overlay and a background microphone — that is the whole reason
Samsung-class and Xiaomi-class are on the list while "a big phone" is not. Defining the class
by that behaviour is also what makes a crowdsourced run comparable to an owner's.

**Pipeline correctness rather than accuracy.** Accuracy needs a reference transcript, a
scoring tool, and tolerance for noise, and it duplicates a measurement that already has a home
in `docs/providers/model-selection.md`. A gate slow enough to be skipped is a gate with no
results, and no results is the failure mode this whole ticket exists to prevent.

**One record, with the machine-readable part inside it.** The alternative was a canonical
markdown table plus a separate JSON summary, which is two files that can disagree, or a
generator, which this project rejected for the website in ticket 26 for the same reason.
Embedding a fenced block that only a parser reads keeps one source of truth and makes a
prose edit inert.

## Consequences

- **The gate grew from ten scenarios to fourteen, and thirty runs became forty-two.** It got
  honest rather than small. The cost is real: a full pass is roughly 2–3 hours per class, so
  **prereleases are a few times a year, not weekly.** Writing a weekly `v0.1.0-alpha.1` would
  mean skipping the gate, which is exactly what this decision exists to prevent.
- **There is still no green gate, and there will not be one soon.** The status block is empty,
  which reads as absence of evidence and blocks both bars. That is the intended state.
- **The enforcement is not wired up yet.** It is
  `.scratch/openflow-v1/issues/36-release-workflow-enforces-the-gate.md`, blocked on a signed
  build, because it cannot be exercised on a real tag before one exists. Until then the gate
  is a record with no enforcement, and it should not be described as blocking anything.
- **The rotation variant of the interruption scenario is unspecified**, owned by
  `.scratch/openflow-v1/issues/34-bubble-overlay-window.md`. It is recorded as pending rather
  than invented here, because the answer belongs to that ticket's decision.
- **Coverage is `N/3` classes and will stay there** until either hardware appears or a
  crowdsourced run lands. Any statement about this project that implies three classes have
  been tested is false until the status block says otherwise.