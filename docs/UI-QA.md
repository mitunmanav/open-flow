# UI-QA — device pass 2026-08-13

**Device:** A059  
**Build:** 0.1.2 (`app.openflow.debug`)  
**When:** 2026-08-13  
**How:** `screencap` + `uiautomator` + `dumpsys meminfo`  
**RAM idle:** TOTAL PSS **183332 KB** (~179 MB)

This log is only what that pass showed. No invented screens.

---

## Captured this turn

| Screen | Result |
|--------|--------|
| Home | Seen |
| Settings | Seen |
| Cleanup | Seen |
| Dict | **BLACK** — screencap fail / screen off |
| History | **BLACK** — screencap fail / screen off |

## Not captured this turn

- Privacy
- Layout editor
- Snippets
- Bubble overlay (live pill on a field)

Do not treat those as QA-passed. No pixels this turn.

---

## Home (seen)

- No first-open guide. Wizard already skipped once a11y + mic were on.
- Cleanup card **clipped**. “Speech on bubble” cut off.
- Bottom nav selected = soft **M3 rounded pill** (not hard square / 2dp border).

## Settings (seen)

- Tab says **Dict**. Body says **Custom Vocabulary**. Same idea, two names.
- Bubble scale / opacity: **no live pill preview** while sliders move.

## Cleanup (seen)

- Four levels shown.
- **Medium** selected.

## Dict / History (this turn)

Extra captures this turn were **black**. Screen off or dump fail. **No visual QA** of those screens from this turn.

---

## Leftover issues (same A059 0.1.2 pass)

From this pass + earlier A059 notes on the same build. Not new screens.

| ID | Seen | Note |
|----|------|------|
| V1 | Home | No first-open how-to |
| V2 | Home nav | M3 pill, not brutal rect |
| V3 | Home | Cleanup card clipped |
| V4 | Settings | Dict vs Custom Vocabulary |
| V6 | Settings | No live bubble preview |
| V10 | meminfo | PSS 183332 KB idle |
| V13 | Copy UX | Unclear. After stop, user does not know to tap check / Copy. No “Copy” label shown. |

Copy path not proven on a good screencap this turn. Still a leftover from the same pass.

---

## Not claimed

- Bubble shape / icon centering / overlay geometry — overlay **not captured**.
- Privacy copy, retention UI — **not captured**.
- Snippets vs Dict on a real screen — Dict dump was black; snippets **not captured**.
- Layout editor — **not captured**.

---

## Recheck next device pass

1. Home: first-open guide + full Cleanup card + brutal nav (no M3 pill).
2. Settings: one Dict name + live bubble preview.
3. Cleanup: still 4 levels; confirm default.
4. Dict + History: screen **on**, then recapture (last dumps were black).
5. Privacy, layout editor, snippets, live bubble overlay — first captures.
6. Copy: user can see **Copy** after stop.
7. Idle RAM under 183332 KB PSS.
