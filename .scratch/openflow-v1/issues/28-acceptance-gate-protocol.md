# How the acceptance gate is run and recorded

Type: grilling
Status: resolved
Blocked by: none

## Question

> **Hardware reality, established in ticket 25: the project has one or two real devices, not
> three.** So the gate as the destination defines it — three device classes × ten scenarios,
> 30 runs — cannot be run as written, and no protocol will fix that. This ticket must
> therefore decide what the gate *is*, not only how it is recorded: either narrow the device
> promise honestly and say so, or keep three classes and accept that the gate is
> aspirational until the hardware exists. Either is defensible; quietly redefining "shipped"
> as "shipped on the devices we happened to have" is not. The device bullet below is no longer
> an open question — it has an answer, and it is the difficult one.

The destination defines "shipped" precisely: three device classes (Pixel-like, Samsung-class, Xiaomi-class) × ten text-entry scenarios, each going start → record → transcribe → clean → insert → recover cleanly. That is 30 runs, and it is the single thing standing between this project and its goal.

What is undecided is the *protocol*, and getting it wrong makes the gate worthless in a specific way — a gate that is tedious enough to be skipped is not a gate.

- **Scripted harness or manual checklist?** The scenarios are dictation runs into third-party apps (Gmail, WhatsApp, Slack, a notes app). You cannot script the *content*, because the point is inserting into apps you do not control. So the honest options are a scripted setup with a manual pass/fail, or a fully manual matrix. What does the project actually want to maintain?
- **How is a run recorded?** Per-device, per-scenario pass/fail with a short note is enough to be useful. The temptation is a full metrics database, which is a system to maintain and defend against rot.
- **What counts as "recover cleanly"?** The destination says it, and it is the least objective phrase in it. Recoverable errors resurface in History per ticket 14 — so a recovery test is exercising a real path, not an edge case, and deserves its own definition.
- **Whose devices?** This is the practical blocker, and it is now **answered**: the owner's own
  one or two devices. That settles the budget and it settles the repeatability downward — this
  cannot be CI-hosted real-device work (Firebase Test Lab) without a spend decision nobody has
  made, so it stays anecdotal-by-construction on owner hardware. What it does *not* settle is
  what the project claims when the gate covers one device class instead of three, which is the
  live question this ticket now has to face.
- **Does a failure block the release tag?** A gate that reports without gating is a report. `release.yml` currently triggers on `v*` and runs unit tests only — decide whether it must also consult the recorded gate result before publishing.

Decide the protocol, not the results. The results do not exist yet, and this ticket is what makes producing them a repeatable act rather than a heroic weekend.

## Answer

The protocol is `docs/quality/acceptance-gate.md`; the reasoning is
`docs/adr/0008-acceptance-gate-two-bars.md`. This is the index of that answer, not a
second copy of it.

### What the gate now is

**Two bars, never conflated.** *Releasable* — a prerelease tag `v0.y.z` — needs all 14
scenarios green on at least one device class. *Shipped* — `v1.0.0` — needs all 14 on all
three. The tag's own major version says which claim is being made. This is the answer to
the ticket's hard question: neither of the two options the ticket offered. Narrowing the
gate would have rewritten the destination's promise; keeping one bar would have meant no
release ever ships. Coverage is reported in a published status block at all times, so the
gap between promise and evidence is public rather than private.

**Crowdsourcing is the sanctioned way to close it.** This is an open-source project; a user
on a device class the project cannot afford to buy is the missing coverage, not an
inconvenience. That is the only realistic route from `N/3` to `3/3`.

**Device class is behavioural**, not a brand list — three questions about OEM hostility to
overlays and background microphones, plus OEM skin and Android major version. The three
classes are on the list precisely because they differ there. A Samsung with "never sleeping
apps" is still testing what a Samsung is.

**Pass means the pipeline behaved** — ADR-0002's state machine, ADR-0003's insertion, the
router's degradation, the recovery paths. **No scenario is ever failed for transcription
accuracy**; that measurement already has a home in the model-selection work, and folding it
in would make the gate slow, subjective, and a duplicate.

### What changed about the scenarios

Fourteen, and the gate grew rather than shrank. Ten scenarios cannot all have been
discovering anything about this product:

- **Two tested the network, not the app.** OpenFlow transcribes on-device, so "weak
  connection" and "no connection" were unfalsifiable as written. Their real content was the
  model store, so they became download-in-the-clear and download-on-a-bad-network. A clear
  failure is the pass; silence is the failure.
- **One was unpassable on the shipped default.** "Multiple languages" is English-only until
  the zh+en download tier is installed, so it is now honestly labelled a download-tier
  scenario and may never be cited as evidence about the default.
- **None of them was a mechanism scenario.** All ten were about what you say. Four were added
  — cancel, interruption, hostile insertion target, recovery re-insert — which is where the
  risk in ADR-0002 and ADR-0003 actually lives.

**"Recover cleanly" is split in two**, which is what the ticket flagged as its least
objective phrase: a documented procedure for *injecting* a failure, and a checklist of
*post-conditions* to check afterwards. Without the split it is the runner's mood on the day.

### How a run is done

The verdict is always a human's — the scenarios dictate into apps this project does not
control, and no tooling decides whether Gmail's compose field got the right text. `adb`
installs, clears state, fixes the content, and captures evidence; it never renders the
verdict. One checklist serves the owner and an external tester, so a crowdsourced run and an
owner run are the same kind of artifact.

**Three runs per cell, pass is two of three, all three recorded** — the same discipline the
model benchmark already uses, so the project holds one rule for measuring things. Flake stays
visible as a property; unlimited retries are not offered as a substitute.

Every run carries its build identity (`version_code` and commit SHA), and external testers
run released APKs only, which is what makes a crowdsourced run re-runnable by anyone.

### The privacy position, which was not in the ticket

A failure's artifact is the transcript, the router decision log and the state trace — both of
which ADR-0004 and ADR-0002 already produce. **Never the audio:** this project deletes
recordings after processing and has no telemetry, and a gate retaining raw recordings would be
a second retention path in the one place nobody was looking. And because runs are committed
and crowdsourced runs are filed in public issues, the checklist tells testers in their hands
to dictate text they wrote themselves. The gate is the project's only manual collection
process, so it has to obey the same rules as the app.

### Where it lives, and whether it gates

One canonical record — the protocol document itself, appended per run, with GitHub issues as
the intake channel for crowdsourced runs. No generator and no metrics database: a gate whose
results live in a system that needs maintaining is a gate that stops being run.

The machine-readable half is a fenced `json` block under `## Gate status`, **embedded in the
human document on purpose.** One file means coverage and its prose cannot drift, and it means
a reworded paragraph cannot move the gate. It is empty now, which is absence of evidence, not
a pass.

**A red gate blocks a tag, inside `release.yml`, before publishing** — not in branch
protection, because tags bypass branch protection entirely and a required context there would
be decorative. `gate_waiver_reason` must be non-empty to bypass and lands in the release
notes.

### Not in this ticket's scope, deliberately

The enforcement is **not implemented here**. It is
[36 Make the release workflow enforce the acceptance gate](36-release-workflow-enforces-the-gate.md),
blocked on 33 and 35, because it cannot be exercised on a real tag until a signed build
exists and it must be verified blocking *and* waived before anyone trusts it. Writing that
code here, against two other open tickets that edit the same file, would have been the
wrong trade.

Three things were inferred rather than asked and are cheap to reverse: the ADR (the two-bar
decision clears all three tests; the scenario list was deliberately kept out of it), the
protocol document living at `docs/quality/acceptance-gate.md`, and the enforcement becoming a
ticket rather than a diff.

### What this ticket changed about the destination

The gate sentence is amended: fourteen scenarios rather than ten, two bars rather than one,
and the "shipped" claim still requires all three device classes. Nothing was quietly dropped —
the two network framings and one language framing were re-homed, and the destination says so.

**No run has been performed. The gate has never been green. Coverage is `N/3` classes and
stays there until hardware or a crowdsourced run arrives.**