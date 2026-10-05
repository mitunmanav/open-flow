# How do the three behavioral answers select a Device Class?

Type: grilling
Status: resolved
Blocked by: 59

## Question

What repeatable rule maps the protocol's three behavioral observations to Pixel-like,
Samsung-class or Xiaomi-class, so independent testers assign comparable Device Classes?

The existing protocol asks about background-app killing, overlay/permission persistence,
and background-microphone restrictions, but supplies no answer-to-class rule. Its former
examples said a Samsung with “never sleeping apps” stays Samsung-class while a Xiaomi
with aggressive killing disabled may be Pixel-like; those examples do not establish
a consistent behavioral classifier.

[Is the keyboard's insets behaviour a fourth Device Class question or a recorded observation?](59-is-the-keyboards-insets-a-fourth-device-class-question.md)
settles keyboard behavior as contextual G12 evidence, not a fourth classification
input. Keep that boundary, the three named classes and the existing release bars.

Settle:

- The repeatable observations or setup procedure that answer each existing question,
  distinguishing measured behavior from a tester's guess or a brand assumption.
- How answer combinations map to the named classes, including mixed behaviors,
  unknown answers and OEM settings that change the observed behavior.
- The evidence needed for an owner-reviewed class assignment, and how unsupported
  or ambiguous classifications are reported without inventing coverage.
- Whether changing the tested device configuration requires a fresh assignment,
  and how the recorded context makes crowdsourced evidence comparable.

Do not silently redefine the three-class shipment promise around the devices on hand.
ABI eligibility, artifact identity, G12 keyboard verdicts and the two release bars
are already settled. Record the assignment rule in the protocol and update its domain
definition as needed; enforcement implementation belongs to
[Make the release workflow enforce the acceptance gate](36-release-workflow-enforces-the-gate.md).
Consult GLOSSARY.md, grilling and domain-modeling.

## Comments

### 2026-10-05 — Facts checked, then three decision rounds accepted

The repository was checked before the first question, and the finding shaped it. The
protocol's own text already conceded the gap — *"This protocol does not yet provide a
repeatable class assignment"* — and the two former examples **contradicted each other and
the rule stated above them**: "a Samsung with never-sleeping apps stays Samsung-class"
decides by **brand**, while "a Xiaomi with aggressive killing disabled may be Pixel-like"
decides by **behaviour**. Since the section opens "Brand is recorded but is not decisive",
one example had to be the invalid one.

Two further facts shaped the rounds. The protocol had no way to carry *why* a class key said
what it said — the entry held only `oem_skin`, `android_major`, `device_model` and
`scenarios` — so a class was a label with nothing behind it. And the counts line up with
intuition without ever consulting a brand: a stock Pixel is hostile on no axis, a One UI
phone on one or two, a HyperOS phone on all three, which is what made a severity reading
look natural before it was offered as an option.

The owner accepted all recommendations in three rounds: **derived rather than assigned**,
**three probes** at a labelled-provisional 10-minute interval, **configuration recorded with
no purity rule**; then **count rather than worst-axis**, **an incomplete profile yields no
class**, **the answers enter the JSON at `gate_version` 6**, and **re-probe on any probed
setting or Android major change**; then **keep the asymmetric class names** and **amend
ADR-0008 rather than adding one**. This confirms the shared understanding for this ticket.

## Answer

Resolved through live exchange on 2026-10-05, in three rounds. The owner accepted every
recommendation, so this is a decision the owner made rather than one the agent supplied.

### The class is derived, not assigned

The protocol's three questions are now **three probes**, and the class is a published
function of their answers rather than a label someone picks. Count the hostile answers:
`0` → `pixel-like`, `1` → `samsung-class`, `2–3` → `xiaomi-class`.

The class key is therefore **recomputable**, which is what makes independent testers agree
without conferring. Comparability across testers is the property that makes crowdsourcing
the sanctioned route out of `N/3`, so it had to be a published function rather than an
owner's judgement.

### The probes, and why they are probes

Each answer is one observable event on the device, never a judgement about an OEM:

1. **Background survival** — start a dictation, background OpenFlow, screen off, wait
   **10 minutes**, return and complete a dictation without restarting OpenFlow.
2. **Overlay persistence** — grant "display over other apps" through the app's own flow,
   **reboot**, check the permission is still granted and the bubble still draws.
3. **Background microphone** — a backgrounded dictation returns no usable transcript while
   the same dictation works in the foreground.

Answers are **three-valued** (`yes`/`no`/`unknown`); an inconclusive probe is retried before
being recorded `unknown`. The 10-minute interval is a **provisional threshold, labelled as
such** — the map's standing precedent from the latency guard, and preferable to an invented
constant or to leaving the axis unmeasured.

Axis 3 **cannot be answered by reading settings**; it is discovered by running, exactly the
insets precedent ticket 59 settled. That is why all three became probes rather than keeping
one as a question.

### An incomplete profile is not a class

Any `unknown` means the device gets **no class at all** — excluded from coverage and reported
unresolved. Not a fourth tier, not a defaulted answer. Both defaults are wrong in opposite
directions: `unknown` as non-hostile credits a device nobody measured, `unknown` as hostile
narrows the gate silently. A checker must **refuse to derive** rather than guess — the same
rule it already follows for an unknown `gate_version` or an unrecognised `abi`. This is
ticket 48's third verb again: **excluded from coverage without being judged.**

### Configuration is part of the identity

The profile describes **a device in a configuration, not a bare model** — the map's
"store the invariant" move, the same reasoning that made the Bubble an `Anchor` rather than
a coordinate. Four settings are recorded because they can move an answer: battery-optimisation
exemption, autostart, per-app microphone permission, unrestricted battery mode.

**No defaults-only purity rule.** A tuned Xiaomi honestly lands in a lower tier with the
values that put it there visible, which is the point of recording them. A rule excluding
tuned devices would exclude the handsets most likely to volunteer a run.

**Re-probe on any change to those four settings, or any Android major-version change; a
changed class invalidates that device's earlier cells.** Results bind to the conditions they
were produced under, as they already do for APK identity and protocol revision.

### Schema

`gate_version` **5 → 6**. A class entry gains `hostility` (three three-valued answers) and
`config` (the four settings). A checker recomputes the class key from `hostility` and refuses
a record whose key disagrees. Nothing is lost today: `classes` is empty, so no active results
are invalidated. The entry stays hand-fillable — three booleans and four settings, on ticket
48's standard.

### Rejected alternatives

- **An owner-assigned label** — unfalsifiable prose, and the defect this map keeps meeting:
  a check that looks configured. Only a published function guarantees comparability.
- **Worst-axis** — principled, but one flaky probe reassigns a device wholesale, and a phone
  hostile only on the mic lands in the top tier alongside one hostile on everything.
- **Reference-profile matching** — needs measurements on two of three classes the project
  does not own, and "closest match" needs its own metric: a rule inside the rule.

### What was found, and one naming honesty

The two former examples **contradicted each other and the rule above them**: "a Samsung with
never-sleeping stays Samsung-class" decides by brand, "a Xiaomi with killing disabled may be
Pixel-like" by behaviour. Since the section already said brand is not decisive, the brand
example was the invalid one. The examples are now gone.

The three class keys keep their existing spellings even though `pixel-like` is honestly
spelled and the other two read like brands — they are named in the destination and are JSON
contract keys. So **the suffix carries no meaning**, and the glossary now says so. Renaming
would rewrite the destination's own words and a machine-read contract to fix a cosmetic
inconsistency.

### Where it landed

Protocol (`docs/quality/acceptance-gate.md`, `gate_version` 6), `GLOSSARY.md` — `Device
Class` amended and `Hostility Profile` added — and ADR-0008 amended rather than a new ADR,
because the question was already opened there and two documents half-claiming one decision
is its own defect. **Implementing the checker stays with
[Make the release workflow enforce the acceptance gate](36-release-workflow-enforces-the-gate.md)**,
which now has the rule and its three refusals.

No device has been probed, `classes` remains empty, and coverage remains `N/3`. Deriving the
class does not manufacture coverage: the rule reads a profile, it does not substitute for
having one.
