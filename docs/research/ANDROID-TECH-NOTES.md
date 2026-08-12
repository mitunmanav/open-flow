# Android tech notes (dictation bubble)

## AccessibilityService
- Detect focus / window changes for editable fields
- ACTION_SET_TEXT / ACTION_PASTE for insert
- TYPE_ACCESSIBILITY_OVERLAY for bubble window
- OEM: force-stop can drop enabled service — user re-toggle or rebind

## Overlay
- SYSTEM_ALERT_WINDOW / a11y overlay path
- Touchable region = bubble only

## SpeechRecognizer
- EXTRA_LANGUAGE / en-US lock
- EXTRA_PREFER_OFFLINE when on-device available
- EXTRA_ENABLE_FORMATTING (API 33+): latency vs quality tradeoff
- EXTRA_PARTIAL_RESULTS for live bubble text
- Mic exclusive: don't MediaRecorder + STT together

## Testing truth
| Gate | Check |
|------|--------|
| G0 | dumpsys accessibility Bound label |
| G1 | Overlay window present |
| G2 | Inject or speak → field text |
| G3 | Cleanup level changes output |
| G4 | Style changes presentation only |

UI dumps of MainActivity alone ≠ product proof.

## android CLI
- `android layout` / `android screen capture` for UI
- `android docs search` for API truth
- `android install` / `android run` for deploy

