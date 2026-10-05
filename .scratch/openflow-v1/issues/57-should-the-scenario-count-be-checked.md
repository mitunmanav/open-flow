# Should the scenario count be checked or derived?

Type: grilling
Status: resolved
Blocked by: none

## Question

At charting, the gate had **fifteen** scenarios, and that number was restated in at least six live places:
`GLOSSARY.md`'s Acceptance Gate entry, `docs/README.md`'s index row, ADR-0008 (its Amended
header, its Context, its Decision and its Consequences), `docs/quality/acceptance-gate.md`
(the two-bar table and the scenario list's own heading), and the map. **Nothing checked that
they agreed.** This describes the original problem; the resolution below updates the guidance.

It has already failed once, silently. Ticket 44 moved the count fourteen → fifteen and
updated `acceptance-gate.md`, ADR-0008 and the map. It missed two live documents:
`GLOSSARY.md` said "**Fourteen** scenarios per device class", and ADR-0008's Consequences
still said the gate grew "from ten scenarios to **fourteen**, and thirty runs became
**forty-two**". Both were correct when written and wrong the moment the scenario was added;
neither was caught, because there is no rule and no failure — a document saying fourteen when
the gate runs fifteen looks exactly like a document saying fifteen.

The general shape is the map's standing one, in a new place: a claim restated by hand in
several places is a claim that will be right in some of them. Which is the third instance
here after `docs-check`'s original `paths:` filter and the `required_approving_review_count`
that nobody had seen pass.

The question is what replaces the hand-restating. Three shapes, and they are genuinely
different:

- **Derive it.** The count lives in exactly one place — the scenario table in
  `acceptance-gate.md` — and every other document links there instead of restating a
  numeral. Nothing can check this; there is nothing left to check. The cost is that a
  reader of `GLOSSARY.md` no longer learns the number without a click, and the glossary is
  where a term is supposed to stand alone.
- **Check it.** The numeral stays where it is useful and `check_docs.py` grows a rule that
  reads the scenario table and fails any document whose stated count disagrees. There is a
  direct precedent: `check_site_nav` already does exactly this for the website's navs, so
  the house already has this shape of rule. The cost is a sixth fact for the checker to know,
  and a rule that has to be taught the spelling of "fifteen" versus "15" versus "fifteen
  scenarios".
- **State the relationship instead of the number.** Documents say "every scenario listed in
  `acceptance-gate.md`" and never a count, which is a weaker version of derive that keeps
  the sentences readable.

Worth answering while grilling it: is the count even load-bearing in each place that states
it? ADR-0008's Consequences line about release cadence is about *cost* — a bigger number
means more hours — so it may want the number. `GLOSSARY.md`'s is a definition, and a
definition that carries a count is a definition that will be wrong.

## Comments

### 2026-10-05 — Facts checked before discussion

The current protocol contains exactly fifteen unique table IDs, G1–G15, under
“The scenarios”. Its mechanism introduction still says “These four” above five rows.
ADR-0008's Consequences describes forty-five scenario/device cells as “runs”; with
three attempts per cell the current full gate would require 135 attempts. These are
protocol arithmetic, not recorded measurements or evidence of a completed gate.

Current count statements must be distinguished from dated historical statements:
the original Context's ten scenarios and the amendment's fourteen-to-fifteen change
record earlier decisions, not current requirements. Do not indiscriminately compare
historical numerals with today's scenario count.

The live Gate Status JSON has no required-scenario registry or expected count. No
release checker is implemented yet. The existing enforcement handoff explicitly
requires reading only status JSON; deriving requirements from a Markdown sentence
would change that contract. The documentation checker currently has no scenario rule
and excludes the local tracker, so a general prose-count rule would not cover the map.

The decision round proposes relational current prose, preserved dated history,
and explicit required IDs in the same JSON block with documentation validation
against the scenario tables. These proposals are awaiting the owner's decisions.

## Answer

Resolved through live exchange on 2026-10-05. The owner accepted both recommendations,
confirming the shared understanding for the complete decision frontier.

### Current prose and historical counts

Current definitions and release requirements say **“every required scenario”**, linking
to the acceptance-gate protocol instead of repeating the scenario total. The glossary,
documentation index, protocol, ADR and map are updated accordingly. Category introductions
also avoid mutable counts, correcting “these four” above the mechanism table.

Preserve clearly dated counts as historical facts rather than treating them as current
requirements. Distinguish scenario/device **cells** from individual **runs**: each cell
requires three runs. Current workload is derived from the required-scenario total ×
device classes × runs per cell. The ADR's historical workload statement now identifies
cells and attempts correctly; none of this claims that any gate has been run.

### Required IDs and validation

The live JSON block under “Gate status” in the protocol contains a non-empty,
duplicate-free `required_scenarios` list. It is the machine-readable authority for
which scenario IDs must be covered. The total is derived from this list rather than
maintained as another number. Stable IDs retain their meanings; the decision changes
representation and validation, not the scenario requirements or the two release bars.

The release checker continues to read only that live JSON, never the scenario tables or
historical prose. A class is complete only when every required ID has a qualifying
passing cell under the existing run/ABI rules. An equal-sized collection with a replaced
ID is not complete. Missing results leave incomplete coverage; unexpected result IDs
invalidate the record. Missing, empty or duplicate required-ID registries are invalid.

Documentation validation separately checks that the registry IDs match the scenario
table IDs under “The scenarios” exactly, rejecting duplicate table IDs and set mismatches.
It ignores the populated example fence and dated prose numerals. This is a focused
registry/table relationship check, not a parser of every way a document spells a count.
The local map does not need to enter the documentation check's scan scope because its
current guidance refers to the protocol.

`gate_version` moves from 2 to 3 because the registry is a required schema addition.
The live empty-evidence record and populated example are amended together. Empty
`classes` remains absence of evidence and qualifies for neither release bar.

### Handoff

The registry and policy are specified, but neither validation implementation is added
by this planning resolution. Both are owned by
[Make the release workflow enforce the acceptance gate](36-release-workflow-enforces-the-gate.md),
whose handoff now includes the required-ID rules and meaningful failure cases.
[What granularity does a gate record have?](58-what-granularity-does-a-gate-record-have.md)
still owns build identity within the record and still blocks that implementation.
It must preserve the registry contract and version any further schema change.

ADR-0008 records the amendment; the protocol holds the schema. No additional fog
graduated and no other ticket was resolved in this invocation.
