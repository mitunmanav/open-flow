# Beat Wispr Flow — gap spec

**Date:** 2026-08-27. Sources: [wisprflow.ai/pricing](https://wisprflow.ai/pricing), [Android Beta FAQ](https://docs.wisprflow.ai/articles/2855566159-wispr-flow-for-android-beta-faq), [Android setup](https://docs.wisprflow.ai/articles/8858845757-setup-wispr-flow-on-android-android-settings), [hands-free](https://docs.wisprflow.ai/articles/6391241694-use-flow-hands-free), [Play listing](https://play.google.com/store/apps/details?id=com.wispr.flowapp).

Not marketing. Where they win, we say so.

## Where Open Flow already wins

| Axis | Wispr Flow | Open Flow |
|---|---|---|
| Network | Cloud-only — "Does Flow work offline? No." | Offline first: system STT + whisper.cpp on-device ear; cloud optional BYOK |
| Cost | Free tier limits; Pro $12–15/user/mo | MIT, free, no account |
| Session cap | 5 min hard cap on Android, auto-submits | No cap |
| History | Synced to Wispr servers | Local only, wipe anytime |
| Insights | Basic usage dashboards (admin-only for teams); counts synced to cloud | Local tiles: WPM, streak, heatmap, per-app breakdown, dayparts, best day |
| Opacity default | 80% default (ghost over dark apps) | 100% solid default (0.8 legacy migrates up) |
| Search | Their transcript list | Local FTS across all history + Home feed |

## Where Wispr wins today (honest)

1. **Languages** — they claim 100+; we ship a 12-entry catalog.
2. **Emoji dictation** — "smile emoji" → 😄. We don't map spoken emoji.
3. **Streaming latency** — real-time chunked cloud STT. Our system ear does partials; cloud ear streams over WS (comparable), on-device whisper is slower.
4. **Personal dictionary auto-learn** — they learn names as you go by default. Our auto-learn exists but defaults off (privacy-first choice — keep).
5. **Cross-device sync + Notetaker** — different product category; we don't chase cloud sync on purpose.

## Rules for closing gaps (do / don't)

- **DO** grow the language catalog behind `LanguagePolicy.SUPPORTED_LANGUAGES` — pure data + test.
- **DO** add spoken-emoji mapping to `CleanupPipeline` as an opt-in level, unit-tested golden corpus.
- **DON'T** add accounts, cloud sync, or telemetry. That is our moat, not their feature list.
- **DON'T** chase Notetaker/meetings — Android dictation focus.
- Latency: prefer tuning partials + flush timeout (`SttTuning`) over new network paths.

## Verify claims before shipping

- Any "beats Wispr" copy in-app or store listing must link `docs/COMPARISON.md` honesty rule: no absolute accuracy claims without a same-device bake-off.
