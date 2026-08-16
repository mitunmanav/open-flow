# Open Flow 0.1.7

**Release date:** 2026-08-16  
**License:** [MIT](../LICENSE)  
**Channel:** GitHub Sideload (debug-signed APK)  
**Version:** `0.1.7` (versionCode `8`)  
**Package:** `app.openflow`  
**Test status:** **793 PASS / 0 fail / 0 err / 0 skip** across 112 XML files.

---

## What’s New in 0.1.7

### 1. Universal App Detection Engine (`AppContextEngine`)
- Automatically detects active app context across **6 categories**:
  - **Chat/Messaging:** WhatsApp, Telegram, Signal, Discord, SMS → Natural, conversational styling.
  - **Email:** Gmail, Outlook, Yahoo Mail, K-9, ProtonMail → Clean paragraphing, formal grammar, greetings & sign-offs.
  - **Work & Collaboration:** Slack, Teams, Jira, Linear, Asana → Crisp action items & bullet lists.
  - **Docs & Notes:** Notion, Obsidian, Google Docs, Keep, Notes → Structured headers & formatting.
  - **Developer Tools & Terminal:** Termux, GitHub, VS Code → Verbatim code syntax, camelCase/snake_case preservation.
  - **Search & AI:** ChatGPT, Perplexity, Claude, Chrome, Firefox → Concise, query-oriented format without pleasantries.

### 2. Local Rule-Based Command Mode
- 100% offline semantic voice editing without requiring cloud API keys:
  - Bulleted lists (`"make bullets"`, `"bullet points"`) → `• Item 1 \n • Item 2`
  - Numbered lists (`"numbered list"`, `"number this"`) → `1. Item 1 \n 2. Item 2`
  - Case formatting (`"all caps"`, `"lowercase"`, `"title case"`)
  - Formatting & Quotes (`"add quotes"`, `"camel case"`, `"snake case"`)

### 3. Audio File Storage & Playback Controller
- Local `.m4a` audio memo recording stored app-privately in `context.filesDir/audio/`.
- Integrated audio player controller in History screen with Play / Pause / Seek / Duration display.
- Inherits strict user retention rules (`keep`, `wipe_24h`, `never_store`).

### 4. Android Quick Settings Tile (`FlowBubbleTileService`)
- 1-tap notification shade toggle to enable, disable, or setup the floating Flow Bubble.

### 5. Multi-Language Speech Engine (`LanguagePolicy`)
- Global BCP-47 language support (English US/UK/IN, Spanish, French, German, Hindi, Portuguese, Japanese, Chinese, Italian).

### 6. Zero-Hallucination AI Guard
- Hardened AI Brain response sanitizer with chatter prefix stripping, length-ratio validation, and instant fallback protection.

---

## Verification
- `./gradlew :app:testDebugUnitTest`: **793 PASS**
- `./gradlew :app:assembleDebug`: `app-debug.apk` (~17.7 MB)
