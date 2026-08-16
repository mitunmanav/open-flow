# How Open Flow works

This is the user guide. No account. Pick speech language in Settings.

## What it is

Open Flow is an Android app that types what you say.

It is **not a keyboard**. You keep Gboard (or any keyboard you already use).

A small **floating bubble** sits over other apps when you are in a text field. You tap it, speak, tap again. The words go into that field.

## First time

1. Install the APK ([Releases](https://github.com/mitunmanav/open-flow/releases/latest) or Desktop `open-flow.apk`).
2. Open the app. Five short screens explain the product.
3. Turn on **Accessibility** (this is how the bubble can type into other apps).
4. If that switch is grey: App info → menu → **Allow restricted settings**, then try again.
5. Allow the **microphone**.
6. Battery **Unrestricted** if the bubble keeps dying (OEM).

Full install: [INSTALL.md](INSTALL.md) · Phone GO list: [LAUNCH_CHECKLIST.md](LAUNCH_CHECKLIST.md#0-mitun-phone-go-do-this).

## Everyday use

| You do | What happens |
|--------|----------------|
| Tap the bubble | Listening starts |
| Speak | Words appear as you talk (if “speech on bubble” is on) |
| Tap again **or** tap Done | Listening stops. Text is cleaned, then inserted |
| Hold the bubble | Talk only while you hold. Release inserts |
| Tap X while listening | Throw away. Nothing is inserted |
| End with “press enter” | Insert, then send (chat fields) |
| Drag the bubble | Move it |
| Drag to the bottom | Hide for 10 minutes. Shake the phone to bring it back |

After insert, **Copy / Undo / Paste** chips show for a few seconds. Old text: open Open Flow → **History**.

## What the app will not type into

- Password fields
- PIN / bank-style fields
- Many bank and wallet apps (the bubble hides)

Those apps may still warn that Accessibility is on. That warning is theirs. Open Flow cannot turn it off.

## Cleanup

After you stop speaking, Open Flow tidies the words **on the phone** (not a cloud AI model).

| Level | What it does |
|-------|----------------|
| None | Exact speech |
| Light | Drops um/uh, repeats, spoken “period” / “new line” |
| Medium | Light, plus “actually 5:30” style corrections and lists |
| High | Medium, plus shorter / less hedge-y wording |

## Style

Applied **after** cleanup.

| Style | Feel |
|-------|------|
| Formal | Sentences, periods, expands “gonna” → “going to” |
| Casual | Normal sentences |
| Very casual | Chat-like, no forced period |
| Excited | Prefers ! |
| Custom | Your own punctuation, caps, and word swaps |

## Dictionary vs snippet

- **Dictionary** — one word becomes another word (`API` → a longer name).
- **Snippet** — you say an exact short trigger, and a whole block is pasted (an address, an email sign-off).

## History and privacy

- History lives on this phone.
- You can search, copy, and share from History.
- Settings: keep / wipe after 24 hours / never save.
- **INTERNET** is declared for future opt-in (model download / cloud ear); unused by default.
- The **phone’s** speech engine may still use Google. That is not an Open Flow server.

More: [PRIVACY.md](PRIVACY.md).

## If the bubble is gone

Check, in order:

1. Are you in a normal text field (not a password)?
2. Is the keyboard up?
3. Did you snooze it (drag down)?
4. Is Accessibility still on? (Force-stop turns it off.)
5. Is the microphone allowed?

## Settings worth knowing

- **Bubble** — size, shape (pill / circle / square / dot), color, opacity
- **Dictation speed** — Fast / Balanced / Accurate (how long it waits after you stop talking)
- **Haptics** — Off / Light / Full
- **Home layout** — show or hide Home cards; move them up and down
