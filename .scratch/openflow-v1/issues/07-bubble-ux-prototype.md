# Bubble UX prototype

Type: prototype
Status: resolved
Blocked by: none

## Question

How should the bubble look and behave: five states (Idle/Listening/Processing/Done/Problem), tap/hold/drag/cancel gestures, haptics, size/opacity/position settings, and how it avoids stealing focus? Produce a rough interactive prototype or annotated mock.

## Answer

Bubble UX settled:

- Follow Wispr Flow's proven interaction model: small floating bubble; **tap** = start/stop, **hold** = push-to-talk (release stops), **drag** = reposition, **✕ / swipe-to-cancel** while active; separate Done/Problem feedback states.
- **Full shape customizability** is a V1 settings feature: user picks among several shapes (round dot, pill, chip, ring — prototype variants A/B/C as seeds) plus size, opacity, and position memory. Shape choice must not change behavior or state legibility: every shape must still communicate Idle/Listening/Processing/Done/Problem (color + label + optional pulse).
- Live transcript: Wispr-style small adjacent text while listening (partial transcript), toggleable in settings; default on.
- Prototype kept as primary source at `.scratch/openflow-v1/prototype/bubble.html`.
