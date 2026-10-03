# The dictionary, snippets and styles surface

Type: prototype
Status: open
Blocked by: none

> **Graduated from the map's fog.** The fog entry said this "sharpens once ticket 27
> lands". Ticket 27 is resolved and the surface was still unowned, which is precisely the
> stale promise the map's own standing rules forbid. It is a ticket now.

## Question

Ticket 10 put full feature parity in V1, so Dictionary, Snippets and Styles are shipping
surface, not later work. Ticket 12 settled the refiner stage that consumes a dictionary, and
ticket 27 landed the Android build they would live in. What none of them settled is the part
a user actually touches:

- **What does a dictionary entry look like to someone writing one?** A misspelled word mapped
  to a correction is a table row. Is the surface the same row, or is there a per-app
  variation, a pronunciation hint, a "whole phrase" entry distinct from a single word? The
  refiner's dictionary stage has to match against whatever this stores, so the answer is not
  only visual.
- **Are snippets per-app?** This is the sharp one. `RouterContext` and the refiner's app-style
  formatting stage both already assume an app can be identified, and ADR-0003 settles
  re-resolving the target by package plus characteristics. Per-app snippets therefore have a
  plausible mechanism — but "plausible mechanism" is not a decision, and per-app scope
  multiplies the entry count a user maintains.
- **What is the styles surface, and does it overlap the refiner?** Ticket 12 settled style
  formatting as the refiner's last stage, deliberately deterministic. A user-facing style
  picker risks becoming a second, conflicting description of the same stage. Which is it:
  configuration for the existing stage, or a feature the refiner does not have?

## Why a prototype, not a question

Two of these are decided by what they look like rather than by argument — whether an entry is
a word or a phrase, and whether per-app scope feels like maintenance or like a feature. The
existing prototypes this map already produced — the bubble, the onboarding and permission
page — were the right instrument both times, and they established the design language the
app inherits. Reuse it; do not invent a third visual language for a settings surface.

Two constraints any prototype must respect, both settled: ticket 19's permissions page owns
the contextual-permission pattern, and the Notes forbid an onboarding flow that justifies its
order against a generic wizard rather than against contextual requests. Dictionary and snippet
entries are typed text, not permissions — say so explicitly rather than borrowing the page's
rhetoric, which is about what a permission touches and cannot do.