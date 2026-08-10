# UI Redesign Drop 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Drawer + Home hub calm-pro UI; Settings Appearance; **bubble shows live partial/final text**.

**Architecture:** Compose `AppShell` + route state; extract screens; pure `BubbleLabelFormatter` for bubble text; fix a11y service labels + layout.

**Tech Stack:** Kotlin, Compose Material3, existing FlowPrefs, AccessibilityService overlay.

**Spec:** `docs/process/specs/2026-08-10-ui-redesign-design.md`

---

### Task 1: BubbleLabelFormatter (TDD)

**Files:**
- Create: `app/src/main/java/app/openflow/bubble/BubbleLabelFormatter.kt`
- Test: `app/src/test/java/app/openflow/bubble/BubbleLabelFormatterTest.kt`

- [ ] Failing tests for partial / final / idle / long wrap soft-cap
- [ ] Implement
- [ ] PASS · commit

### Task 2: Bubble layout + service wire

**Files:**
- Modify: `flow_bubble.xml` (maxLines 3, maxWidth 280dp)
- Modify: `FlowAccessibilityService.kt` use formatter; show final text; don’t clobber partials with onReady only

- [ ] Wire onPartial / onFinal / idle
- [ ] Manual-ready labels
- [ ] commit

### Task 3: AppShell drawer

**Files:**
- Create: `ui/shell/AppShell.kt`, `ui/shell/AppRoute.kt`
- Modify: `MainActivity.kt` — remove bottom nav

### Task 4: Home hub + extract screens

Split Home / History / Dictionary / Snippets / Style / Settings into `ui/*` files; Home = setup, stats, recent, test field.

### Task 5: Settings hub + Appearance

Settings list → Appearance (theme). Placeholders for Drop 2.

### Task 6: Verify

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --offline
cp apk → Desktop
```

Update HANDOFF / BASELINE.
