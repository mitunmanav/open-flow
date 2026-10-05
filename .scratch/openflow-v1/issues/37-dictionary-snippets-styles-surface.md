# The dictionary, snippets and styles surface

Type: prototype
Status: resolved
Blocked by: none
Parked on: — resolved through live exchange; see ## Answer. Three questions were deliberately
  left open and now have their own tickets rather than being folded in here.

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

## Prototype

`.scratch/openflow-v1/prototype/dictionary-snippets-styles.html` — single file, open it directly.

**It started as three variants and is now one surface, by the owner's call.** That is worth
recording, because the merge is the decision and it settled two of the three questions by
elimination rather than by argument:

| was | is now |
|---|---|
| **A** three lists, kind-first + **B** scope-first, two surfaces to choose between | **one surface**: three category tabs, and per-app scope moved onto the entry itself |
| **C** the live pipeline bench | **gone** — it was a decision aid, not a settings screen |

What the merge settled, without either side having to win an argument:

- **Per-app is in, and it is per-*package*, not per-category.** Scope is a property of an entry:
  one row reads `sig → Best regards,` with an app chip under it, and tapping the chip opens a list of
  the apps on the phone. That makes finding 2 below **moot** — naming a specific app the user
  recognises is a different act from naming a category, so V1 needs no taxonomy at all.
- **Styles is configuration for stage 8**, not a second feature. The Styles tab shows the three
  presets and then only the apps that have been set differently from the default.

The generalisable move: **a per-item scope beats a per-axis scope.** B's separate scope rail made
the user hold two hierarchies at once and then arithmetic about which entries applied where; putting
the app on the entry itself makes the common case (one tap, done) and the rare case (three taps)
both obvious.

### What the four skills changed

`mobile-android-design` governs this surface because it is an Android surface. Taken from M3: the
48dp floor, ListItem-shaped rows well past it, a `SingleChoiceSegmentedButtonRow` for the three
categories, hoisted state with every render derived from it, a WindowSizeClass-style breakpoint, a
back affordance, `contentDescription` on every control, and a scrollable list rather than a `Column`.

**Not** taken from M3, because the map has already decided it and a prototype that quietly reopens a
settled decision is worse than one that ignores the skill: **dynamic colour is off.** M3 best practice
says support it; this app's accent is a fixed electric violet, one value per theme, verified AA. The
brutalist surface (zero radius, no shadows, hairline rules, 2px borders) is likewise kept. Its
structure, semantics, metrics and accessibility are adopted; its shape and elevation are not.

**The app list, not a row of chips.** The first pass offered six apps as six chips, which fitted a
desktop mock and quietly implied the product had six apps. A phone has sixty. It is now a
full-height panel: the entry being scoped named at the top, a search field, a scrollable list of
installed apps each with its icon and package name, and an honest count (`22 apps installed, 2
matching`). Only the first five apps have hand-drawn marks; the rest get a monogram tile, because a
mock should not invent marks it does not have. **On the device none of this is needed:
`PackageManager.getApplicationIcon()` returns the real icon, and
`getInstalledPackages()` is the list.** The SVG marks are trademarks and must not ship.

### Motion

Four movements, and nothing else. The project's standing rule is *structure draws itself, and
nothing moves on its own* — so every one of these is caused by something you did, and every one
shows what changed. No entrance-on-scroll, no fades-and-slides, no parallax, no progress bar.

1. **The tab ink travels** to the tab you tapped, rather than a fill blinking on somewhere else.
2. **The app list pushes in from the right, and leaves the same way** — enter and exit share a
   path, and the rail stays still, because the app chrome is chrome.
3. **A row you just added draws its own perimeter clockwise, once.** This is the *same device*
   the permissions page uses for "granted" (a conic-gradient mask over a 2px border). Reused, not
   reinvented — which is the point of having a design language. But the persistence differs and
   deliberately so: "granted" is a lasting state so its sweep settles visibly, while "just added"
   is transient, so **the sweep leaves no trace** when it lands.
4. **A row you just deleted collapses** and the rows below close the gap. Height only — never a
   fade-and-slide, which is the most recognisable generated-page tell.

All springs, **all critically damped (ζ = 1.0)**. There is no flick or throw anywhere on this
screen, so nothing earns overshoot — bounce on a control that carried no momentum reads as a toy.
A spring has no duration: it re-targets from its current value, so tapping a third tab mid-travel
turns the ink around from where it is rather than snapping. Verified: retargeting mid-flight went
70px → 198px with no snap back to zero.

`prefers-reduced-motion` is **not "no feedback"** — the spring lands on its target in one step, so
every state still changes, visibly and instantly; only the travel is removed. Nothing on this
screen depends on motion to be understood. Verified in both modes.

**Two real bugs the motion introduced, both found by measuring rather than reasoning:**

- **`frame()` killed its own rAF loop.** It wrote to `.tabs` unconditionally, but the panel view
  has no tab bar, so it threw — and because the reschedule is the last statement in the callback,
  the loop died permanently. The app list froze at `--push=0.9757` and never moved again, in either
  direction. Every DOM write in an animation loop is now guarded. The general rule: **an exception
  inside a `requestAnimationFrame` callback is terminal, because nothing reschedules.**
- **The sweep left a permanent accent outline.** Copying the permissions page's device copied its
  persistence too, which is wrong for a transient fact. The marker is now removed on landing.

### Verified, not asserted

- **72 controls hit-tested at 48×48** across all four screens, by probing the rendered page rather
  than by adding up paddings. The first pass had five controls under the floor, including the add
  button at ~25px (`padding: 0 16px`, so its height came from its text alone) and the **delete**
  button at ~23px — the destructive action had the smallest target on the screen. Fixed with a
  `--tap: 48px` token, and controls whose visual should stay small (the rail icons, the app chip,
  the preset buttons) reach the floor through a pseudo-element rather than by growing their box.
- **29 behaviour checks and 30 motion checks pass**, covering the add flow, the app list, search
  by name and by package, leaving two ways, the styles overrides, and every movement above.
- One regression caught only by looking at the rendered page: the rewrite briefly shipped **with no
  tab bar at all**, so Snippets and Styles were unreachable. Every isolated unit test passed,
  because the view functions were fine — the navigation around them was missing. Screenshot or no
  screenshot, that one only shows up on screen.

## What building it turned up

Five things the ticket did not know when it was written. None is resolved here; they are the
reasons the ticket is parked.

**1. The premise about `RouterContext` is wrong.** The ticket says `RouterContext` and the
refiner's app-style stage "both already assume an app can be identified". `RouterContext` does
not: ADR-0004 gives it `privacyMode`, `offline`, `costCeilingMicrosUsd`, `preferredProviderId`,
and nothing else. ADR-0003 re-resolves the target by package + characteristics, but that is the
**inserter's** private concern at insertion time — it is not a router input and not a refiner
input. So "plausible mechanism" is weaker than the ticket claims: per-app scope needs **new
plumbing end to end**, not just a new screen, and the merged surface still has to say where the
app identity enters the pipeline.

**2. No app-category taxonomy exists — and per-app scope no longer needs one.** `refiner.md` stage 8
says styles are "assigned per app category", and no category is defined anywhere in `docs/` — the
only trace of one is `reference-apps.md` noting that Wispr "adapts tone per app context (chat vs
email vs notes)". The merged surface answers this by **scope**: an entry names a package, not a
category. **`refiner.md` stage 8 should be amended to say per-app rather than per-app-category**,
because as written it promises a taxonomy nobody has agreed on.

**3. `refiner.md`'s "whole phrase" under-specifies matching in two directions.** It settles
neither whether an entry may be **multi-word** (the ticket assumes yes) nor whether matching is
**word-boundary anchored**. Both change what an entry *is*, so both are the ticket's first
question wearing a different hat. The prototype matches a single word on word boundaries and a
spaced entry as a substring, and says so on the surface — disclosed, not decided.

**4. Running style last, which the document gets right for the wrong stated reason, produces
punctuation bugs.** Stage 8 is "capitalization + ending punctuation" and "runs last so it sees
final content". Because it runs last, a snippet that already ends in punctuation arrives
*finished*: appending a full stop blindly turns `sig` → `Best regards,` into **`Best regards,.`**.
The prototype hit this on its first run and now declines to add a full stop to text that already
ends in any punctuation. Worth deciding deliberately rather than inheriting — and the general
point is that "runs last so it sees final content" is an argument for *reads*, not for *writes*.

**5. An entry row cannot hold a real snippet, because snippets are not one line.** The row is
`from → to`, which fits `sig → Best regards,` and fits nothing longer. But the point of a snippet is
often a block — a sign-off, an agenda, a canned paragraph — and `tl;dr → To put it briefly:` is
already at the edge of the row. **A snippet whose expansion is multi-line has nowhere to go on this
surface**, and every answer to "what does a dictionary entry look like" has to survive the case.
This is the one place where merging A and B lost something: the bench in C was the only view that
showed an expansion at length. Open, and it is a genuine question rather than a polish item —
clamp with an expand affordance, a separate detail screen, or admit that V1 snippets are
single-line and say so.

## To resolve this

Two of the three questions are now settled by the merge. What is left:

- **Entry shape** — the open half is finding 5: what happens to a snippet that expands to more than
  one line. Single-line with a clamp, a detail screen, or an explicit V1 limit?
- **Matching** — finding 3: word-boundary anchored or substring, and are multi-word entries allowed?
  `refiner.md` settles neither and the surface currently discloses a choice rather than making one.
- **Where app identity enters the pipeline** — finding 1, and the one thing the prototype cannot
  answer, because `core/` has no `src/` and the honest answer needs code that does not exist yet.

## Answer

**The surface is settled; three questions are explicitly not.** Those are different outcomes and
recording them as one would be the exact quiet redefinition this map forbids.

This one **was** resolved through live exchange — unlike ticket 34, the owner made the decisions
here rather than signing off on the agent's. The shape below is theirs.

### Settled

**One surface, not a choice between three.** Variants A and B are merged and C is dropped. Two of
the ticket's three questions were settled *by* the merge rather than by argument:

- **Per-app scope is in, and it is per-package, not per-category.** Scope is a property of an
  entry — one row reads `sig → Best regards,` with an app chip under it — and tapping the chip
  opens a searchable list of the apps on the phone. **This dissolves the missing taxonomy
  (finding 2): naming a specific app the user recognises is a different act from naming a
  category, so V1 needs no taxonomy at all.** `refiner.md` stage 8 must be amended from
  "assigned per app category" to per-app, because as written it promises a taxonomy nobody agreed
  on. **That amendment is not done and is owed.**
- **Styles is configuration for the refiner's stage 8**, not a second feature wearing a different
  name. The Styles tab offers the three presets, then only the apps that differ from the default.

**The generalisable move: a per-item scope beats a per-axis scope.** B's separate scope rail made
the user hold two hierarchies at once and then do arithmetic about which entries applied where.
Moving the app onto the entry makes the common case one tap and the rare case three taps, and both
are obvious.

**Motion** was added on the owner's instruction and follows the map's standing rule — *structure
draws itself, and nothing moves on its own*. Four movements, all springs, all critically damped
(ζ=1.0) because nothing on this screen is a flick and so nothing earns overshoot.

### The dark-mode defect: fixed here, not deferred

The owner noted dark-mode issues and said they could be fixed during the app build. One of them
could not be deferred, because it was not cosmetic:

> **Buttons do not inherit `color`.** The UA stylesheet gives them `buttontext` — black. On this
> surface that rendered **pure black on a near-black ground: 1.06:1**, for the app name in the app
> list and for "Choose an app" in Styles. **Light mode hid it completely** (black on paper is
> 19:1), which is exactly why every earlier check missed it — this surface had only ever been
> audited in one theme.

Fixed with one line, `button { color: inherit; }`, so no button can fall back to the UA colour
again. Re-audited with an alpha-compositing checker that also understands the selected tab's ink
being a *sibling* block rather than an ancestor background: **70 text roles measured in both
themes, all meet WCAG AA.** The lowest is 4.64:1 (the small mono notes in light).

The lesson worth keeping: **an audit run in one theme is not a contrast audit.** Light mode is
where a colour bug hides best.

### Deferred to the app build, by the owner's call

- **Icon legibility in the app list.** The owner saw the hand-drawn marks and judged them unclear.
  Correct, and unfixable here: `PackageManager.getApplicationIcon()` returns the installed app's
  real icon, which is both more correct and free. The SVG marks exist so the mock is
  self-contained, are trademarks, and **must not ship**.
- Remaining dark-mode polish, now that the contrast failure is gone.

### Explicitly still open — owned by new tickets, not resolved here

- **A snippet longer than one line** has nowhere to go on an entry row. → *What does a snippet
  longer than one line look like?*
- **Where app identity enters the pipeline.** `RouterContext` carries no app identity, so per-app
  scope needs new plumbing end to end and the surface cannot be built without it. →
  *Where does app identity enter the pipeline?*
- **`refiner.md` stage 8's amendment**, above. Small, but owed, and it is a stale claim the moment
  this ticket closes.

### Verified

72 controls hit-tested at 48×48 by probing the rendered page; 29 behaviour checks; 30 motion
checks; 70 text roles in both themes; zero runtime errors. The prototype is a single
self-contained file at `.scratch/openflow-v1/prototype/dictionary-snippets-styles.html`.