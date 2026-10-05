# Is the keyboard's insets behaviour a fourth Device Class question or a recorded observation?

Type: grilling
Status: resolved
Blocked by: none

## Question

At charting, `GLOSSARY.md` defined Device Class as decided by **four** behaviours:

> decided by behaviour rather than brand: how aggressively the OEM kills background apps,
> whether an overlay survives, whether background microphone access is restricted, **and
> whether the keyboard's own insets reach an overlay**.

`docs/quality/acceptance-gate.md`'s `## Device class` section says **Answer three
questions**, and they are the first three. The keyboard is not among them. The insets
behaviour appears exactly once in the protocol, as something a run *records* rather than
asks about — inside G12:

> **Keyboard**: the bubble stays reachable and tappable while the keyboard is up — whether
> it rides above the keyboard or stays put is an OEM fact the run *records* (it feeds Device
> Class), not a pass/fail distinction.

The original documents supported two readings. This question preserves that history;
the resolution below updates the current guidance.

**The glossary is right and the protocol under-asks.** The insets behaviour genuinely
discriminates: ADR-0011 settled that the bubble reads its *own* window insets and never the
accessibility service's, and that "where a skin delivers no insets it stays put" — which is
an OEM behaviour, and is the reason the map records it as a Device Class discriminator at
all. Under this reading the class is under-determined for a tester who has not run G12, and
the gate would let two runs of the same class disagree about what the class is.

**The protocol is right and the glossary overstates.** Insets are *discovered by running*,
not *asked about*, because you cannot know whether a skin delivers usable insets without
putting a bubble over a keyboard on that device. Making it a question would produce a
tester's guess. Under this reading a glossary entry listing a behaviour as class-defining,
when the protocol discovers it as a by-product, is the glossary claiming a determinism the
gate does not have — and the same kind of overstatement as a promise nobody has bounded.

What is actually at stake is the destination's core axis. Device Class is what "shipped"
means, and it is the reason the three classes are Pixel-like, Samsung-class and Xiaomi-class
rather than a brand list. If a class can be decided two different ways by two documents, then
a crowdsourced run and an owner's run stop being the same kind of artifact, which is the
property ADR-0008 says makes crowdsourcing comparable at all.

Grill it towards: does the insets behaviour decide the class, or does it refine it? And if it
refines rather than decides, does `GLOSSARY.md` say so — because a glossary that lists four
deciders for a thing the protocol decides with three is the same document defect as the
scenario count restated in the wrong number of places, one level up and harder to notice.

## Comments

### 2026-10-05 — Facts checked and decision round accepted

The fact check confirmed the glossary/ADR discriminator claim differed from the
three-question protocol, and found no rule mapping those three answers to class labels.
The live JSON has no keyboard-observation field; scenario evidence already belongs
in prose. A stationary Bubble alone cannot establish that usable IME insets were absent.
Android documents window-flag effects on IME layering and context-dependent IME insets:
[WindowManager flags](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_FOCUSABLE),
[WindowInsets](https://developer.android.com/reference/android/view/WindowInsets#getInsetsIgnoringVisibility(int)).

The owner agreed with both recommendations in the live round: record keyboard behavior
as G12 observation without a fourth class input, and capture keyboard name/version,
mode, orientation, Anchor and each run's motion/reachability in prose evidence.
This confirms the shared understanding for this ticket.

## Answer

Resolved through live exchange on 2026-10-05; the owner accepted both recommendations.

### Observation, classification and verdict

Keyboard/insets behavior is contextual **G12 evidence**, not a fourth Device Class
input. The existing classification axes remain background-app survival, overlay/
permission persistence and background-microphone restrictions. Keyboard movement
neither assigns a class nor reassigns an existing one.

Riding above the keyboard and staying put can both pass when the Bubble remains
reachable and tappable. Losing access or an unexpected jump fails G12; it does not
change Device Class. Observed movement is not a diagnosis of OEM behavior or proof
that the window did or did not receive usable IME insets.

### Evidence context

G12 prose records the keyboard name/version, docked or floating mode, orientation,
Bubble Anchor and each run's observed motion and reachability/tappability. Include
context changes during the run; unknown details are recorded as unknown rather than
guessed. All three runs remain recorded under the existing two-of-three verdict rule.

Gate Status keeps its existing G12 pass/fail results. No keyboard JSON field, fourth
classification question, new scenario or schema-version bump is added; the protocol
remains version 4. The own-window insets policy, Anchor behavior and settled limits
on accessibility observation are preserved.

### Handoff and new frontier

GLOSSARY.md, the protocol, ADR-0011 and ADR-0008 now distinguish classification from
keyboard observation. The map indexes this resolution; its earlier discriminator
claim is marked as superseded. This is a planning resolution, with no device results
invented and no production UI or classifier implemented.

The investigation made a separate decision precise: the three existing questions have
no answer-to-class mapping. The former Samsung/Xiaomi settings examples do not supply
one. That mapping is owned by
[How do the three behavioral answers select a Device Class?](64-device-class-assignment.md),
created and wired as a prerequisite for meaningful gate enforcement. This ticket does
not choose that rule or redefine the three-class shipment promise.
