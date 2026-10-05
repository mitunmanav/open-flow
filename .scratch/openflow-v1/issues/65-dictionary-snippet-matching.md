# How do dictionary and snippet entries match spoken text?

Type: grilling
Status: resolved
Blocked by: 37, 52, 60

## Question

Define how stages 6 and 7 select entries and replace their matching input ranges.
The surface permits scoped entries, but its prototype's single-word boundary matching
and multi-word substring matching were disclosed rather than agreed. Production must
not inherit them silently.

Preserve the settled pipeline order and dictionary's whole-phrase, case-insensitive
contract. Decide whether both entry kinds allow multi-word input, what constitutes a
phrase boundary, and how case, punctuation and whitespace affect snippet matching.
Specify overlapping matches, repeated occurrences, duplicate inputs and collisions
between applicable scopes. Matching rules may differ by entry kind if justified.

Use concrete cases such as `sig` within `signal`, `sign off` within a longer sentence,
`sign` competing with `sign off`, and identical shorthand in applicable scopes. State
whether conflicts are rejected in settings or resolved deterministically at runtime;
entry-list order must not accidentally become the contract.

Snippet expansion is already settled as one pass over stage 7's input. Authored
replacement text remains literal and cannot trigger another snippet. Matching must
produce the authored/dictated origin segments required by the style handoff; do not
reopen those decisions or implement the production refiner in this ticket.

Context:
[The dictionary, snippets and styles surface](37-dictionary-snippets-styles-surface.md),
[Where does app identity enter the pipeline?](52-where-does-app-identity-enter.md), and
[How should app-style formatting treat a snippet's authored text?](60-style-authored-snippet-text.md).

## Comments

### 2026-10-05 — Facts checked before discussion

The prototype's disclosed rule branches on whether the trigger contains a space: a
single-word entry becomes `\b…\b`, an entry with a space becomes a literal substring.
So `sig` is safe inside `signal` and `sign off` is not — it fires inside "design off".
`refiner.md` settled "whole phrase, case-insensitive" for the dictionary only and left
snippet matching unspecified, then handed the whole question here. The surface
separately discloses that an app's own entry beats the every-app one.

No refiner code exists (`core/` has no `src/`), so this is a contract decision with
nothing to read and nothing to run. Ticket 60 settled one-pass expansion, literal
bodies and dictated/authored segments — and scoped that origin handoff to **stage 7
only**, so a stage 6 replacement body's origin was unclassified going in.

### 2026-10-05 — Rounds one and two accepted

The owner accepted all recommendations across three rounds: the matcher and its
tokenizer, conflict handling in settings rather than at runtime, origin classification
for dictionary bodies, cross-stage consequences, and where the contract is written.

Two findings worth keeping from the exchange. **Two rules this ticket had already
agreed contradicted each other**: nearest-scope-wins and longest-match-wins give
different answers for a WhatsApp-scoped `sign` against a global `sign off`, so
specificity was made a per-position filter ahead of the scan. And **origin was being
asked to carry two unrelated answers** — may stage 8 write here, may stage 7 rescan
here — which one two-value enum cannot hold, so origin now means stage 8's write
authority and stage 7's rule is stated separately.

## Answer

Resolved through three live rounds on 2026-10-05: the owner accepted all eighteen
recommendations. **The prototype's disclosed matching is rejected in full** — the
branch on whether a trigger contains a space is the defect, not the disclosure.

### One matcher, both kinds

Stages 6 and 7 share a single token-sequence matcher. A Dictionary entry and a Snippet
differ only in what the body is for, so nothing justifies different rules and **multi-word
triggers are allowed for both**. The prototype's asymmetry dies here: there is no branch
on the trigger's shape, so there is no case where a trigger is safe in one spelling and
unsafe in another.

### What a token is

A token is a maximal run of Unicode letters and digits, where an apostrophe (`'` or `’`)
**between letters is internal** and every other non-alphanumeric character separates.
Comparison is Unicode case-folded, with **no accent folding**.

- `don't` and `it's` are one token each; `sign-off` and `e-mail` are two; `iphone15` is
  one token while `iphone 15` is two; `24/7` is two.
- `Café` matches `café`. `cafe` does **not** match `café` — accent folding would also
  make `resume` match `résumé`.
- Because punctuation and case are not load-bearing, the trigger field accepts any of
  them freely. That tolerance is invisible unless the surface says so, which is why
  stating it per entry is part of this decision.

### What one match replaces

Any run of whitespace between tokens counts, newlines included, so a match may span a
paragraph break. **The replaced range runs from the first matched token's start to the
last token's end**, and nothing outside it is touched: irregular spacing *inside* a
match disappears with the match, and spacing outside is untouched verbatim. Leading and
trailing whitespace in a trigger needs no trimming rule, because the tokenizer gives it
no tokens.

Matching is defined **on the text as it reaches stages 6 and 7, re-tokenized there** —
deliberately independent of stage 1's normalization, so no future change to stage 1 can
silently change which entries match.

### How matches are chosen, and why order cannot matter

Per position, longest applicable match wins; then one left-to-right scan resumes after
each match; every occurrence is replaced. With one exception, made to keep the surface's
own sentence true:

> **Specificity filters ahead of the scan, per position.** If any app-scoped entry
> matches at a position, the longest of *those* wins; only when none does is the longest
> global match considered.

That exception was found during the exchange, not chosen up front. Nearest-scope-wins
and longest-match-wins as first stated **contradict each other** for a WhatsApp-scoped
`sign` against a global `sign off` dictated into WhatsApp. Filtering per position keeps
both promises: the scan stays order-free, and "the app's own version wins over the
every-app one" is true in the hardest case rather than only when the triggers are
identical. The cost is real and intended — an app's short trigger beats a global longer
one, because the user's app-specific intent outranks a generic match.

**Entry-list order is never consulted**, so "entry-list order must not accidentally
become the contract" is true by construction rather than by convention.

### Conflicts

| Case | Outcome |
| --- | --- |
| Same trigger, same scope | **Rejected at save**, naming the existing entry |
| Same trigger, global vs one app | Allowed; app-scoped wins by specificity |
| Same trigger, two different apps | Allowed; never compete — a Dictation has one package (ticket 52) |
| Same trigger, dictionary *and* snippet in one scope | **Allowed.** Stages are ordered: dictionary runs, then the snippet fires on its output |

Duplicates are compared **as token sequences**, so `sign-off` plus `sign off` in one
scope is a duplicate — the check agrees with the matcher. Rejecting in settings is
chosen over a runtime tie-break because it is the only option where the user *learns*,
and it deletes an ambiguity from the runtime contract instead of encoding one. Cross-kind
overlap is not a conflict at all, since the pipeline order already decides it.

### Origin: two values, and only about stage 8

Origin is **dictated / authored**, and it means **stage 8's write authority only**.

Stage 7's rule is a separate statement of the shape ticket 60 already used: stage 7's
*own* insertions are never rescanned by stage 7, while a stage-6 body **is** scannable by
stage 7. A stage-7 match spanning a stage-6 boundary produces a wholly authored segment
— never partial. No third origin value and no per-stage flag on segments: an enum that
carried both answers would be a lie about what it controls.

**A dictionary replacement body is authored**, the same as a snippet body, so stage 8
cannot repair its capitalisation (`ios → iphone` stays lowercase under Formal). That is
the user's own spelling, and ticket 60's rule is that authored text is the user's. A user
who wants stage 8's help has no unprotected path, which is consistent.

### Consequences accepted rather than engineered away

- **A snippet body is never dictionary-corrected**, because stage 6 already ran.
  `sig → Kubernetis` stays uncorrected. The alternative — re-running stage 6 after
  stage 7 — would reopen the pipeline order tickets 12 and 60 both fixed.
- **A dictionary entry whose right side is a trigger does expand**: `wrong → sig` yields
  the snippet, which is the pipeline order working as intended.
- **Triggers are unusable in a script without word spacing.** Under token-sequence
  matching a CJK run is one token, so a zh trigger could only match an entire run. V1
  accepts this: a second per-language matcher costs more than the feature is worth when
  the default is English and the bilingual tier is a download-tier test. It is written
  down so it cannot quietly read as support.

### Concrete cases

| Spoken | Applicable | Result |
| --- | --- | --- |
| `the signal is weak` | `sig` | Unchanged — `signal` is one token, not `sig` |
| `please sign off the draft` | `sign off` | Expands — a whole-sentence occurrence, not just a leading one |
| `i will sign off now` | `sign`, `sign off` | `sign off` wins on length at the same start; not on list order |
| `sig sig` | `sig` | Both occurrences replaced |
| `design off the layout` | `sign off` | Unchanged — `design` is one token |
| `please sign-off` | `sign off` | Expands — the hyphen separates tokens |
| `the design off plan` in WhatsApp | global `sign off`, WA `sign` | WA's `sign` wins; specificity filtered first |
| `i will sign off` | global `sig`, WA `sig` | WA's `sig` wins by specificity |
| `sig` as dictionary *and* snippet | both | Dictionary first, then the snippet on its output |
| `em will go` + `em → I`, then `i will` | both stages | `I` is stage-6 authored and scannable; stage 7 matches across the boundary and the range becomes wholly authored |

### Where this is recorded

A new **Entry matching** section in `docs/architecture/refiner.md`, which also replaces
stage 6's "whole phrase, case-insensitive" and re-points the deferred-decision note that
sent this question here. `GLOSSARY.md` gains Dictionary Entry and Origin Segment. **No
new ADR**: tickets 51 and 60 recorded their decisions in `refiner.md` and neither needed
one, and a new ADR file would put one rule in two places.

Two amendments elsewhere are part of this decision rather than follow-ups: the
prototype's closing note is **false** under this rule and is replaced by a per-entry
statement in mono under the trigger field, and `G3` widens from "a dictionary entry" to
"a dictionary entry or snippet" with **no new scenario ID**, so the scenario count does
not move. The matcher is deterministic string logic with no device in it, so it is
unit-tested in `core` and not gated — ticket 44's precedent.

**No production refiner is implemented here.** That gap now has an owner:
[Nobody owns implementing the TranscriptRefiner](68-implement-the-transcript-refiner.md).

