# The Latency Guard: making an unmeasured weakest tier survivable

Type: grilling
Status: resolved
Blocked by: none

## Question

Ticket 25 established that the V1 model default **cannot be confirmed by measurement**
before shipping: the pass threshold is "RTF ≤ 0.3 on the weakest tier", the plan needs three
device classes, and the project has one or two devices. So the default is an assumption, and
the acceptance gate will test it by failing on whatever hardware a user happens to own.

The usual answers are both bad here. Pinning a higher `minSdk` or a narrower support matrix
would shrink the gate quietly, which is exactly what ticket 27 warns against. And shipping a
larger, slower model "to be safe" makes the confirmed case worse and still is not a
guarantee — the slow device is the one nobody benchmarked.

The third answer is to make the assumption **falsifiable at runtime instead of before
shipment.** The app can see, for free, whether decoding is keeping up: it knows audio
duration in and `Final` out, across a dictation it already measures. That turns "we believe
20M int8 is fast enough" into "and if it turns out not to be, here is what happens," which
is a claim that survives contact with an untested device.

This is a design decision with real edges, and it is deliberately takeable *now* — it does
not wait on numbers:

- **Where the measurement lives.** RTF per dictation is a property of a provider run. Does
  it surface as a new `SpeechProvider` capability, as a value on the `SpeechEvent` stream, or
  as something the router derives from timings it already has?
- **What "too slow" means, and who decides.** A threshold constant in the provider, or a
  router rule? A hard RTF threshold and a smoothed one behave very differently on a phone
  that thermally throttles after two minutes of dictation — and thermal throttling is the
  normal case, not an edge case.
- **What the guard is allowed to do.** Ticket 22 settled that `ProviderHealth.DEGRADED`
  ranks *last* rather than sinking, and the map rules automatic health-based rerouting
  mid-dictation out of scope (V1 reads a health snapshot at `PREPARING`). A guard that
  reacts mid-utterance crosses that line, so it has to be pinned down: does it steer the
  *next* dictation, degrade within the current one, or only change what the user is told?
- **Silence versus degradation.** A provider that is running at RTF 1.2 still produces a
  correct transcript, just later. Whether that is `DEGRADED`, a warning, or nothing at all
  is a product call, and the map's brutalist design language has something to say about how
  a limitation is stated.
- **The honest fallback when no faster local option exists.** The V1 fallback chain is
  capability-matched among local providers. If every bundled local model is too slow on that
  device, the guard has nothing to fall back to — it can only tell the truth. Whether V1
  accepts that, or whether a device that slow should be told to download a cloud provider,
  is undecided.

Deliverable: the guard's shape in `docs/adr/0001-speech-provider-contract.md` and its rules
in `docs/adr/0004-adaptive-dictation-router.md`, plus a `GLOSSARY.md` entry if it earns a
term of its own.

Revisit once ticket 25 or 30 lands with real numbers: a guard whose threshold was never
calibrated against a measurement is a guess wearing a control's clothes, which is the same
defect ticket 25 was opened for.

> **Related, already settled — do not re-open it.** While re-scoping ticket 30 it was
> decided that `model-selection.md`'s ship-time bar ("RTF ≤ 0.3 at `num_threads=4` on the
> weakest tier") **stays exactly as written and stays unmeasured**, gated by the acceptance
> gate's existing "aspirational until hardware exists" mechanism (ticket 28) rather than by
> a threshold rewritten to fit one phone. That is the *ship-time* half of this problem and
> it is closed. This ticket owns the *runtime* half, and it is the more interesting one: the
> threshold being unmeasurable before shipment is precisely what makes a runtime guard
> necessary rather than optional. The map's Notes carry the decision.

## Answer

The guard exists, it watches a **duration**, and it costs the contract nothing.

**The term was the first thing wrong.** RTF — inference time ÷ audio duration — is a
benchmark ratio, meaningful when a fixed file is replayed as fast as the machine allows. At
runtime, audio arrives at 1× real time and a user never experiences a ratio; they experience
lag. So the quantity is **Final Latency**: endpoint → `Final`, derived from
`Final.endedAtMs` against the last audio timestamp. RTF stays a benchmark-only number in
`model-selection.md`, and `GLOSSARY.md` now says so explicitly, so the two are never
compared. Three new terms: Final Latency, RTF, Latency Guard.

**It lives in the router, not the contract.** ADR-0001 gains no member. The router computes
latency from timestamps it already receives and *narrows* an existing verdict — which
ADR-0001 already permitted ("may narrow a verdict by exclusion but never upgrades one").
Three rejected placements, each for a specific reason:

| Rejected | Why |
| --- | --- |
| A `SpeechProvider` capability | A capability is a static claim about a model. Slowness is an observation about *this* device over time. Declaring an expected lag would make an undeclared capability indistinguishable from a measured fault. |
| A value on the `SpeechEvent` stream | A live latency signal belongs on a health stream — which is exactly what ADR-0001 declines to add, because a stream "would invite the automatic health-based rerouting ADR-0004 defers." |
| A parallel "slow" verdict beside `DEGRADED` | Creates a second thing that means *rank last*, and the router would then need a rule for which one wins. One job, two mechanisms. |

**Slowness is a cause of `DEGRADED`, not a new value.** The enum stays flat, as ADR-0001
deliberately made it. The cause is recorded in the router's decision log, where "which rule
excluded this provider" already lives and is already the user's explanation.

**It acts only between dictations.** The verdict is written when a dictation ends and read by
`choose()` at the next `PREPARING` — the same snapshot ADR-0004 already reads, written one
dictation late. No mid-dictation model swap, no mid-dictation thread change. That is what
keeps it on the right side of the out-of-scope line against automatic mid-dictation
rerouting, and it is why the guard needed no new event and no new eligibility rule: it is
rule 5's second input, and the order became 5 / 6 / 7.

**Smoothed, not raw.** Thermal throttling is the normal case on a phone, not the edge case.
A hard threshold trips late in every long dictation and never clears — a control that looks
configured and is noise. The question is not *is this device fast* but *is this device fast
right now, for this person*. Hysteresis on both edges so a value on the line cannot flap the
verdict between dictations.

**The threshold ships provisionally at 1 s**, taken from `model-selection.md`'s own stated
budget (Final within ~1 s of endpoint) because it is the project's number, already a
duration, already in the units measured. It is explicitly uncalibrated — no runtime threshold
can be, until measurements exist. Shipping a labelled provisional number beats inventing a
constant, and beats shipping no guard at all, which would leave the unmeasured model default
exactly as un-falsifiable as it is today.

**V1 offers no remedy, and says so once per session.** Every bundled English streaming model
larger than the en-20M default is slower, and the offline-tier models are both larger and a
different mode — so a too-slow device has nothing faster to fall back to, because V1 ships
one real provider. The app states the limitation once, the limit in mono beside what still
works, then stays quiet: a warning on every dictation trains dismissal, and on a genuinely
slow device it would be permanent. Final Latency stays in History per dictation, so the
truth is always one tap away rather than only ever announced once.

**Assets:** the guard's shape in `docs/adr/0001-speech-provider-contract.md` (why the
contract gains nothing), its rules as **rule 6** in
`docs/adr/0004-adaptive-dictation-router.md`, three terms in `GLOSSARY.md`.

**Surfaced, not folded in:** the gate does not yet say whether it covers the guard, and a
gate cannot require a slow device to exist.
[44 Does the acceptance gate test the latency guard?](44-does-the-gate-test-the-latency-guard.md)
carries it.

**Still owed, unchanged:** this guard's threshold stays a guess until real numbers land.
Recalibrate against [41](41-take-the-api-level-benchmark-numbers.md).