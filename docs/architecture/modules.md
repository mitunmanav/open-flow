# Module layout

V1 ships exactly three real modules — no empty placeholders:

```text
app/                  Compose UI, onboarding, settings, history screens, bubble host
core/                 audio · dictation · stt · refinement · insertion · routing · model
providers/sherpa/     first real STT provider
```

Hard dependency rule:

```text
app ──► core ◄── providers/sherpa
              ▲
              └── future providers/* (only when implemented)
```

- `core` never imports a provider or `app`.
- `providers/*` imports only `core`'s public contract types.
- `app` wires everything with manual constructor injection (no DI framework in V1).

Extension seams:

| Want | Do |
|---|---|
| New STT provider | add `providers/<name>/`, implement `SpeechProvider`, one registry entry in `app/` |
| New feature area | new package under `core/`, wire through `DictationController` |
| New refiner stage | add stage in `docs/architecture/refiner.md`'s pipeline order |

Gradle visibility: `implementation` everywhere; `api` only for types a module must re-export. Tests: core unit tests + FakeProvider pipeline on every PR; provider contract tests run FakeProvider expectations; instrumented tests on a separate workflow.
