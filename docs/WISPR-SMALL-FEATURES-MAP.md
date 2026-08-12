# Wispr Flow small features map → Open Flow (local)

Source: Wispr Help *Smart Formatting & Backtrack* (2026) + Android product notes.  
Open Flow: **local rules only**, en-US focus, **no cloud AI / no LLM rewrite**.

## Architecture (stop / inject polish path)

```
raw STT → dictionary → snippets → CleanupPipeline(level, style, custom) → insert
```

| Stage | Owner | Notes |
|-------|--------|--------|
| Dictionary | Room + [TextPostProcessor.applyDictionary] | Whole-word replace; early |
| Snippets | Room + [TextPostProcessor.expandSnippets] | Exact trigger expand; early |
| Cleanup level | FlowPrefs `cleanup_level` + [CleanupPipeline] | none/light/medium/high — rules only |
| Spoken punct / layout / backspace | [VoiceCommands] inside pipeline | Local word→symbol |
| Course-correct | [CourseCorrector] Medium+ | Marker-based, not AI |
| Writing style | FlowPrefs style + [StyleApplicator] | After levels; Custom knobs in prefs |
| Entry | [TextPostProcessor.polishSessionResult] | stopListening + debug inject |

**Honesty:** style = local caps/punct/expand/replace rules. Not an AI tone model.

## A. Spoken punctuation (say name → symbol)

| Say | Symbol | Open Flow |
|-----|--------|-----------|
| period, full stop | `.` | **ship** |
| comma | `,` | **ship** |
| question mark | `?` | **ship** |
| exclamation point / mark | `!` | **ship** |
| colon | `:` | **ship** |
| semicolon | `;` | **ship** |
| open/close paren(thesis), brackets | `()` | **ship** |
| open/close quote, quotation mark, quote | `"` | **ship** |
| apostrophe, single quote | `'` | **ship** |
| em dash / emdash / dash | `—` / `-` | **ship** |
| ellipsis | `...` | **ship** |
| slash, forward slash | `/` | **ship** |
| backslash | `\` | **ship** |
| underscore | `_` | **ship** |
| hashtag, hash | `#` | **ship** |
| at sign, at symbol | `@` | **ship** |
| asterisk, star | `*` | **ship** |
| ampersand | `&` | **ship** |
| percent sign | `%` | **ship** |
| plus / plus sign | `+` | **ship** |
| minus / negative | `-` | **ship** |
| equals / equals sign | `=` | **ship** |
| tilde | `~` | **ship** |
| degree / degrees celsius / fahrenheit | `°` / `°C` / `°F` | **ship** |
| copyright / trademark / registered | `©` `™` `®` | **ship** |
| less-than / greater-than / angle bracket | `<` `>` | **ship** |

## B. Layout commands

| Say | Action | Open Flow |
|-----|--------|-----------|
| new line, next line, line break | `\n` | **ship** |
| new paragraph, start a new paragraph, skip a line | `\n\n` | **ship** |
| press enter (desktop paste+Enter) | key event | **defer** (a11y insert only) |

## C. Edit / backspace commands

| Say | Action | Open Flow |
|-----|--------|-----------|
| backspace, delete last word, delete word, remove last word | drop last word | **ship** |
| delete last character, backspace character | drop last char | **ship** |
| delete last sentence, remove last sentence | drop last sentence | **ship** |
| clear all, delete all | empty buffer | **ship** |
| scratch that / actually / no (course correct) | CourseCorrector | **have** (Medium+) |

## D. Cleanup + style (wired)

| Feature | Status | Where |
|---------|--------|--------|
| Fillers / levels None–High | **ship** | CleanupPipeline + FlowPrefs `cleanup_level` |
| Course-correct / backtrack | **have** | CourseCorrector (Medium+) |
| Lists 1. 2. / number one | **ship** | CleanupPipeline Medium+ |
| Writing styles (formal/casual/very_casual/excited) | **ship** | WritingStyle / StyleApplicator |
| Custom style (end punct, caps, expand, replacements) | **ship** | FlowPrefs + StyleTab + CustomStyleConfig |
| Dictionary | **ship** | Room + polish early |
| Snippets | **ship** | Room + polish early |
| Sensitive / bank hide | **have** | FieldPolicy / PackagePolicy |

## E. Not local FOSS yet (skip / later)

- Cloud Command Mode (“make more concise”) — **no AI**
- App-category auto style (Slack vs Gmail)
- Press Enter after paste (desktop)
- File tagging in Cursor
- Multilingual Smart Formatting packs
- LLM tone rewrite
