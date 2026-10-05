# What does a snippet longer than one line look like?

Type: prototype
Status: resolved
Blocked by: none

> **Graduated from ticket 37** (`.scratch/openflow-v1/issues/37-dictionary-snippets-styles-surface.md`),
> finding 5. Ticket 37 settled the surface and deliberately left this open, because the answer
> changes what an entry *is* and no amount of looking at the surface settles it.

## Question

The dictionary/snippets surface, as ticket 37 left it, shows every entry as one row: `you say →
it becomes`. That fits `sig → Best regards,` and `tl;dr → To put it briefly:`, and it fits
nothing longer.

But a snippet is not a word. Its whole purpose is often a **block** — a sign-off, an agenda, a
canned paragraph, a set of instructions. A user who wants "when I say `signoff`, type four lines"
currently has nowhere to put four lines, and a user who puts them in anyway gets a row whose
expansion is silently truncated or scrolled off the edge.

Which is it:

- **A clamped row with an expand affordance** — the list stays scannable, the full expansion is one
  tap away. Cheapest, and it keeps every entry comparable at a glance.
- **A separate detail screen per snippet** — the row is a label, the body is an editable text
  field. Most room for the real case; most machinery, and it makes editing a snippet a two-step
  errand for what is a one-line change for most entries.
- **Admit V1 snippets are single-line, and say so on the surface** — refuse a newline in the
  expansion, and tell the user why. The smallest honest answer, and it is a real product
  narrowing that should be recorded as one rather than discovered later.

Three things make this sharper than a layout question:

- **It is the one thing ticket 37's merge lost.** Variant C's live bench was the only view that
  showed an expansion at length; A and B both had the one-line row. The merge was right, and this
  is the cost of it.
- **Dictionary entries almost certainly do not need this.** A misspelling mapped to a correction
  is one word. So the answer may legitimately differ by entry kind, which is more honest than one
  rule for both — and means the surface may need two row shapes rather than one.
- **`refiner.md` does not say.** Stage 7 is "user shorthand→expansion map, user-toggled" and is
  silent on length, on newlines, and on whether an expansion may be multi-line at all. Whatever is
  decided here, that sentence has to change — otherwise the document and the surface disagree.

## Why a prototype, not a question

The choice is between a clamp, a screen, and a refusal, and those three look different enough to
argue about and similar enough to be indistinguishable on paper. Ticket 37's merged surface is
the host: mount the alternatives on it rather than describing them, because the thing being
judged is how a long expansion feels next to a short one.

Reuse ticket 37's design language and its motion rules — the ink that travels, the row that draws
its own perimeter when added, the collapse on delete. **Do not invent a third visual language for
a row shape.**

## Comments

### 2026-10-04 — Alternatives ready for live review

Asset: [Long snippet variants](../prototype/long-snippet-variants.html), mounted on a copy of
[The dictionary, snippets and styles surface](../prototype/dictionary-snippets-styles.html).
Open the HTML directly; the bottom arrows switch `?variant=A`, `B`, and `C`.
Switching variants resets the examples; changes otherwise stay in memory.

- **A — Expandable row:** two-line preview, inline full-body editor, explicit save/cancel.
- **B — Detail screen:** the row opens a separate editor; adding uses that editor too.
- **C — Single-line V1:** an explicit limit and a four-line example that can be tried and is
  refused on save, without flattening or silently truncating its text.

All retain the settled tabs, per-entry app scope, theme controls, addition perimeter sweep,
and deletion collapse. The variants load the repository's local fonts.

Browser verification: A inline edit and B detail add/edit preserve newlines and blank lines;
C refuses the four-line sign-off; variant changes update the URL and wrap; DOM IDs are unique;
390px layout has no content extending past the viewport and reserves space for the bottom bar.

Recommendation for discussion: A, because short entries remain scannable while long entries
can be edited in place. **No owner verdict yet; this ticket remains unresolved.** No refiner
contract or glossary term has been changed.


## Answer

Resolved through live exchange on 2026-10-04: the owner accepted the recommendation,
**A — Expandable row**.

- V1 snippet expansions may be phrases, paragraphs or multiline blocks. Stored text keeps
  authored line breaks and blank lines; a preview never truncates the stored expansion.
- The list shows a two-line preview. An explicit expand/edit action opens the full body
  inline, with Save changes and Cancel. Short snippets use the same edit action.
- Adding a snippet uses a multiline expansion field. Scope remains on the entry, and the
  settled Dictionary and Styles surfaces keep their existing shape.
- Keep the host's design language and motion: travelling tab ink, addition perimeter sweep,
  deletion collapse, and instant feedback when reduced motion is requested.

Why: short entries remain scannable while longer snippets can be edited in place.
The separate detail screen and single-line restriction are rejected for this decision.

Primary-source prototype: branch `prototype/long-snippet-surface`, files
[Long snippet variants](../prototype/long-snippet-variants.html) and
[The dictionary, snippets and styles surface](../prototype/dictionary-snippets-styles.html).
This is a planning decision; production Android implementation is still a handoff.

The Snippet definition and TranscriptRefiner stage 7 now record multiline support.
What stage 8 may change in authored expansions is settled separately in
[How should app-style formatting treat a snippet's authored text?](60-style-authored-snippet-text.md).
