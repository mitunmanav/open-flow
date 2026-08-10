# Open Flow — handoff

**main tip:** Drop 3 STT performance + Wispr-style session insert  
**Desktop APK:** `open-flow-debug.apk` (~17MB)

## Product
Bubble + a11y NOT IME. Local STT. MIT. No account.

## Done
- Drop 1: drawer hub, bubble live text, Appearance
- Drop 2: Home layout modules, Menu item visibility, bubble pulse
- **Drop 3 (this):** faster STT, en-US, course-correct, **no raw dump into field**

## Dictation model (Wispr Android parity, local)
1. Tap bubble → listen  
2. Words show on **bubble only** (not keyboard)  
3. Tap again / PTT release → course-correct + polish **once**  
4. Single SET_TEXT into field (prefix + session)  
5. Clipboard only if insert fails  

## Course correct examples
- “reminder 4:30 actually 5:30” → 5:30 only  
- “Tuesday wait no Friday” → Friday  
- fillers stripped · new line / period voice cmds  

## Settings
Home bottom: Quick settings (lang, style, pulse)  
Also: Appearance · Bubble · Home layout · Menu  

## STT tuning
Silence ~0.9s (was 2.8s) · offline prefer · en-US default

## NEXT
F15 export · F16 recorder · stronger NL correct if needed

## Phone
Install Desktop APK. Focus field → tap 🎙 → speak → tap stop. Check text is polished once.
