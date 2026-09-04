# Todo — Overhaul 2026-08-27

Spec: [docs/specs/overhaul-2026-08-27.md](../docs/specs/overhaul-2026-08-27.md)
Companion: [docs/specs/beat-wispr.md](../docs/specs/beat-wispr.md)

Offline-smart work stays in `tasks/todo.md` — do not merge.

## P0 Stabilize ground

- [ ] Clean WIP into logical commits (docs / bubble / home+insights+setup / qa)
- [ ] Authorship gate: Mitun only, no Co-Authored-By
- [ ] `git status` clean after commits; unit tests if env allows

## P1 Floating bubble P0

- [ ] `of_win` visual + `BubbleVisualPainter` / geometry / xml / service
- [ ] Show-text + pulse prefs; no grey veil; ~252x126
- [ ] Visual-capture + Geometry/Painter/prefs tests

## P2 Home history at scale

- [ ] Paging 200 + Load more
- [ ] Debounce search 300ms (`HistorySearchPolicy`)
- [ ] Sticky day headers; footer anti-clip
- [ ] `HistorySearchPolicyTest` + device scroll/search

## P3 Insights upgrade

- [ ] Aggregates in `InsightsAggregatePolicy`
- [ ] Wispr-beat UI tiles/heatmap/by-app + share
- [ ] `InsightsAggregatePolicyTest` + device share smoke

## P4 Setup polish

- [ ] 3-step FirstRun + progress dots
- [ ] OEM battery hint + TalkBack labels
- [ ] Fresh-install / clear-data path check

## P5 Repo organize + docs never stale

- [ ] Expand `DocsStaleScanTest`
- [ ] README clear **dev** vs **launch**
- [ ] Finished todos under `tasks/done/`; offline-smart todo intact

## P6 Automation

- [ ] CI authorship / secret / play-check
- [ ] Gate + visual hooks
- [ ] `tag → release.yml`

## P7 Beat Wispr honest gaps

- [ ] Language catalog grow
- [ ] Spoken-emoji opt-in (cleanup)
- [ ] Latency tune (partials/flush)
- [ ] NO accounts / sync / Notetaker

## P8 Verify + ship

- [ ] Unit + lint + assemble
- [ ] Gate + visual-capture + functional 6/6
- [ ] Push only on GO
