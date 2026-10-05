# 0008: The acceptance gate has two bars

Date: 2026-10-03

Amended 2026-10-03 (`.scratch/openflow-v1/issues/44-does-the-gate-test-the-latency-guard.md`): the scenario count grew fourteen → fifteen with the addition of G15; the two bars and their tag-driven selection are unchanged.

Amended 2026-10-04 (`.scratch/openflow-v1/issues/48-record-device-abi-in-the-gate.md`): the device's **ABI is recorded** with every run, and a run on an ABI the build does not ship **does not count toward coverage**. This amendment introduced `gate_version` **2**, defining the per-class entry for the first time — v1 had only an empty object. The two bars, their tag-driven selection, and the scenario list were unchanged.

Amended 2026-10-05: [Should the scenario count be checked or derived?](../../.scratch/openflow-v1/issues/57-should-the-scenario-count-be-checked.md)
settles relational current prose and a required-ID registry in the status JSON.
This amendment introduced `gate_version` 3; release validation derives completeness from that registry, and
documentation validation compares its IDs with the scenario tables. The two bars are
unchanged. The ticket holds the decision detail.

Amended 2026-10-05: [What granularity does a gate record have?](../../.scratch/openflow-v1/issues/58-what-granularity-does-a-gate-record-have.md)
settles one active signed APK per live record, shared source/version/checksum identity,
official candidates testable before publication, and promotion of the exact tested
bytes. `gate_version` is 4. Historical results do not contribute coverage; evidence
selection is settled by the subsequent version-5 amendment.

Amended 2026-10-05: [How does publication select reviewed gate evidence and the tested APK?](../../.scratch/openflow-v1/issues/63-select-reviewed-gate-evidence.md)
settles candidate-only tag pushes, manual promotion using pinned reviewed evidence and
90-day Actions artifact identities, and coverage-only waivers. Gate Status version 5
adds testing protocol identity and owner-reviewed compatibility. This ticket holds the
selection/lifecycle detail; implementation remains with the enforcement ticket.

Amended 2026-10-05: [How do the three behavioral answers select a Device Class?](../../.scratch/openflow-v1/issues/64-device-class-assignment.md)
settles the answer-to-class rule this ADR had left open. A class is **derived** from a
recorded Hostility Profile by counting hostile answers, so the class key is recomputable
rather than asserted. `gate_version` is 6. The two bars are unchanged.

Amended 2026-10-05: [Make the release workflow enforce the acceptance gate](../../.scratch/openflow-v1/issues/36-release-workflow-enforces-the-gate.md)
**implements the enforcement this ADR decided.** `.github/scripts/check_gate.py` resolves
coverage for one artifact and refuses to publish when it is not met; the `gate` job in
`.github/workflows/release.yml` runs it before the publish job can start, with the bar chosen
by the tag's own major version and a `gate_waiver_reason` that must be non-empty and lands
in the release notes. A tag push now builds a signed **candidate** only; publication is a
manual promotion of the exact tested bytes from protected main. The populated example in
`docs/quality/acceptance-gate.md` was found carrying `gate_version` 5 while this ADR and the
live record said 6, so a tester filling a block by hand from the shape would have handed the
checker a record it refuses; the shape now carries 6. **The gate is now enforced and it is
currently red**: coverage is `N/3` classes, so every release blocks until hardware exists or a
waiver is written down. No tag has been pushed and the promotion path has not been exercised
end to end, which is the remaining gap. The two bars are unchanged.

## Status

Accepted

Amended 2026-10-05: [Is the keyboard's insets behaviour a fourth Device Class question or a recorded observation?](../../.scratch/openflow-v1/issues/59-is-the-keyboards-insets-a-fourth-device-class-question.md)
keeps keyboard behavior in G12 prose evidence, without a fourth class input or schema
change. The existing three behavioral answers still need an assignment rule, owned by
[How do the three behavioral answers select a Device Class?](../../.scratch/openflow-v1/issues/64-device-class-assignment.md).

## Context

The original destination at this ADR's adoption defined "shipped" precisely: three
device classes — Pixel-like, Samsung-class, Xiaomi-class — times ten text-entry
scenarios, thirty scenario/class cells, each going
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

- **Two bars, never conflated.** *Releasable* — a prerelease tag `v0.y.z` — requires every
  required scenario green on at least one device class. *Shipped* — `v1.0.0` — requires every required scenario
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
  background microphones, plus OEM skin, Android version and the device's ABI. Brand and ABI
  are recorded, not decisive — but an ABI the build does not ship **disqualifies the run from
  coverage** rather than failing it. Keyboard behavior is separately recorded in G12
  evidence and does not select the class. **The class is derived, not assigned:** three
  three-valued probe answers form a Hostility Profile, and the class follows from the count
  of hostile answers — `0` → Pixel-like, `1` → Samsung-class, `2–3` → Xiaomi-class. See
  `## Why these`.
- **The three questions are probes, not opinions.** Each answer comes from one observable
  event on the device — survive ten minutes backgrounded with the screen off, the overlay
  permission surviving a reboot, a backgrounded dictation returning a usable transcript —
  so nothing asks a tester to judge how aggressive an OEM is. The third cannot be answered
  from settings at all; it is discovered by running, exactly as the insets behavior is.
- **An incomplete profile is not a class.** Any `unknown` answer means the device is
  excluded from coverage and reported unresolved, and a checker must refuse to derive a
  class from it. Defaulting the unknown to non-hostile would credit an unmeasured device;
  defaulting it to hostile would narrow the gate silently.
- **The profile describes a device in a configuration, not a bare model.** The four settings
  that can move an answer are recorded, so a reader sees why a device reached its tier.
  There is no defaults-only purity rule — that would exclude the handsets most likely to
  volunteer. Changing a probed setting, or the Android major version, requires re-probing,
  and a changed class invalidates that device's earlier cells.
- **The status block's shape is defined, versioned, and hand-fillable.**
  `gate_version` is 6. The block identifies one active signed APK and lists
  `required_scenarios`; a class carries its recorded device facts, its `hostility` answers
  and `config` values, and a map of scenario id to
  a required `abi` and three `pass`/`fail` results. A checker recomputes the class key from
  `hostility` and refuses a record whose key disagrees. An `abi` outside the shipped list voids
  the cell; an unrecognised one voids it too, because a typo is where a tolerant check would
  pass silently. The shape carries no per-run timestamps, because a tester fills it in by
  hand.
- **Completeness is derived from required IDs.** Release validation reads the status JSON
  only; documentation validation checks exact agreement with the protocol's scenario
  table IDs. Current requirements refer to every required scenario rather than repeating
  totals. Historical counts remain explicitly historical.
- **Crowdsourcing is the sanctioned route to closing the coverage gap.** An open-source
  project treats a user on an unbought device class as missing coverage rather than an
  inconvenience.
- **The gate tests the pipeline, not the model.** No scenario is failed for transcription
  accuracy; word error rate belongs to the model-selection work.
- **One canonical document and one active APK record.** The live JSON stores artifact
  identity once for all its runs. Historical sections preserve earlier results but
  never fill gaps in active coverage. Official signed candidates can be tested before
  publication; publishing promotes the exact tested APK after verifying its SHA-256.
  A byte-different rebuild needs fresh evidence or an explicit, public waiver.

The required scenarios cover content on the English default, model delivery and the
bilingual download tier, and mechanism behavior — cancel, interruption, hostile insertion target,
recovery, router degradation — because the original ten were all about what you say and
none about what the app does under stress. The router-degradation scenario covers the
Latency Guard's state transition (a Final above the provisional threshold narrows the
verdict, and the next `choose()` ranks it last); the threshold's value belongs to the
benchmark, never the gate. The full protocol is in
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

**The ABI is recorded and disqualifying, and it is neither of the two things the amendment
could have made it.** Recording it alone was the house-style answer — the gate logs OEM skin
without judging it — but it is not enough, because three ABIs ship and a device on a fourth
installs nothing at all, so a cell recorded from one is an absence of evidence wearing the
shape of evidence. Failing the scenario was the other answer, and it is wrong for the same
reason the gate never fails a scenario for accuracy: the architecture of the phone is not
OpenFlow's behaviour, and a gate that fails runs for reasons outside the pipeline stops
being a statement about the pipeline. What is left is a third thing the protocol did not
have a word for — **the run is excluded from coverage without being judged** — and it needs
a separate category because it is genuinely neither recording nor failing. An unrecognised
ABI disqualifies for the same reason: `arm64` and `arm64-v8a` are a typo apart, and a check
that accepts what it does not recognise is a check that passes exactly where nobody is
looking. The authority is ADR-0010's list, and the authoritative place to read it from is
`abiFilters` in `app/build.gradle.kts`, so the parser compares against the build rather than
against a copy of a list that can drift.

**The emulator is named explicitly, because it is the easiest way to close the gap on
paper.** `x86_64` is retained in the shipped list solely because the instrumented suite runs
on an `x86_64` emulator — no device of that class is claimed. That makes the one shipped ABI
with no real hardware behind it the one an emulator supplies, and the emulator is configured
with a `pixel_6` profile, so it presents as the class a reader would most like to see
covered. A rule nobody has to write down is a rule that gets broken by somebody being
helpful.

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

- **At the 2026-10-03 amendment, the gate had grown from ten scenarios to fifteen.**
  Across three classes, that meant forty-five scenario/class cells and, with three runs
  per cell, 135 attempts for a complete gate. These are historical workload arithmetic,
  not recorded results or a second source of current requirements. It got honest rather
  than small. The planning estimate is roughly 2–3 hours per class, so
  **prereleases are a few times a year, not weekly.** Writing a weekly `v0.1.0-alpha.1` would
  mean skipping the gate, which is exactly what this decision exists to prevent.
  Current workload derives from the required IDs, device classes and runs per cell;
  the historical figures above are not maintained as a live total.
- **There is still no green gate, and there will not be one soon.** The status block is empty,
  which reads as absence of evidence and blocks both bars. That is the intended state.
- **The status block has a shape for the first time, and a version.** It went out with
  `"classes": {}` and no per-class entry specified anywhere, so the checker ticket was going
  to invent one. The shape is in the protocol, and the current version also includes
  the required-scenario registry. The cost of versioning
  a block nothing reads yet is zero, and the cost of *not* versioning it is a checker
  written against a shape that later moves underneath it.
- **A run's ABI is now required, which is a new thing to fill in.** It is one string per
  scenario cell on a form a tester types by hand, and it is read off the device with a single
  `adb` property. That is the whole cost. What it buys is that a class's coverage can no
  longer be claimed by a phone that could not have installed the build.
- **The active record describes one exact APK.** All runs inherit its source/version/
  checksum identity; testers on different devices can test that same APK. Crowdsourcing
  does not require pooling evidence from different builds. The earlier claim that
  parent identity and per-run provenance could not coexist was too strong, corrected by
  [What granularity does a gate record have?](../../.scratch/openflow-v1/issues/58-what-granularity-does-a-gate-record-have.md).
- **Reviewed evidence may arrive after the source commit.** The record's `commit` names
  APK source, while a later repository revision contains the results. A source tag's
  embedded document cannot acquire those results. Selection of the reviewed evidence
  and tested artifact is settled by
  [How does publication select reviewed gate evidence and the tested APK?](../../.scratch/openflow-v1/issues/63-select-reviewed-gate-evidence.md).
- **The enforcement now exists, and the first thing it does is block every release.** The
  checker is `.github/scripts/check_gate.py`, the `gate` job runs it before the publish job
  can start, and the current record has empty `classes`, so `v0.2.0` and `v1.0.0` are both
  refused. That is the decision working, not a defect: a record that looked like a gate and
  passed everything would have been the failure this ADR was written to prevent. The cost
  is that the first `v0.y.z` cannot be published without either hardware or a written
  `gate_waiver_reason`, and the waiver is in the release notes where a user reads it.
- **The gate's own release has not been exercised.** A tag push now produces only a signed
  candidate, and publication is a manual promotion that names the evidence revision, run ID
  and artifact ID. None of that has run, because no tag exists. So the enforcement is
  reviewed and tested against fixtures but has never gated a real release, and this ADR
  should not be read as claiming otherwise. The reworded statement in the protocol is
  deliberate on that point: verifying that a job runs is not verifying that it gates.
- **The rotation variant of the interruption scenario is now specified**, in G12, by
  `docs/adr/0011-bubble-overlay-window.md`. Rotation mid-dictation does not cancel the
  dictation; the bubble re-anchors to the same edge on the new display metrics and
  insertion still lands, because the target was never stored as screen coordinates. The
  keyboard observation is in the same scenario, recorded with keyboard name/version,
  mode, orientation and Anchor. It does not select Device Class. A bubble that rides
  above the keyboard or stays put can pass when reachable and tappable; occlusion or
  an unexpected jump fails. Context and motion are prose evidence, not new JSON fields.
- **Coverage is `N/3` classes and will stay there** until either hardware appears or a
  crowdsourced run lands. Any statement about this project that implies three classes have
  been tested is false until the status block says otherwise. **Deriving the class does not
  manufacture coverage**: no device has been probed, `classes` is empty, and the count rule
  is a way of reading the profile, not a way around having one.

### Why the class is derived rather than assigned

The rule was left open here until the answer-to-class ticket, and its two earlier examples
contradicted each other and this ADR: a Samsung with "never sleeping apps" was said to stay
Samsung-class, which decides by **brand**, while a Xiaomi with aggressive killing disabled
was said to be Pixel-like, which decides by **behaviour**. One of them had to go, and since
this ADR already records that brand is not decisive, the brand-derived example was the
invalid one.

Three alternatives were weighed and rejected:

- **An owner-assigned label.** The owner would pick the class from the answers and a checker
  could not verify the pick. Rejected because comparability across independent testers is
  the property that makes crowdsourcing the sanctioned route out of `N/3`, and only a
  published function guarantees it — a hand-assigned label is unfalsifiable prose, the same
  defect as a check that looks configured.
- **Worst-axis.** The most hostile axis sets the tier. Principled, since OpenFlow must
  survive the worst thing a phone does, but one flaky probe reassigns a device wholesale and
  a phone hostile only on the microphone lands in the top tier alongside one hostile on
  everything.
- **Reference-profile matching.** Classify by closeness to three measured profiles. Rejected
  because it needs measurements on two of three classes the project does not own yet, and
  "closest match" needs its own metric — a rule inside the rule.

The count rule was chosen because it is monotone and robust: one flipped probe moves a
device one step rather than reassigning it. The class names were **kept as they are**, even
though `pixel-like` is honestly spelled and the other two read like brands, because they are
named in the destination and are JSON contract keys — so the suffix carries no meaning and
the glossary says so.

The recorded answers sit in the block because a class key nothing can check is the label
version of an unfalsifiable claim. This costs nothing today: `classes` is empty, so no active
results are invalidated.
