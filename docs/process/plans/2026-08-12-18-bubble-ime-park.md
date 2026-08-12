# F18 — Bubble IME park + listen size + prefix merge

> Agent: TDD. Caveman. DID / PASS-FAIL / NEXT. Worktree `feat/18-bubble-ime-park`.

**Goal:** Bubble sits **above** keyboard. Listen bar is a small chrome row, not a full-screen veil. Session insert does not double the prefix.

**Not this slice:** STT bias strings, on-device LLM.

**Architecture:** Pure geometry + FieldPolicy first. Service only applies numbers. Gravity stays `BOTTOM|END` (`y` = offset from bottom).

**Stack:** Kotlin, AccessibilityService overlay, unit tests. No new perms. No INTERNET.

---

## Device proof (2026-08-12)

- Idle orb frame `[79,1931][215,2067]` on Z/X keys. `y≈325` < IME height.
- Listen: `Requested w=840 h=2392` — pulse ring `match_parent` inflates overlay to screen.
- Inject High: `Does` + `Does naren…` → `Does Does naren…`

## Files

- `bubble/BubbleGeometry.kt` + `BubbleGeometryTest.kt` — `parkYAboveIme`
- `bubble/FieldPolicy.kt` + `FieldPolicyTest.kt` — overlap merge
- `res/layout/flow_bubble.xml` — pulse wrap_content
- `bubble/FlowAccessibilityService.kt` — IME height from `TYPE_INPUT_METHOD` bounds; apply park; do not save parked `y`

## TDD

1. RED: `parkYAboveIme(325, ime=800, gap=24) == 824`; ime 0 keeps 325
2. GREEN: implement
3. RED: `mergeSession("Does", "Does naren know…")` == piece, no double
4. GREEN: if piece starts with prefix (ignore case) return piece
5. XML + service wire
6. `./gradlew :app:testDebugUnitTest :app:assembleDebug`
7. Device: field + keyboard → orb above keys; tap listen → small bar, no dimmer; inject twice no `Does Does`

## Security

Overlay only. No new permission. No network.

## Mitun test

1. Practice field → keyboard up
2. Square orb above Gboard, not on Z
3. Tap → Cancel | status | Done, no full-screen grey
4. Speak or inject → text once
