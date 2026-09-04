# Website Funnel Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the 8-page docs site into an install funnel with honest simulated demo, sideload reassurance, FAQ, and full SEO/a11y shell, staying vanilla no-build.

**Architecture:** Same 8 pages + shared CSS/JS, no build step, Pages deploys docs/ as-is. Split site.css into tokens/base/components/motion includes via multiple <link> tags; split site.js into features via multiple <script> tags; shared head/nav/footer injected once per page from JS partials with no-JS <noscript> fallback nav. Copy: rules-cleanup honesty, no "AI"/"Live"/"Watch" claims.

**Tech Stack:** Hand HTML, vanilla CSS/JS only, Google Fonts (Eb Garamond + Figtree) + local woff2 preloads as today.

**Spec:** Approved in chat 2026-09-09 (story / convert / trust / tech sections). No separate spec file — chat approval is the spec.

## Global Constraints

- Amend 25th commit only, no new commits; `git rev-list --count HEAD` stays 25; no push without explicit GO; author Mitun only, no `Co-Authored-By` trailer.
- Keep test-pinned files/keys: `docs/privacy.html` must exist; `docs/COMPARISON.md` keeps "Wispr Flow" + "Keep your keyboard"; `docs/GUIDE.md` keeps "opacity"; `CONTRIBUTING.md` keeps "## Dev vs Launch", "of_win", "release.yml", "No `Co-Authored-By`".
- Motion: transform + opacity only, rAF-throttled scroll, honor `prefers-reduced-motion` in CSS and JS.
- Copy honesty: default = phone system recognizer (often Google); offline = on-device Whisper; "rules cleanup", never "AI"; demos labeled "Simulated preview"; Android-only ("not supported yet" for other platforms).
- Caveman style on site: short lines, easy words, brief bullets.

---

### Task 1: CSS split (tokens / base / components / motion)

**Files:**
- Create: `docs/css/tokens.css`, `docs/css/base.css`, `docs/css/components.css`, `docs/css/motion.css`
- Modify: all 8 `docs/*.html` head blocks (`<link rel="stylesheet" href="site.css">` → 4 links)
- Delete: `docs/site.css` (after split verified)

**Interfaces:**
- Consumes: nothing.
- Produces: 4 stylesheets; every selector from site.css lives in exactly one of them; pages link all 4 in order tokens → base → components → motion.

- [ ] **Step 1: Partition map.** Read `docs/site.css` fully. Assign each rule block: `:root` vars + `@font-face` → tokens; reset/body/type/layout/header/nav/footer/tables → base; hero/demo/marquee/cards/cta/faq/flow-pipe → components; keyframes/transitions/reveal/enter/marquee-anim/reduced-motion → motion. Undefined `--ease-spring` usages → replace with `var(--ease-smooth)` (token exists; spring never defined).
- [ ] **Step 2: Write the 4 files** with exact copied rules, each starting with a 1-line comment (`/* tokens: vars + fonts */` etc.).
- [ ] **Step 3: Swap links in all 8 pages.** Old: `<link rel="stylesheet" href="site.css">`. New: 4 links in order. Verify: `grep -c "site.css" docs/*.html` → 0.
- [ ] **Step 4: Delete site.css, run checks.** Run: `node --check docs/site.js` (untouched, sanity) + landing checks script (12 checks from 2026-09-09 session) + `./gradlew :app:testDebugUnitTest --tests "app.openflow.docs.DocsStaleScanTest"`. Expected: BUILD SUCCESSFUL, tree shows 4 new css + 8 modified html + deleted site.css.

### Task 2: JS split + shared partials (head/nav/footer include)

**Files:**
- Create: `docs/js/core.js` (router + smooth nav + reveal + hero-enter + sticky), `docs/js/demo.js` (dictation sim + speech tabs + app pills + cleanup loop), `docs/js/partials.js` (head-meta/nav/footer injection + marquee a11y)
- Modify: all 8 `docs/*.html` (replace duplicated `<header>`/`<footer>` blocks with `<div data-partial="header|footer">` + noscript fallback; replace `<script src="site.js" defer>` with 3 scripts)
- Delete: `docs/site.js` (after split verified)

**Interfaces:**
- Consumes: Task 1 (CSS paths stable, class names unchanged).
- Produces: `window.OpenFlow` namespace with `initAll()`; partials render identical nav/footer markup incl. `aria-current` per page (page sets `data-page="install"` on `<body>`).

- [ ] **Step 1: Split site.js by feature** into core/demo/partials, exact copied logic, one `initAll()` in core calling all inits. Keep `reducedMotion` guard in each file (duplicate the 2-line matchMedia const).
- [ ] **Step 2: Extract header/footer partials.** Markup identical to today (same links, same `.nav-cta`, same footer). Pages keep `<noscript>` with plain nav links so no-JS still navigates.
- [ ] **Step 3: Swap scripts + partial divs in all 8 pages**, add `data-page` to each body. Verify: `grep -c "site.js" docs/*.html` → 0; open each page: nav/footer render, aria-current correct, router still swaps main+hero.
- [ ] **Step 4: Delete site.js, run checks.** `node --check docs/js/*.js`, 12 landing checks, DocsStaleScanTest. Expected: green.

### Task 3: Landing funnel (story order + honest demo + FAQ + proof strip)

**Files:**
- Modify: `docs/index.html` (main only), `docs/css/components.css` (faq + proof-strip styles), `docs/js/demo.js` (caption text only)

**Interfaces:**
- Consumes: Tasks 1–2 (asset paths).
- Produces: landing section order pain → proof → how → trust → get; FAQ block; version/size/date proof strip under hero.

- [ ] **Step 1: Proof strip under hero CTAs.** One line: `v0.1.9 · ~8 MB APK · updated Sep 2026 · Android 8+` (verify versionName from app/build.gradle.kts before writing; size as "~8 MB" only if release APK in dist/ confirms — else omit size).
- [ ] **Step 2: Honest demo labels.** "Watch in action" → "See how it works"; "Play live demo"/"Live dictation"/"Live polish" → "Simulated preview" + caption `Simulation — real app looks the same, runs on your phone.`
- [ ] **Step 3: FAQ section** (5 items, `<details>`/`<summary>`): sideload safe? why not Play yet? offline cost (model MB)? languages (40+, read count from LanguagePolicy.kt — test pins ≥40, cite "40+")? bank apps? iOS? (fold iOS into sideload answer — 5 items max).
- [ ] **Step 4: Copy scrub.** Remove "AI voice dictation" from title/meta → "rules cleanup"; "Why Flow wins/beats" → "Why Flow fits". Verify: `grep -ri "AI voice\|Live polish\|Live dictation\|Watch in action\|beats built" docs/*.html` → 0.
- [ ] **Step 5: Run checks.** 12 landing checks updated (new strings), full `:app:testDebugUnitTest`. Expected: green.

### Task 4: Conversion (install-first CTAs + install proof + CTA bands + sticky everywhere)

**Files:**
- Modify: all 8 `docs/*.html`, `docs/js/core.js` (sticky selector), `docs/js/partials.js` (sticky markup), `docs/install.html` (proof box)

**Interfaces:**
- Consumes: Tasks 1–3.
- Produces: hero/nav Download → `install.html` (not raw releases URL); install.html proof box (version/size/date/publisher check/Protect steps); shared bottom CTA band on all pages; sticky CTA on all pages.

- [ ] **Step 1: Retarget Download CTAs.** Every hero/nav/sticky `href="https://github.com/mitunmanav/open-flow/releases/latest"` → `href="install.html"`. install.html keeps the 2 external buttons; fix "All releases" → `.../releases` list URL.
- [ ] **Step 2: Install proof box.** Version (from gradle), min SDK 26 → "Android 8+", publisher `github.com/mitunmanav`, Play Protect "Install anyway" 3-step, SHA note (only if a release hash is published — else "compare size on releases page").
- [ ] **Step 3: CTA band partial** in partials.js: `Free for Android 8+. No account. [Install in minutes → install.html]`. Append to every page main bottom (skip install.html — it IS the target; give it a Guide-next band instead).
- [ ] **Step 4: Sticky CTA all pages.** Move sticky markup into header partial; core.js already toggles past hero — guard `if (!hero) show after 400px`.
- [ ] **Step 5: Run checks.** `grep -c "releases/latest" docs/index.html` → 0 (except install.html); full unit tests. Expected: green.

### Task 5: Trust + contributors (freshness lines + nav + compare calm + arch discovery)

**Files:**
- Modify: `docs/js/partials.js` (footer freshness + header nav), 7 subpage mains (freshness line), `docs/compare.html` (title/meta/h1), `docs/roadmap.html` (version/date line)

**Interfaces:**
- Consumes: Tasks 1–2.
- Produces: header nav gains Compare + Built; footer gains `v0.1.9 · updated Sep 2026`; compare title "Flow vs built-in voice typing"; roadmap freshness line.

- [ ] **Step 1: Header nav** add `Compare`, `How it is built` links (keep 6 max: Home, Install, Compare, Built, Guide, Privacy + Download CTA). aria-current per data-page.
- [ ] **Step 2: Freshness line** in footer partial: `v{versionName} · updated {month year}` — single source, all pages inherit.
- [ ] **Step 3: Compare calm-down.** Title/meta/h1 "beats" → "vs"; keep FUTO/Sayboard honest nod + COMPARISON.md keywords ("Wispr Flow", "Keep your keyboard") intact — test-pinned.
- [ ] **Step 4: Run checks.** DocsStaleScanTest + full unit suite. Expected: green.

### Task 6: SEO shell + a11y pass

**Files:**
- Modify: all 8 `docs/*.html` heads (via partials.js head injection where static allows), `docs/robots.txt` (create), `docs/sitemap.xml` (create)

**Interfaces:**
- Consumes: Tasks 1–5.
- Produces: canonical + OG + twitter card on all 8 pages (absolute URLs `https://mitunmanav.github.io/open-flow/`); robots.txt + sitemap.xml; bubble `role=button` div → real `<button>`; logo `alt="Open Flow"`; table `scope` attrs; skip-link on all pages.

- [ ] **Step 1: Head tags per page.** Canonical, og:type/url/site_name/title/description, twitter:card=summary, og:image=icon.svg absolute URL. Titles keep `X — Open Flow`, add "Android dictation app" keyword to landing + compare + install descriptions.
- [ ] **Step 2: robots.txt + sitemap.xml** listing all 8 pages.
- [ ] **Step 3: A11y fixes.** Bubble div → `<button type="button" class="bubble">` (keep classes; demo.js binds `.bubble` — selector unchanged); brand/footer logo alt text; `<th scope>`; skip-link markup into header partial.
- [ ] **Step 4: Final verify.** 12+ checks (old + new strings), `node --check` all js, full `:app:testDebugUnitTest`, `git status --short` shows only intended docs/* paths. Hold commit — wait for GO to amend 25th, no push without second GO.
