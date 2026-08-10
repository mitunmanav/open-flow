# Plan: Research drop · dual worktrees · dual skins

**Spec:** `docs/process/specs/2026-08-10-research-drop-dual-skin-design.md`  
**Base:** `main` @ current tip  

## Worktrees

```bash
git worktree add .worktrees/product-m3 -b feat/product-m3
git worktree add .worktrees/product-brutal -b feat/product-brutal
```

## Strategy

1. Implement full stack on **product-m3** (features 1–15 + IA + both skins, default M3).  
2. On **product-brutal**: same commits or merge from m3, default skin = subtle brutal.  
3. Each slice: Android doc check → TDD where pure → implement → test → commit.

## Slice order (product-m3)

| Slice | Android docs | Deliverable |
|-------|--------------|-------------|
| S1 Nav IA | NavigationBar + ModalNavigationDrawer | Bottom Home/Dict/Snips/Style; drawer Settings/History/Customize |
| S2 Theme | M3 theming | `VisualSkin` enum · M3 + SubtleBrutal color/shape |
| S3 Bubble | Overlay / a11y patterns | No text default · shapes · snap · haptic · wave |
| S4 Speech | SpeechRecognizer extras | Cleanup levels · style-by-app |
| S5 History | Share / Room | Export · search · flag · raw/polish · retention · dict star |
| S6 System | Notifications | Optional notif · sounds · lang hints |

## Merge

1. `feat/product-m3` → main when green  
2. `feat/product-brutal` → main or keep as alternate branch / flavor  

## File ownership (S1–S2)

- `AppShell.kt`, `AppRoute.kt`, `MainActivity.kt` (shell parts)  
- `Theme.kt`, `OpenFlowColors.kt`, new `BrutalColors.kt` / `VisualSkin`  
- `FlowPrefs.kt`, `LayoutPrefs.kt`  
