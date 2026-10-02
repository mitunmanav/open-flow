# Architecture

See `docs/architecture/modules.md` for the full write-up, and `docs/adr/` for the decisions behind it.

```text
Bubble
  ↓
DictationController (state machine)
  ↓
Audio → AdaptiveDictationRouter → SpeechProvider → TranscriptRefiner
  ↓
SafeTextInserter
```

Modules: `app/`, `core/`, `providers/sherpa/`. Dependency direction `app → core ← providers/*`.
