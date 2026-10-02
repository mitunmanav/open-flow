# TranscriptRefiner

Provider-independent cleanup of raw STT text. Deterministic in V1 — no LLM required. The advanced-refiner slot in the pipeline is reserved (identity in V1) for a future local-LLM refiner without changing the pipeline shape.

## Pipeline order

1. Normalization: whitespace, casing baseline.
2. Spoken-command expansion: spoken punctuation → symbols, new lines/paragraphs, quotes/parens.
3. Backtracking resolution: "scratch that <X>" deletes back to the last sentence boundary; "no", "I mean", "actually" are soft markers only (the following clause does not replace the prior one in V1).
4. Filler removal: "um", "uh", discourse markers like "you know"/"like" — per-language filler lists, user toggle.
5. List detection: ordinals ("number one", "first") → numbered list; line-leading "dash"/"bullet" → bulleted list. Plain-text `1.` / `-`.
6. Dictionary replacements: user wrong→right map, whole phrase, case-insensitive.
7. Snippet expansion: user shorthand→expansion map, user-toggled.
8. App-style formatting: one of `Casual` / `Neutral` / `Formal` presets (capitalization + ending punctuation rules), assigned per app category.

Style formatting runs last so it sees final content.

## Language behaviour

Command sets and filler lists are per-language, keyed by provider-reported language. V1 ships English + a small set; unknown language → pass-through with dictionary/snippets/style only.

## Failure

A refiner exception degrades to the raw transcript marked unprocessed (ADR-0002); the dictation is not failed. An "advanced refiner" failure never fails insertion.
