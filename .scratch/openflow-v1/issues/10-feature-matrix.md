# V1 feature matrix

Type: grilling
Status: resolved
Blocked by: 01

## Question

Exactly which Wispr-parity features are in V1 (from the brief's list: filler removal, punctuation, spoken corrections, lists, dictionary, snippets, styles, language choice, history, retry, copy/paste recovery, bubble settings, haptics, offline states, sensitive-field protection) and which are deferred? Define the MVP thin slice vs stretch.

## Answer

V1 feature matrix settled:

- V1 ships the full parity list; no thin-slice cut: bubble (tap/hold/drag/cancel), auto punctuation, filler removal, spoken punctuation, self-correction/backtracking, numbered/bulleted lists, language selection + autodetect per provider, custom dictionary/replacements, snippets, app-category styles, local history (retry/copy/delete/retention), copy/paste recovery, bubble shape/size/opacity/position + haptics, offline/error states, sensitive-field protection, provider selection UI with visible provider/latency/privacy per dictation.
- Settings grouping (flatter than Wispr): Dictation (provider/language/style), Bubble, Dictionary & Snippets, History, Privacy, About.
- Meeting notetaker, monetization, sync, multi-platform, auto-ML routing, local-LLM rewrite stay out of scope (see map Out of scope).
