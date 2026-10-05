# How a disabled bubble gets switched back on

Type: prototype
Status: claimed
Blocked by: none

Graduated from ticket 34's resolution, which created the gap rather than inheriting it.

## Question

Ticket 34 settled that the Bubble has three states — **disabled**, **idle**, **dictating** —
and that only *dictating* runs a service. What it did not settle is what happens when a user
who has disabled it wants it back.

The problem is that **disabled means absent**. There is no bubble, so there is nothing to tap,
no state to show, and nothing on any screen that says the product still exists. The only
route back is Settings → Bubble, which means launching the app first. That is a
disproportionate amount of ceremony for a control the product's premise is that you use
constantly, and it is the kind of dead end users read as breakage.

Sub-questions:

- **Is the toggle only in settings?** If so, is that acceptable given disabled = invisible,
  or does the disabled state need some other surface — a launcher shortcut, a
  notification, a `shortcuts.xml` pinned tile?
- **What does "disabled" even protect against?** The reason it exists is that a floating
  overlay nobody can switch off is a sharper edge than this product's honesty rule usually
  takes. If the affordance that brings it back is itself noisy, the state has defeated its
  own purpose.
- **Does the bubble warn before it goes away?** Disabling is one tap in settings and takes
  effect immediately. Is that right, or is an overlay that vanishes under the user a small
  betrayal worth a confirm?
- **How does this compose with the permission-health screen**, which ticket 34 gave a
  Bubble-enabled row, and with ticket 47's flow order?

Interact with **Bubble motion & visual details** (`.scratch/openflow-v1/issues/20-bubble-motion-spec.md`),
which owns the idle-state visuals this sits next to — and which is currently `claimed` with no
recorded answer, so that ticket needs triage before the two can be worked coherently.

React to the asset ticket 07 produced at `.scratch/openflow-v1/prototype/bubble.html`. The
design language is settled and not up for renegotiation: brutalist, zero radius, hairline
rules, electric violet, machine voice in mono.

## Answer

Not started.