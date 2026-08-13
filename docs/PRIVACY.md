# Privacy — Open Flow

Last updated: 2026-08-13  
Contact: GitHub Issues on this repository only. Do not send personal data.

## Short

- No account. No ads. No analytics. No trackers.
- No Open Flow server. We do not see your voice or text.
- History stays on this phone unless you share it.
- Default path uses the **phone’s** speech engine + local rules. We do not upload that.
- If **you** turn on a cloud model, that vendor gets what that pick needs. We say which one.

## What we use

**Accessibility**  
Only to see if a text field is focused and to insert the words you just dictated.  
We do not scrape other apps for ads. We hide the bubble in bank / wallet / authenticator apps.

**Microphone**  
Only while you are dictating (you tap the bubble).

**Storage**  
Transcripts you keep, dictionary, snippets, settings, and (if you add one) your API key. App-private. Backup to the cloud is off.

**Internet**  
The app may declare INTERNET so **you** can: download an on-phone model, talk to a laptop URL, or call a key you pasted.  
Unused until you pick one of those. Default = no request from Open Flow.

## Honest about speech (default)

Open Flow does not upload audio on the default path.  
Android’s **SpeechRecognizer** is the phone’s engine. On some phones it may still use Google or the network. That is the phone, not an Open Flow server.

## If you pick a model

You choose the ear (who hears) and the brain (who rewrites).  
We do **not** claim vendors keep your data local. Their rules are theirs.

| Your pick | What can leave this phone | Who gets it |
|-----------|---------------------------|-------------|
| Default (system STT + rules) | Not by us. Phone STT may still send audio. | Phone / Google (device) |
| On-phone model (after download) | Nothing for that listen | Nobody |
| Laptop / your URL | Audio and/or text | **Your** computer |
| OpenAI ear | Voice | OpenAI |
| OpenAI brain | This utterance’s **text** (not your history) | OpenAI |
| Grok brain (xAI) | This utterance’s **text** | xAI — **Grok, not Groq** |
| MiniMax | Voice and/or text, if you picked them | MiniMax |
| DeepSeek | Text, if you picked them as brain | DeepSeek |
| Gemini | Text, if you picked them as brain | Google (Gemini API) |
| Anthropic | Text, if you picked them as brain | Anthropic |
| Sarvam ear / brain | Voice and/or text | Sarvam |
| Deepgram / AssemblyAI ear | Voice | that vendor |
| Custom URL | Whatever that URL is | whoever hosts it |
| OpenRouter / Together / Fireworks / Mistral | Text, if you picked them | that vendor |

**We never send:** dictation history, dictionary, snippets, other apps, your key to *us* (we have no server).

**We try (not perfect):** before a cloud **brain** call, hide email- and phone-shaped bits. The rest of the sentence still goes. We do **not** hide your voice from a cloud **ear**. That cannot be local.

Keys stay on the phone. Uninstall deletes them.

Their own policies (read theirs; we do not copy their promises):

- [OpenAI](https://openai.com/policies/privacy-policy)
- [xAI (Grok)](https://x.ai/legal/privacy-policy)
- [MiniMax](https://www.minimax.io/)
- [DeepSeek](https://www.deepseek.com/)
- [Google Gemini](https://policies.google.com/privacy)
- [Anthropic](https://www.anthropic.com/legal/privacy)
- [Sarvam](https://www.sarvam.ai/)
- [Deepgram](https://deepgram.com/privacy)
- [AssemblyAI](https://www.assemblyai.com/legal/privacy-policy)

## What we do not do

- No account or email.
- No sale of data (we do not have your data on a server).
- No crash reports sent off the device.
- No “anonymous voice” claim.
- No Groq product in this app (if you see **Grok**, that is xAI).

## Your choices

- Stay on default: phone STT + local rules.
- Pick on-phone model if you want audio to stay here.
- Revoke mic in system settings.
- Turn off Accessibility.
- Settings: keep / wipe 24h / never store. Clear a vendor key any time.
- Uninstall to delete local data.

## Children

Not directed at children under 13.

## Banks

We hide our bubble there. The bank may still say Accessibility is risky. That warning is theirs. We cannot remove it.
