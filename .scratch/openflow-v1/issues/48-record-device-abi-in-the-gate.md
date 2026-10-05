# Record the device ABI in the acceptance gate

Type: task
Status: resolved
Blocked by: none

## Question

Not a question — a small protocol amendment with a specific reason behind it, created by
ADR-0010.

`docs/quality/acceptance-gate.md` defines Device Class behaviourally and records **OEM skin
and Android major version** with each run. It records **no ABI**. That was harmless while
one universal APK carried all four ABIs and every phone could run it.

ADR-0010 ships three — `arm64-v8a`, `armeabi-v7a`, `x86_64` — and drops `x86`. So a run on
a device whose ABI OpenFlow does not ship would now install nothing at all and could not be
mistaken for a pass, but a run recorded without an ABI gives a reader no way to know which
ABI was exercised. **A run on an unshipped ABI is not a run**, and that has to be checkable
from the record rather than inferred later.

Three things this needs, all of them small:

1. **Record the ABI** alongside OEM skin and Android version, in both the human table and
   the fenced `json` Gate Status block — that block is the only thing CI parses, so a field
   that exists only in prose is a field no check can use.
2. **Decide whether an unshipped ABI is rejected or merely recorded.** Recorded is the
   house style — the gate records facts like OEM skin and does not judge them, and Device
   Class is behavioural rather than a brand list. But an out-of-range ABI is not a fact about
   an OEM's behaviour, it is a fact about whether the run means anything, so this one may
   deserve to be checked rather than logged.
3. **Note the emulator's place.** `android-test.yml` runs on an `x86_64` emulator, which is
   why `x86_64` is retained. An emulator run is not a Device Class run and must not be
   recorded as one — worth stating so a future reader does not close the coverage gap with a
   green emulator run.

Blocked on nothing, but [36 Release workflow enforces the gate](36-release-workflow-enforces-the-gate.md)
parses the Gate Status block and so should see this amendment before it enforces anything
against it.

## Answer

Three things were needed and the third turned out to be the largest. The ABI is now recorded
and disqualifying, the emulator's place is written down, and the status block has a shape for
the first time.

**1. The ABI is recorded, per scenario cell.** `docs/quality/acceptance-gate.md` gained
`## What counts as a run`, the setup checklist's device step names it, and `## Device class`
lists it beside OEM skin and Android version. It is read off the device with
`adb shell getprop ro.product.cpu.abilist` — the first entry OpenFlow ships — rather than
typed from memory.

**2. Recorded *and* excluded, which is a third category and not one the ticket offered.** The
ticket's two options were *rejected* and *merely recorded*, and both are wrong. Failing the
scenario is wrong for exactly the reason the gate never fails one for transcription accuracy:
the phone's architecture is not OpenFlow's behaviour, and a gate that fails runs for reasons
outside the pipeline stops being a statement about the pipeline. Merely recording is wrong
because three ABIs ship, a fourth-architected device installs nothing at all
(`INSTALL_FAILED_NO_MATCHING_ABIS`), and a cell recorded from one is absence of evidence
wearing the shape of evidence. So: **the run is excluded from coverage without being
judged**, and the protocol needed a new category to say it. An `abi` outside the shipped list
voids the cell, **and so does an unrecognised one** — `arm64` and `arm64-v8a` are a typo
apart, and a check that accepts what it does not recognise passes exactly where nobody is
looking. ADR-0010's list is the authority, and ticket 36 is told to read it from
`abiFilters` in `app/build.gradle.kts` rather than hardcode a copy that can drift.

**3. The status block had no shape at all, which is the real finding.** `gate_version` was 1
and `"classes": {}` — and no document anywhere specified what a populated entry looks like.
Ticket 28 wrote the block empty and ticket 36 was going to invent the schema while writing
the checker that enforces it. So the shape is now specified: a class carries its recorded
device facts and a map of scenario id to a **required `abi`** and three `pass`/`fail`
results. `gate_version` goes to **2**; a checker must read the version and refuse one it does
not know rather than guess.

The shape is deliberately tiny because **it is filled in by hand, by a tester, from a
checklist** — there is no generator, because ADR-0008 rejected one and the website rejected
one for the same reason. So no per-run timestamps, no per-run notes, and the ABI once per
**cell** rather than once per run, which is what lets a cell's three runs stay three plain
strings. A schema nobody can hand-fill reliably is a schema that stops being filled. Writing
that down is also why the cell, not the run, is the unit: the three runs of a cell are the
same device, which the protocol now says outright.

**4. The emulator, and the reason it needed saying.** `.github/workflows/android-test.yml`
runs the instrumented suite on an **`x86_64` emulator against a debug build**, and that
workflow is the *sole* reason `x86_64` is in ADR-0010's list. So the one shipped ABI with no
real hardware behind it is the one an emulator supplies — and it runs the **`pixel_6`
profile**, so it presents as the class a reader would most want the gap closed with. The
rule now says so in the protocol, in ADR-0008, and in GLOSSARY.md: `x86_64` is retained for
a CI workflow, which is a different claim from a device class tested.

**Three things were found that the ticket did not ask about.**

- **The amendment created a parser ambiguity and the amendment names it.** The section now
  holds two `json` fences — the record and the shape example. A checker that reads the wrong
  one reports on placeholder values and passes. The rule (take the first fence after the
  heading) is stated in the protocol *and* handed to ticket 36, rather than left for whoever
  writes the checker to discover.
- **The scenario count is restated in six live places and nothing checks it.** Ticket 44's
  fourteen → fifteen landed in three of them. `GLOSSARY.md` was still saying "Fourteen
  scenarios per device class" and ADR-0008's Consequences still said the gate grew "to
  fourteen … thirty runs became forty-two" — both **live documents making a false statement
  about the product**, invisible because a document saying fourteen looks exactly like one
  saying fifteen. Both fixed here. The structural question — derive it, check it, or state
  the relationship — is **[57](57-should-the-scenario-count-be-checked.md)**.
- **The record's granularity is undecided and this ticket could not honestly decide it.** The
  prose says every *run* records its own `versionCode` and commit; the block carries one of
  each for the whole record. Those cannot both be true once a record folds together
  crowdsourced runs from several testers on several builds — which is the gate's own
  sanctioned route out of `N/3`. That is **[58](58-what-granularity-does-a-gate-record-have.md)**,
  and it **blocks ticket 36**, because writing a parser before settling it means writing it
  twice.

**One inconsistency found and deliberately not fixed here:**
**[59](59-is-the-keyboards-insets-a-fourth-device-class-question.md)** —
`GLOSSARY.md` says Device Class is decided by **four** behaviours and the protocol says
"Answer three questions". The fourth, the keyboard's own insets, appears in the protocol only
as something G12 *records*. Deciding which document is right is a question about what decides
a device class, which is the destination's core axis, and it does not belong inside a
protocol amendment about the ABI.

**Verified:** `check_docs.py` green (43 files), and both `json` fences parse with
`gate_version: 2`. Nothing was run on a device and no gate number was invented — the shape
example's values are visibly placeholders under an explicit banner, because the gate's own
rule is that it carries no invented numbers.