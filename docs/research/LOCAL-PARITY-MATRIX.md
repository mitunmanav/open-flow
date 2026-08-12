# Local FOSS parity matrix (Open Flow constraints)

Constraints: **no cloud AI**, **en-US**, **bubble + a11y**, SpeechRecognizer.

| Wispr feature | Local without AI? | Open Flow approach |
|---------------|-------------------|--------------------|
| Bubble + a11y insert | YES | FlowAccessibilityService |
| Overlay mic bubble | YES | TYPE_ACCESSIBILITY_OVERLAY |
| None/Light cleanup | YES | Rules: fillers, grammar cmds |
| Medium (clarity) | PARTIAL | Course-correct + lists + light clarity |
| High polish rewrite | NO (honest) | Short hedge rules only — label as rules |
| Spoken punct → symbol | YES | Phrase map / tokenizer |
| Backspace last word | YES | Edit commands |
| Backtrack "actually 5:30" | PARTIAL | Structural CourseCorrector |
| Dictionary | YES | Room map on polish |
| Snippets | YES | Expand on polish |
| Styles Formal/Casual | PARTIAL | Caps/end/informal expand |
| Custom style | YES | User from=>to + toggles |
| Command Mode rewrite | NO | Defer forever without model |
| 100+ languages | NO | en-US lock |
| Context-aware field read | PARTIAL | Read field text before append (a11y) |
| Auto punct from pause | PARTIAL | RecognizerIntent formatting extras |
| 5-min session limit | OPTIONAL | Product choice |
| Bubble size/opacity | YES | Prefs already pattern |
| History + undo raw | YES | Store raw + clean |
| Cloud sync | NO | Local only (feature) |

## Pipeline (recommended)

```
SpeechRecognizer (+ formatting extras)
  → Command pass (phrase map)
  → Cleanup level (None/Light/Medium/High-rules)
  → Writing style
  → Dictionary + snippets
  → ACTION_SET_TEXT
```

