# OpenFlow

Open-source Android voice dictation that floats over your existing keyboard.

Tap or hold the bubble, speak naturally, and OpenFlow inserts polished text where your cursor already is — offline by default, with no mandatory account.

> Status: pre-alpha. Architecture decisions are being locked; see `docs/adr/`.

## What is this?

A floating bubble (not a keyboard, not an IME) that captures speech anywhere on Android, cleans it up, and inserts it into the focused field.

## Why is it different?

The intelligence underneath is replaceable, measurable, and user-controlled. A provider-neutral `SpeechProvider` contract and the **Adaptive Dictation Router** decide which engine handles each dictation (local, offline, cost-capped), and every dictation shows which provider answered, how long it took, and whether anything left the device.

## Documentation

| Doc | What's in it |
| --- | --- |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Module layout, seams, diagrams |
| [docs/adr/](docs/adr/) | Architecture decision records |
| [docs/architecture/](docs/architecture/) | Provider contract, refiner, modules, reference-app teardown |
| [docs/providers/](docs/providers/) | sherpa-onnx capabilities and model selection |
| [docs/privacy/](docs/privacy/) | Privacy policy and Android permission/Play policy |
| [GLOSSARY.md](GLOSSARY.md) | Shared vocabulary |

## License

Apache-2.0. See [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
