# TranscriptRefiner spec

Type: grilling
Status: resolved
Blocked by: 04

## Question

Define the deterministic V1 refinement pipeline: raw STT → normalization → filler handling → spoken punctuation → self-correction/backtracking → list formatting → personal dictionary replacements → app-style formatter. Which spoken commands are supported, and what is the app-category style model?

## Answer

Refiner settled (detail in `docs/architecture/refiner.md`):

- Pipeline order: normalize → spoken commands → backtracking → filler removal → list detection → dictionary → snippets → style formatting (style last).
- V1 command set: full spoken punctuation vocabulary; fillers per-language with toggle; lists via ordinals/bullets; dictionary + snippets maps; style presets Casual/Neutral/Formal per app category.
- Backtracking rule: "scratch that" deletes to last sentence boundary; soft markers documented, not edit triggers.
- Advanced-refiner slot reserved, identity in V1; refiner failure degrades to raw transcript marked unprocessed.
- Language-keyed command/filler sets; unknown language = pass-through.
