# 0013: The order of first launch

Date: 2026-10-03

## Status

Accepted — amended 2026-10-04: post-setup model recovery, settled by [Where does MODEL_MISSING put its download prompt?](../../.scratch/openflow-v1/issues/54-where-does-model-missing-put-its-prompt.md).

Amended 2026-10-05: download-size presentation and storage-check policy, settled by
[Is 175 MB the headline?](../../.scratch/openflow-v1/issues/56-is-175-mb-the-headline.md).

Settles the "flow order deliberately left undecided" note in [Onboarding &
permission-health flow](../../.scratch/openflow-v1/issues/19-onboarding-permission-health.md).
It is the screen-level consequence named by [0010](0010-release-artifact-shape.md)
("a new first-run screen"), and it does not amend that ADR.

## Context

[0010](0010-release-artifact-shape.md) made model delivery hybrid: the Silero VAD model
(629 KB) is bundled, and the streaming ASR archive is downloaded on first launch:
127,887,156 bytes (about 128 MB downloaded), with about 45 MB retained after selective
extraction. The previously stated 45 MB fetch conflated installed and downloaded size.
That decision has a consequence it could only name, not design: **on first launch the
user has no recogniser**, so a mandatory, slow, fallible network step now sits between
opening the app and the thing the app is for.

Three settled decisions constrain where it can go.

- **[Android permissions & Play policy for a dictation bubble](../../.scratch/openflow-v1/issues/03-android-permissions-policy.md)
  settled that permissions are requested contextually, not at first launch.** So this
  flow has to justify itself against that rule rather than against a generic wizard —
  and a model download, which asks the user for nothing but disk and network, must not
  borrow the permission screen's context to make itself feel smaller than it is.
- **[Onboarding & permission-health flow](../../.scratch/openflow-v1/issues/19-onboarding-permission-health.md)
  settled what the permissions screen looks like and left the order open on purpose**,
  pending exactly this question.
- **[0011](0011-bubble-overlay-window.md)** settled the accessibility prominent
  disclosure's content, and that `POST_NOTIFICATIONS` is requested at first dictation
  and must not be pulled forward to make a flow feel complete.

The order was decided against a prototype rather than argued on paper. Three
alternatives were built and measured, at `.scratch/openflow-v1/prototype/first-launch.html`.

## Decision

**First run is four steps, and the download is a blocking one.**

`Welcome → Voice model → Dictation service → Ready`

- **The download is opt-in, not automatic.** A 128 MB fetch the user did not ask for is a
  different act from one they chose, and on a metered connection it is a cost they did
  not agree to. Automatic would be defensible for 629 KB and is not defensible for 128 MB.
- **The cost is stated before the tap, not after it.** Welcome and model recovery lead
  with “128 MB download”, followed by “About 45 MB installed · About 175 MB needed during
  setup.” The temporary-storage estimate is approximate, not an exact installation
  cutoff. The decision detail is recorded in
  [Is 175 MB the headline?](../../.scratch/openflow-v1/issues/56-is-175-mb-the-headline.md).
- **The download size, installed size, estimated temporary storage, source and integrity check are a permanent
  footer** of the download surface, in every state: idle, running, failed, and ready.
  There is no "see more", because a number that disappears while you wait for it is a
  number you cannot hold us to.
- **Check storage before fetching.** A known shortage offers storage settings and a
  recheck on return. If storage cannot be queried, explain that and allow an explicit
  Download tap. Passing preflight does not guarantee subsequent writes; derive the
  enforced requirement from verified sizes and justified headroom. Its numeric value
  remains a separate decision in
  [What verified storage requirement should model setup enforce?](../../.scratch/openflow-v1/issues/62-model-setup-storage-requirement.md).
- **One progress indicator per screen.** One meter, and the phase named in words beneath
  it — which is the thing a bar cannot say anyway. Two bars is not honesty, it is noise,
  and it quietly says the app cannot decide what is happening.
- **Every failure names itself, in English, and offers the action that fits it.**
  "Not enough space" hands off to the system storage settings and the app keeps its
  state, so coming back retains the flow and rechecks storage; fetching still requires
  an explicit tap. This does not promise byte-range resume. A refusal the user cannot act on
  is not an error message, it is a dead end.
- **A metered connection is a pause, not a failure.** It is a question, so both answers
  stay on screen for as long as it is true, and choosing to wait does not dismiss the
  question.
- **The accessibility disclosure sits after the download and before Ready.** It is
  consented to before the service is enabled, and `POST_NOTIFICATIONS` is not on this
  screen at all.
- **The primary action lives in the bottom bar, in every state.** A blocking first-run
  task that can put its own action somewhere it might not fit is not blocking, it is
  hopeful.

## Why these

**Why is a download in the onboarding flow at all, when permissions are contextual?**
Because a download is not a permission, and conflating the two is what the rule exists to
prevent. It asks for no capability, grants no ongoing access, and appears in no
permission list. It also cannot be deferred to a point of use, because there is no point
of use before the first dictation — the whole reason it is on first launch is that
nothing else in the app needs a recogniser yet.

**Why blocking, when "not blocking" is the more modern answer?** Because "not blocking"
is only better if there is something to do meanwhile, and at first launch there is
nothing: no dictionary, no history, no snippets, and dictation itself impossible. The
honest version of a non-blocking download on the first screen would have been an empty
screen with a progress bar on it. The distinction the ticket drew is the right one —
"cannot" (dictation is impossible without the model) is not "must not" (the app is
perfectly usable to look at) — and at first launch the second half is false. Three taps
follow the download, so blocking costs one wait, once.

**Why is the disclosure after the download?** This is the ordering most worth defending,
because it is the one that feels wrong. The user has just spent a wait on 128 MB, and
interrupting that wait with a consent request is the worst possible moment to ask
anything of them.

It still goes second, because the alternative positions are both worse. **First** opens
the flow with a consent request, before the user knows what the app is or what it will
do with the service — asking for consent to a capability nobody has seen the value of is
the way to be refused. **Interleaved with the download** puts a modal consent dialog over
a progress bar, which is both: it interrupts the wait *and* asks for something whose
purpose has not been established. **After** the model is on the phone, the disclosure is
the last thing before the app works, so it is read by someone who has just watched the
app do the only thing it could do.

**Why opt-in?** See above: 128 MB is not a rounding error, and the whole point of
`Model Delivery`'s split is that this download is a cost the user bears.

## Considered options

Three alternatives were built and measured before this one was chosen. All three are
recorded because each failed on something a paper argument would not have found.

- **Auto-start, not blocking, a rail on every screen.** Rejected, and it failed on its
  own terms: it showed **two** progress indicators on two of its three screens and
  **three** inside its own bottom sheet. The variant whose entire argument was "the app
  is usable meanwhile" could not decide what was happening on its own screen.
- **Opt-in, not blocking, the download as the welcome screen** — a 45 MB numeral as the
  hero. Rejected: it was the same measurement twice, a counting numeral and the trace,
  and the numeral as hero made a fact look like an achievement.
- **Contextual and blocking: nothing is fetched until the user holds the bubble.** This
  is the variant that fails hardest against the settled rules. It makes an
  accessibility-service disclosure and a permission the *consequence of a drag gesture*,
  which is precisely the pretext [Android permissions & Play policy for a dictation
  bubble](../../.scratch/openflow-v1/issues/03-android-permissions-policy.md)
  rules out. It also pushes the download past the point where the user has agreed to
  anything at all.

## Consequences

- **There are now two entry points to one download surface.** [0011](0011-bubble-overlay-window.md)
  is explicit that the "download the speech model" prompt "belongs on the user's screen
  only once every candidate has failed" — and on first launch, with one real provider,
  that is immediately true. So the surface serves both first run and `MODEL_MISSING`
  afterwards. The post-setup entry point is settled by
  [Where does MODEL_MISSING put its download prompt?](../../.scratch/openflow-v1/issues/54-where-does-model-missing-put-its-prompt.md):
  Settings offers Voice model recovery, and the Bubble offers that action only after
  eligible provider routes are exhausted because the model is missing. Reuse the download
  controls with recovery wording, preserving existing setup and consent. Missing or
  downloading weights block dictation only; History, dictionary, snippets and settings
  remain accessible. Full app-data clearing follows first-run setup instead.
- **Recovery remains explicitly initiated.** Every fetching surface keeps the permanent
  cost/source/integrity footer, one progress indicator and phase labels, failure actions,
  and the metered-connection choice. POST_NOTIFICATIONS is not requested here. After
  verification and successful model preparation, show “Ready to dictate”; require a fresh
  Bubble tap and target/safety checks, leaving the microphone closed until then.
- **In-app navigation does not cancel recovery downloads.** Settings exposes the current
  state and an explicit Cancel action while the app remains open. Process termination
  leaves Retry available on reopening; reopening never starts a new fetch automatically.
  This does not promise unattended background completion or byte-range resume.
- **The pipeline tolerates an absent model; the first-run flow does not.** These are not
  in conflict, and the distinction is worth stating because it reads like one:
  `MODEL_MISSING` remains a Provider Health value rather than a crash, so after first run
  a user whose model has been deleted can reach a dictation action with the model gone.
  First run blocks; the pipeline does not.
- **Ticket 19's prototype is now wrong in one specific place.** Its permissions list
  includes a row reading "Voice model — 45 MB once, then offline", and a download is not
  a permission. That asset is a resolved ticket's record, so correcting it is its own
  ticket rather than an edit smuggled in here.
- **The headline decision is now settled.** Download cost leads on Welcome and recovery;
  installed size and approximate setup storage remain visible. The resolution and its
  storage-check edge cases live in
  [Is 175 MB the headline?](../../.scratch/openflow-v1/issues/56-is-175-mb-the-headline.md).
- **The prototype is throwaway and lives at
  `.scratch/openflow-v1/prototype/first-launch.html`.** It is not a specification. What
  transfers is the order, the copy, and the rule that every surface states what it costs
  and what it cannot do. Its historical 45 MB download wording is superseded by the
  verified amounts and presentation decision above.
