# 0005: Module layout and dependency direction

Date: 2026-10-02

## Status

Accepted

## Context

The product must support many future STT providers and features without a restructure. The brief warns against dozens of empty modules, and the reference apps show the cost of hard-wired engines.

## Decision

V1 ships three modules:

- `app/` — Compose UI, onboarding, settings, history screens, bubble overlay host.
- `core/` — `audio/`, `dictation/`, `stt/`, `refinement/`, `insertion/`, `routing/`, `model/`. Contracts and state machines.
- `providers/sherpa/` — the first real provider.

Dependency direction is a hard rule:

```text
app  ──►  core   ◄──   providers/sherpa
           ▲
           └── providers/*  (future, added only when implemented)
```

- `app` depends on `core` and providers.
- `core` never depends on a provider or on `app`.
- Providers depend only on `core`'s public contract types (`SpeechProvider`, `AudioSource` events, capabilities, failures).
- Extension seams documented in `docs/architecture/modules.md`: new provider = new module + one registry entry; new feature = new `core` package + controller wire-in.
- Wiring is manual constructor injection in `app/`; no DI framework in V1.
- Each module exposes a narrow public interface + data types; internals are `internal`. Gradle: `implementation` everywhere, `api` only for re-exported contract types.
- Tests: `core` unit tests (FakeProvider pipeline, transition table); provider modules run the same FakeProvider contract expectations; `app` instrumented tests. CI runs core tests on PR; instrumented suite on a separate workflow.

## Consequences

Adding a provider or feature never restructures existing modules; the bubble/controller/refiner/history layers cannot learn which engine answered.
