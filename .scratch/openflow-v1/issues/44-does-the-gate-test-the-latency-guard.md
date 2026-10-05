# Does the acceptance gate test the latency guard?

Type: grilling
Status: resolved
Blocked by: none

## Question

[31 The Latency Guard](31-latency-guard.md) put a Latency Guard in V1: the router
smooths each dictation's Final Latency and narrows a slow provider's verdict to `DEGRADED`
for the next dictation. Ticket 28 settled the gate at **fourteen** scenarios and listed them
by stable id, so adding one is a change to settled work — which is why this is a question
rather than an assumption.

**The case for testing it.** Ticket 28's own reasoning grew the list from ten to fourteen: all
ten originals were about *what you say*, none about what the app does under stress, so four
**mechanism** scenarios were added. The guard is mechanism under stress — arguably the most
stress-shaped mechanism in the product, since it exists precisely because a device might not
keep up. A gate that ignores it leaves the one component whose entire job is a device being
slower than expected with no coverage at all. The gate already claims to test "the router's
degradation and fallback," so a reader reasonably assumes it does.

**The case against, and it is not weak.** A gate that requires a *slow device* cannot be run
by anyone, including a tester on a fast phone. Every other mechanism scenario is reproducible
on hardware you happen to own; this one is not reproducible by construction. And the threshold
itself ships explicitly uncalibrated at 1 s (rule 6 in ADR-0004), so gating on it would gate
on a number the project has already admitted is provisional.

So the question is not simply "add a scenario." It is **which half of the guard is
gateable**, and the likely shape is the split the gate already uses for everything else:

- **The state transition** — a Final Latency above the threshold narrows the verdict to
  `DEGRADED`, and the next `choose()` ranks it last. That is deterministic logic, testable by
  injecting latency, with no slow hardware required. `FakeProvider`
  ([43](43-fakeprovider.md)) is the injection mechanism.
- **The threshold's value** — whether 1 s is the right number on the weakest device class.
  That is a model/benchmark question belonging to
  [41](41-take-the-api-level-benchmark-numbers.md) and
  [30](30-harness-model-benchmark.md), never to the gate.

## What to settle

- Does the guard get a scenario, a mechanism assertion inside an existing scenario, or
  nothing? "Nothing" is a legitimate answer **if** it is written down as a decision with its
  reason — the failure mode to avoid is a silent gap, which reads as thoroughness.
- If it is covered, where does the coverage live: a new numbered scenario (changing
  ticket 28's count of fourteen), or folded into an existing router-degradation scenario?
  Ticket 28 gave scenarios stable ids precisely so the record and the list cannot drift, so
  this has to be an explicit choice rather than an extra bullet.
- What is the injection path, and does it need `FakeProvider` to be scriptable with
  *deliberately slow* events — a capability the ticket does not currently require?
- Does the gate record the guard's verdict in its evidence, alongside the transcript, router
  decision log and state trace? The gate already collects those three and never the audio,
  so a guard verdict is free to add and costs nothing in privacy terms.

## Done when

The gate's coverage of the guard is either explicit or deliberately absent with a recorded
reason, `docs/quality/acceptance-gate.md` and ADR-0008 agree with each other, and any new
requirement on `FakeProvider` is stated as a blocking edge on
[43](43-fakeprovider.md) rather than assumed to exist.

## Answer

Covered, explicitly. **G15 Router degradation and latency guard** is the fifth mechanism
scenario: a mechanism assertion for the guard's *state transition* (a Final over the
provisional 1 s threshold narrows the provider's verdict to `DEGRADED`, and the next
`choose()` ranks it last), folded into the degradation scenario rather than standing alone.
**The threshold's value is never gateable** — whether 1 s is right on the weakest class
stays with the benchmark (30/40/41), and a gate that required slow hardware could not be
run by anyone. Injection is `FakeProvider` scripting a deliberately slow outcome, added as
an explicit requirement on ticket 43 with the blocking edge **44 → 43** recorded there.
The gate's evidence spec now names the guard: the router decision log entry for rule 6 —
the verdict and why — is part of the on-failure artifact, alongside the transcript and
state trace. The scenario count moved fourteen → fifteen (`acceptance-gate.md`,
ADR-0008, `docs/README.md`, the map), stable ids G1–G14 untouched, and ADR-0008 notes the
amendment on its header. Deliberately absent, recorded here so it can't silently widen:
the gate does not test the guard's *smoothing/hysteresis over a rolling window* — that
logic is deterministic and belongs to the unit test suite, not a gate run.