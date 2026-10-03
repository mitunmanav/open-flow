# The project site

Type: grilling
Status: resolved
Blocked by: none

## Question

The destination includes a working GitHub Pages site, and ticket 15 settled `pages.yml` (deploy `website/` on push to `main`). But `website/` did not exist, and nothing decided what it would contain or what would generate it.

**The decision first: docs site or product page?** At the time this was written the answer looked like "docs site, obviously" — twenty-odd Markdown files that render perfectly on GitHub, where the ADRs are diffable and searchable, and where adding a generator introduces a second description of the doc set. That staleness risk is real: this repo has been bitten by it twice, in ticket 21's unwritten guides and in the empty `CHANGELOG.md` that `docs-check` caught on its first run.

But the framing was wrong. The site's job is not to make the documentation easier to read — GitHub already does that, better, with blame and `?plain=1`. The site's job is to explain a product to someone who has never opened this repository. Those are different artifacts, and the second one cannot be a documentation mirror.

If product page, then: how many pages, what generates it, what does it say, and what is the one action it asks for.

## Answer

**It is a product page. The documentation stays on GitHub and the site links to it.** Canonical term: **the project site** — the four pages published at the repository's Pages URL, whose job is to explain what OpenFlow is and ask for one action. It never re-renders a document; each section names the one it came from and links to it, so the two cannot drift.

**Four pages, hand-written, no generator, no build step.** Home, How it works, Privacy, Get involved. GitHub Pages serves `website/` exactly as committed, and a build step would mean `package.json` in a repository that has no application code — the premature-module mistake twice over. The alternative to a generator is duplicated chrome, so that duplication is made mechanical instead: `docs-check` now fails if the four pages disagree about which pages exist, in either the header nav or the footer list, or if a relative `href`, `src` or CSS `url()` does not resolve. Both rules were verified to fire before they were trusted; a gate nobody has seen fail is worse than no gate.

**Content comes only from documents that exist.** The home page's signature is the claim-against-limit list, generalised from ticket 19's permission screen: the promise in the UI face, the limit in mono beside it. The routing decision log, the state machine, the eight refiner stages and the four Android permissions all appear as themselves rather than as paraphrases, with a source note at the foot of each band rather than a label above each heading. **No benchmark numbers appear on the site at all, because ticket 25 has not reported one.** Inventing a figure would have violated the page's own argument.

**The one action is starring the repository.** There is no APK, no `gradlew` and no signing config, so a download button could only lie. Pre-alpha is stated in the hero rather than badged in a corner.

**Typography was decided here because it had never been decided.** Ticket 19's prototype used Archivo and JetBrains Mono; no document recorded it, so the app and the site would have drifted apart. Settled: Archivo at weight 800 and **width 125%** for display against normal-width body — the width axis is the idea, not a size scale — and JetBrains Mono for machine voice only. Both committed as variable woff2, latin subset, ~127 KB. Self-hosted rather than loaded from Google Fonts, because a privacy-first project's landing page must not send a visitor's IP to a font CDN; the OFL is vendored at `website/fonts/OFL.txt` and recorded in `THIRD_PARTY_NOTICES.md`.

**The hero is the app running.** Not a screenshot: a working dictation you can hold to talk, drag to move and throw, showing ADR-0002's states and the refiner's real stages as the transcript is cleaned. The bubble is physically correct — grab offset, 1:1 tracking under pointer capture, measured release velocity handed to a critically damped spring, momentum projection rather than snapping to a bound, rubber-banding at the edges, and interruptibility so a moving bubble can always be grabbed. Escape cancels while recording and is ignored afterwards, because ADR-0002 makes `Inserting` non-cancellable.

**Motion has one rule.** There is exactly one choreographed moment, the page-load entrance. Everything after it is the same gesture — structure draws itself: a rule scales out from a left edge, a marker grows down, a wire draws along its own path. No scroll-triggered fades-and-slides, because that is the most recognisable generated-page tell and it would have undone the rest. The narrative device is `position: sticky`: the phone pins while the six claims scroll past, and its caption names whichever claim is in the reading position, so you read each limit while looking at the thing it is about.

**Two bugs worth remembering.** The first entrance animation set `opacity: 0` as a base state, so a blocked or failed script left the whole hero blank; start states now live behind a `js` class set synchronously in `<head>`, and animations use `fill-mode: backwards` so one that never runs leaves its element visible rather than absent. And `pages.yml` was missing `actions/configure-pages`, which is what enables Pages and authenticates the deployment — without it `deploy-pages` fails with a 404, so the site would have been built on every push and never published. Pages was not enabled on the repository either. Both fixed, and the probe that skipped the deploy when `website/` was absent is deleted: a conditional that can never be false is a stale promise.

Nothing graduated from the fog. `docs-check` remains green with no baseline exemptions, and now governs the site as well as the documents.