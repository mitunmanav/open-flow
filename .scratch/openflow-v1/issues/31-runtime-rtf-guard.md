# The runtime RTF guard: making an unmeasured weakest tier survivable

Type: grilling
Status: open
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