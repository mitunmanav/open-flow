# Take the API-level benchmark numbers

Type: task
Status: blocked:human
Blocked by: 40

> **Parked on a human with hardware.** No agent session can finish this one — it is
> measured by running the harness on a phone. Parked rather than claimed so it does not sit
> on the frontier pretending to be takeable.

## Question

Run the harness from [40](40-build-the-sherpa-benchmark-harness.md) on the devices that
exist, and record what it reports in `docs/providers/model-benchmark-results.md` as the
**second** of three sections.

## What it produces

The matrix and protocol come from `model-selection.md`; this ticket only executes them.
Results go in `docs/providers/model-benchmark-results.md`, which
[25](25-run-model-benchmark.md) creates — so 25 is the practical prerequisite for the file
itself even though both are parked on the same hardware.

- **Section 1 — demo APKs.** Ticket 25's directional numbers. Written first, because it
  needs no code and only a phone.
- **Section 2 — this ticket.** The API-level instrument: RTF, WER, first-partial latency,
  endpoint→Final including VAD segmentation cost, peak RSS, model load time, the
  `num_threads ∈ {2, 4}` sweep.
- **Section 3 — through the provider.**
  [30](30-harness-model-benchmark.md), blocked on
  [42](42-implement-the-sherpa-speech-provider.md).

Each section names the instrument that produced it. Three instruments reporting on the same
models is the point, not a redundancy to be tidied away: the gap between section 2 and
section 3 **is** the cost of OpenFlow's integration, and collapsing them would delete the
only measurement of that cost the project has.

## What it settles

- **Whether the recommended V1 default survives contact with a real decode** —
  `sherpa-onnx-streaming-zipformer-en-20M-2023-02-17` int8, the model already named as the
  bundled default in `model-selection.md` and in the map's Notes. Ticket 06 recorded that
  default as a *hypothesis to be confirmed by benchmarking*; this is that confirmation, or
  its refutation.
- **The `num_threads ∈ {2, 4}` sweep**, which decides whether the 4-thread assumption in
  `model-selection.md`'s requirements recap holds on the hardware in hand.
- **Which number is directional.** Section 2 is explicitly *not* the shipping number, and
  the document must say so in its own words rather than in a footnote.

## What it cannot settle, and must not pretend to

- **The pass threshold.** RTF ≤ 0.3 at `num_threads=4` on the weakest tier stays unmeasured:
  there is no weakest tier, because the project has one or two devices. Report RTF, report
  whether the weakest *available* device met it, and name that device. Do not restate the
  bar in a way that one phone can pass.
- **The device matrix as a gate.** Coverage stays `N/3` classes. A green section here is not
  a gate result, and the acceptance gate's coverage block stays empty until the gate itself
  has run.
- **Integration cost.** That is section 3, and it cannot exist until 42 does.

## Done when

- Every model in the matrix that can be run on the hardware available has been run, with
  the protocol's discipline (warmups discarded, ≥20 iterations, median, thermal state noted)
  — and any model that could **not** be run is listed as not run, with the reason. A matrix
  with silent gaps is worse than a short one, because a reader cannot tell which is which.
- `model-selection.md`'s *Recommended V1 defaults* section is revised if the numbers move
  it, which is what that section's own closing line promises.
- Section 2 exists in the results document, labelled with its instrument.