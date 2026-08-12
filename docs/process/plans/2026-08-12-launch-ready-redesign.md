# Launch-Ready UI & Bubble Redesign — Open Flow

**Goal:** Complete, premium redesign of Open Flow UI, Branding, and Floating Bubble shapes/states from A to Z for launch readiness.

## 1. Floating Bubble Overhaul (`flow_bubble.xml`, `FlowAccessibilityService.kt`, `BubbleGeometry.kt`)
- **Real Dynamic Shapes**:
  - **Pill**: Floating pill (`wrap_content` x 48dp, corner radius 24dp), vector mic icon + typography label / timer / waveform bars.
  - **Circle / Orb**: Perfect 56dp x 56dp circular floating sphere (corner radius 28dp), centered vector mic with glowing recording ring.
  - **Squircle**: 52dp x 52dp rounded superellipse (corner radius 16dp), modern OneUI/iOS glass aesthetic.
  - **Dot**: Compact 38dp x 38dp jewel indicator for minimal distraction on edge.
- **Visual Design**:
  - Dark glass surface (`#18181B` / `#09090B`) with subtle 1dp border (`#27272A`) and soft elevation shadow.
  - Idle state: Crisp illuminated microphone icon (`#A1A1AA` / `#FFFFFF`).
  - Active Listening state: Electric Indigo / Emerald recording glow (`#6366F1` / `#10B981`), pulsing scale based on live RMS speech volume.
  - Real dynamic shape morphing via programmatically generated `GradientDrawable` based on `prefs.bubbleShape`.
  - Proper click and touch feedback without glitching.

## 2. Design System & Theme Polish (`OpenFlowColors.kt`, `Theme.kt`, `OpenChip.kt`, `OpenCard.kt`, `OpenButton.kt`)
- **Palette**: Refined Electric Indigo (`#6366F1`), Deep Slate backgrounds (`#09090B` / `#F8FAFC`), crisp high-contrast text, subtle translucent borders.
- **OpenChip**: Eliminate jarring red/green toggles. Use modern M3 tonal chips (subtle surface variant when unselected, rich primary container with contrasting text when selected).
- **OpenCard**: Glass / modern elevated cards with subtle 1dp border outline, refined 16-20dp corners, consistent padding.
- **OpenButton**: High-touch target, smooth haptic feedback, primary gradient/solid, outlined, and ghost variants.
- **Typography & Motion**: Clear typography hierarchy, smooth crossfade transitions.

## 3. Screen Experience Redesign (`MainActivity.kt`, `AppShell.kt`)
- **Home Hub**:
  - Brand Header with Open Flow logo mark, live readiness badge (Bubble & Mic status).
  - Practice Field: Elevated interactive dictation playground with word/char counter and 1-tap copy/clear.
  - Quick Tune: Segmented controls for Cleanup level, Writing style, and quick Bubble launcher.
  - Recent Flows: Elegant cards with word count chips, audio duration, timestamps, 1-tap copy & share.
- **History Screen**:
  - Interactive search bar to search previous transcripts.
  - Raw vs Clean toggle on each card.
  - Bulk Export (Markdown, Text) + per-item Share sheet.
- **Dictionary & Snippets Screens**:
  - Beautiful cards, empty states with illustrations, clean input dialogues/cards.
- **Settings Screen**:
  - Visual Bubble Customizer: Live shape selector (Pill, Circle, Squircle, Dot), interactive scale & opacity sliders with live feedback, edge-snap & haptic switches.
  - AI & Cleanup rules: Raw, Light, Normal, High polish preview.
  - Theme & Appearance: Dark / Light / System switcher.

## 4. Verification & Launch Assets
- Unit tests green (`./gradlew test`).
- Android lint 0 errors (`./gradlew lintDebug`).
- Build APKs (`./gradlew assembleDebug assembleRelease`).
- Copy fresh APK to Desktop locations.
