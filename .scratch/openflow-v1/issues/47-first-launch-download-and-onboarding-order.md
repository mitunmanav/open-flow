# Where the first-launch model download sits in the onboarding flow

Type: prototype
Status: resolved
Blocked by: none

## Question

This graduates the map's last deliberately-open fog item. Ticket 19 settled what the
permissions screen *looks like* and left the flow order undecided on purpose, pending the
contextual-permission decision in the map's Notes. ADR-0010 has now changed the shape of
first launch, so the order question has a new term in it and can be asked.

**What changed:** the streaming ASR model is no longer bundled. On first launch the user has
no recogniser, and OpenFlow will fetch about 128 MB from the network, retaining about
45 MB after selective extraction, before a Dictation is
possible. That is a mandatory, slow, fallible step that did not exist when ticket 19 left
the order open.

The decision is **where that step sits relative to everything else on first launch** — and
it is constrained from both sides:

- **Ticket 03 settled that permissions are requested contextually, not at first launch.**
  So this flow must justify its order against that rule, not against a generic wizard. A
  model download is not a permission and must not be used as the pretext for one.
- **Ticket 19's honesty rule sets each ask against what it cannot do**, and the map's Notes
  name it "the one idea worth stealing". A first-run screen that downloads 128 MB is the
  most conspicuous place in the product to keep that promise, and the most conspicuous
  place to break it.

Sub-questions worth answering rather than assuming:

- Does the download start automatically on launch, or wait for a deliberate tap? An
  automatic 128 MB fetch on a metered connection is a different act from one the user chose.
- Is it blocking? The user cannot dictate without it, but they could be shown what the app
  does first. "Cannot" and "must not" are different constraints and the honest UI honours
  both.
- What happens on failure and on cancellation, given that the transcript pipeline cannot
  run? This is where Provider Health's `MODEL_MISSING` becomes something a person sees
  rather than a value the router reads.
- Does anything about this belong in the *permission* screen at all, or is it a separate
  first-run step that must not borrow that screen's context?

**Two things now settled upstream that this flow has to accommodate** (ticket 34,
`docs/adr/0011-bubble-overlay-window.md`):

- **The accessibility prominent disclosure exists and has settled content**, and this
  ticket owns **where it sits**. It must be standalone, in normal usage flow, with
  affirmative consent, and must not set `isAccessibilityTool`. Its content is no longer
  open — the event types are named, there is no keyboard watching, and the disclosure has
  to say the service *can* read the focused field's text and declines to. What is still
  open is its order relative to the download, which is genuinely in tension: the download
  is the first thing a user must wait through, and the disclosure is the first thing they
  must consent to.
- **`POST_NOTIFICATIONS` is requested at the moment of first dictation**, not on this
  screen. Do not pull it forward to make the flow feel complete. It is a platform
  requirement for the foreground service to be visible, and it only means something once
  there is something to record.

React to the asset ticket 19 already produced at
`.scratch/openflow-v1/prototype/onboarding.html`, and to the permissions variants beside it
— the design language (brutalist, zero radius, hairline rules, electric violet, machine
voice in mono) is settled and is not up for renegotiation here. This ticket is about
**order and what the screen has to say**, not about appearance.

## Answer

**The download is a blocking, opt-in step, second of four. Reasoned in
`docs/adr/0013-first-launch-order.md`; the prototype is
`.scratch/openflow-v1/prototype/first-launch.html` (throwaway).**

`Welcome → Voice model → Dictation service → Ready`

The four sub-questions, answered:

- **Automatic or a deliberate tap?** A tap. 128 MB is not a rounding error, and an
  automatic fetch on a metered connection is a cost the user never agreed to.
  A **metered** connection is therefore a *pause*, not a failure — a question with both
  answers on screen, and choosing to wait does not dismiss the question.
- **Blocking?** Blocking, and that is the part worth arguing. "Not blocking" is only
  better when there is something to do meanwhile, and at first launch there is nothing:
  no dictionary, no history, no snippets, and dictation itself impossible. The honest
  version of a non-blocking download on screen one would have been an empty screen with
  a progress bar on it. Three taps follow the download, so blocking costs one wait, once.
- **Failure and cancellation?** Every failure **names itself in English** and offers the
  one action that fits it. "Not enough space" hands off to the system storage settings
  and the app keeps its state, so returning resumes rather than restarts — a refusal the
  user cannot act on is a dead end, not an error message. The size, the **peak free
  space**, the source and the integrity check are a permanent footer in *every* state,
  including while running and after failure.
- **Anything in the permission screen?** No, and this is the constraint the ticket set
  hardest. A download asks for no capability and grants no ongoing access; putting it in
  a permissions list would have borrowed that screen's context to make itself feel
  smaller than it is. The accessibility disclosure therefore sits **after** the download
  and **before** Ready, consented to before the service is enabled — not first (a consent
  request for a capability whose value has not been shown is how you get refused), and
  not interleaved with the progress bar (that is both problems at once).
  `POST_NOTIFICATIONS` is not on this screen at all.

### What the work found that the ticket did not ask

**Three answers were built and rejected on measurements, not arguments.** The
auto-start rail variant failed on its own terms — two progress indicators on two of its
three screens and **three** inside its own bottom sheet. The numeral-as-welcome variant
was the same measurement twice. The drag-to-fetch variant is the one that fails hardest
against settled rules: it makes an accessibility disclosure and a permission the
*consequence of a gesture*, which is exactly the pretext
[Android permissions & policy](03-android-permissions-policy.md) rules out.

**Download, installed size and temporary storage are distinct costs.** The archive
costs about 128 MB to download; about 45 MB remain installed, and about 175 MB is the
temporary setup-storage estimate. The original answer called 45 MB the download and
compared it with the setup estimate as a 4× difference; that factual error was corrected
on 2026-10-05. Presentation and preflight policy are now settled by
[Is 175 MB the headline?](56-is-175-mb-the-headline.md).

**The pipeline tolerates an absent model; first run does not.** That reads like a
contradiction and is not: `MODEL_MISSING` stays a Provider Health value rather than a
crash, so after first run a user whose model was deleted can reach a dictation action
with the model gone. But it also means **the same download surface has two entry points,
and the second has no owner** —
[Where does MODEL_MISSING put its download prompt?](54-where-does-model-missing-put-its-prompt.md).

**A resolved ticket's asset is now wrong in one place, deliberately not fixed here.**
[Onboarding & permission-health flow](19-onboarding-permission-health.md)'s prototype
lists a row reading "Voice model — 45 MB once, then offline" among the permissions, and a
download is not a permission. It is that ticket's record, so the correction is its own
ticket rather than an edit smuggled into this one —
[Correct the permissions list that lists a download as a permission](55-correct-the-permissions-list.md).

### What is settled about the prototype's own construction

Recorded because it is reusable and the map has been paying for the same lesson repeatedly:

- **Every resting style is plain CSS, and every animation is keyed to the state it
  animates *from*.** Six resting states were held by `animation-fill-mode: forwards`,
  which means those animations were still **attached** to their elements after
  everything settled — eleven of them. And because `render()` replaces `innerHTML`, the
  perimeter sweep replayed on a **font-scale tap**: a one-shot that restarts when
  nothing changed is not a one-shot. Probing the running page is the only way either is
  visible; a screenshot shows both as fine.
- **A harness can lie about the thing it measures.** The prototype's control bar reserved
  a fixed `8rem`; at 200% font it is 872px tall and lay across the phone, covering the
  primary action. Every large-font hit-test was failing for a reason that had nothing to
  do with the product, so a "green at 200%" run meant very little.
- **A container query cannot express a display-to-type ratio.** Chromium compares the
  container's pixel width against a rem threshold resolved at the root, so `20rem` means
  640px at 200% font and the reflow query silently stopped matching at exactly the sizes
  it was written for. It is measured on the real element now.
- **A press is not a transition.** The primary action's pressed state inverts, so a 100ms
  cross-fade walked paper→ink and ink→paper at the same rate and measured **1.33:1** at
  +50ms. A static audit cannot find this, because it never hovers and the failing frames
  exist only while a finger is down.
- **Reduced motion is not the same animations, faster.** It is no animation at all, which
  is only safe because every resting style is plain CSS — and the one-shot classes are
  not added in the first place, since a class whose animation was suppressed outlives its
  purpose forever.

Final state: **94 checks, 240 screen-states audited** (both themes × 100/150/200% font ×
320dp and device width × every step and every download state), all green.

### Follow-ups

- [Where does MODEL_MISSING put its download prompt?](54-where-does-model-missing-put-its-prompt.md)
- [Correct the permissions list that lists a download as a permission](55-correct-the-permissions-list.md)
- [Is 175 MB the headline?](56-is-175-mb-the-headline.md)
