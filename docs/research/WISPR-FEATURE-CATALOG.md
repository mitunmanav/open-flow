# Wispr Flow feature catalog (research)

Sources (2025–2026 public pages):
- https://wisprflow.ai/features
- https://wisprflow.ai/whats-new
- https://wisprflow.ai/android
- https://wisprflow.ai/pricing
- https://docs.wisprflow.ai/articles/8858845757-setup-wispr-flow-on-android-android-settings
- https://docs.wisprflow.ai/articles/5096240724-navigating-the-wispr-flow-app-desktop-ios-and-android
- https://docs.wisprflow.ai/articles/5373093536-how-do-i-use-smart-formatting-and-backtrack
- https://docs.wisprflow.ai/articles/6344532666-android-system-requirements
- Play: `com.wispr.flowapp`

## Product shape (Android)

| Piece | What Wispr does |
|-------|-----------------|
| **Surface** | Floating **bubble** over keyboard / focused field — **not** a replacement IME |
| **Insert** | AccessibilityService: detect editable fields, insert text |
| **Overlay** | Draw-over-apps for bubble |
| **Mic** | Runtime mic permission |
| **Nav** | Bottom nav: Home, History, Style, etc. |
| **Session cap** | ~**5 min** per dictation session on Android |

## Dictation core

| Feature | Notes |
|---------|--------|
| Unlimited dictation (Android tier notes) | Marketing; free tiers may cap other platforms |
| Auto punctuation from pauses/tone | Cloud/model |
| Spoken punctuation by name | "comma", "parentheses", full stop, etc. |
| Filler removal | um / uh |
| Self-corrections / Backtrack | "actually", "scratch that", restatement |
| Numbered lists | "1. Apples 2. Bananas" → list |
| Context-aware dictation (Android what's-new) | Reads surrounding field text before STT |
| Clipboard fallback | If insert fails → paste button on bubble |

## Auto Cleanup (was Smart Formatting)

Under **Style** tab; four levels:

| Level | Wispr meaning |
|-------|----------------|
| **None** | Exact speech, mistakes kept |
| **Light** | Fillers + grammar |
| **Medium** | Clarity + conciseness |
| **High** | Brevity + polish rewrite |
| Undo AI edit | History keeps raw; undo AI edit |

**High is AI rewrite** — not pure rules.

## Styles

- Writing styles (formal/casual/etc.)
- Style tab separate from cleanup intensity
- App-category auto style: Wispr moved away / limited (what's-new: styles no longer auto-adjust by app in some release)

## Personalization

| Feature | Notes |
|---------|--------|
| Personal dictionary | Auto-add on correction + manual terms |
| Snippets | Desktop/iOS strong; Android core expanding |
| Languages | 100+ claimed (cloud) |

## Command Mode (Pro / desktop-heavy)

- Highlight text → voice edit instruction ("organize into bullets")
- Cloud LLM editor — **not** local

## Bubble UX (Android docs)

- Tap dictate / long-press hold-to-dictate
- Bubble size steps (0.7x–1.15x)
- Opacity steps (20%–100%)
- Auto-shrink idle / search fields
- Repair when a11y dies (settings prompts)

## Privacy (marketing vs reality)

- Cloud AI processing for cleanup
- Privacy Mode / retention claims on desktop plans
- Android a11y: field detect, insert, keyboard visibility — Play disclosure

## Pricing (snapshot)

- Free + paid Pro (advanced models, Command Mode, higher caps)
- Platform matrix: Mac, Windows, iOS, Android

## Competitors / alternatives (local angle)

| App type | Notes |
|----------|--------|
| Gboard voice | Verbatim-ish, Google stack |
| Yaps / offline AI keyboards | Different surface (IME), still often model |
| Whisper offline apps | File/transcribe, not always bubble-anywhere |

