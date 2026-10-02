# Provider proposal

<!--
The single source of truth for provider proposal criteria. `docs/providers/provider-proposal.md`
links here rather than restating the criteria, so that this file and the guide
cannot drift apart.

Read GLOSSARY.md first: SpeechProvider, Provider Adapter, Provider State,
Provider Health, Declared Rate, and Capabilities Honesty all carry precise
meanings, and several of them differ from their everyday senses.

V1 ships two adapters, both written by us — FakeProvider and SherpaOnnxProvider.
There is no plugin system, no SDK artifact, and no compatibility promise
(ADR-0006). A third-party provider arrives as a pull request adding
`providers/<name>/` plus one registry entry, subject to CI and review. That is
the whole mechanism.
-->

## Summary

<!-- What engine, on which devices, in which languages. -->

## Engine and licence

| | |
| --- | --- |
| Upstream project | |
| Licence of the engine | |
| Licence of the **model weights** | |
| Are weights redistributable? | |

<!--
The licence that disqualifies a provider is the model licence, not the code
licence. Engines are usually permissively licensed; their weights frequently are
not, and redistribution is exactly what an Android APK does.

**A provider cannot ship if its model licence forbids redistribution.** If you
cannot state that redistribution is permitted, this proposal is blocked until it
can be.
-->

## Capabilities to declare

<!--
Capabilities are binding. A provider that declares something must do it, and
the Contract Test suite in core's test fixtures enforces that. Declaring a
capability you do not reliably have is worse than omitting it, because app
behaviour is driven entirely by these declarations.

| Capability | Declared | Evidence it works |
| --- | --- | --- |
| Streaming | | |
| Offline | | |
| Languages | | |
| Utterance timestamps | | |
| Partial transcripts | | |
| Confidence scores | | |
| Declared rate (micros USD/sec) | | |

`0` means free — say so if the provider runs on-device. `unknown` is also
acceptable, and is not the same as free: it means the provider cannot estimate.
-->

## Integration shape

<!--
Does it fit `SpeechProvider` as ADR-0001 defines it, or does it need the contract
changed? A change to the contract affects every existing adapter, so "small
change" is rarely small.
-->

| | |
| --- | --- |
| Requires a new `SpeechProvider` method? | |
| Requires a new capability? | |
| Requires a change to ADR-0001? | |

## Health reporting

<!--
`health()` returns `HEALTHY`, `DEGRADED`, `UNAVAILABLE`, or `MODEL_MISSING`,
read from cached local state — no network probe. For each value your adapter can
return, say what condition produces it and what the user is expected to do.
`MODEL_MISSING` means a download prompt; `UNAVAILABLE` means a retry.
-->

## Tests

<!--
The shared Contract Test suite runs against your adapter. Say how you will run it
locally before CI does, and attach the output of a passing run.
-->

## Licensing obligations

<!--
Per ADR-0006, a provider author adds a section to root `THIRD_PARTY_NOTICES.md`,
and any model licence requiring attribution or redistribution has its text
dropped in `providers/<name>/licenses/`.

Which notices will you add, and where?
-->

## Alternatives

<!-- Other engines considered, and why this one. -->