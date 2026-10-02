# Contributing

1. Open an issue before large work.
2. Keep the dependency rule: `app → core ← providers/*` (`docs/adr/0005-module-layout.md`).
3. New providers implement the `SpeechProvider` contract (`docs/adr/0001-speech-provider-contract.md`) and register in one place.
4. `./gradlew lint test assembleDebug` must pass; provider changes need contract tests.
5. Never commit keystores, keys, or provider API secrets.
