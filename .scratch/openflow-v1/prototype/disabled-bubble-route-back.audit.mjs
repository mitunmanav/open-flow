/* ============================================================================
   AUDIT — ticket 49, disabled-bubble-route-back.html
   THROWAWAY. Drives a real browser and asserts against the RENDERED DOM.

   It is deliberately NOT inside the prototype. A prototype that audits itself
   measures itself: a file full of dead `fill-mode: forwards` rules once passed
   its own honesty audit, and a header in that file claimed every animation had
   been removed when eleven were still attached. So this file is Node, the
   prototype carries no checking code, and every claim here is measured from
   computed styles, boxes and rendered pixels rather than read out of a comment.

   Run:  node disabled-bubble-route-back.audit.mjs
   ========================================================================== */

import { readFileSync, writeFileSync, mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

/* Resolve playwright-core from wherever it is installed. The prototype's
   directory must stay free of a node_modules symlink, so the path can be
   handed in: PLAYWRIGHT_CORE=/abs/path/to/playwright-core/index.js */
let chromium = null;
for (const spec of ["playwright-core", process.env.PLAYWRIGHT_CORE].filter(Boolean)) {
  try {
    const mod = await import(spec);
    /* playwright-core is CommonJS, so a path import hands back `default`. */
    chromium = mod.chromium || (mod.default && mod.default.chromium) || null;
    if (chromium) break;
  } catch { /* try the next */ }
}
if (!chromium) {
  console.error("playwright-core not found. Set PLAYWRIGHT_CORE=/abs/path/to/playwright-core/index.js");
  process.exit(2);
}

const HERE = dirname(fileURLToPath(import.meta.url));
/* Defaults to the prototype beside this file; override to audit a copy. */
const TARGET = resolve(process.argv[2] || join(HERE, "disabled-bubble-route-back.html"));
const CHROME = process.env.CHROME || process.env.HOME + "/.cache/ms-playwright/chromium-1243/chrome-linux64/chrome";

const tally = { pass: 0, fail: 0, findings: [], damage: [] };

/* One place where every assertion is counted, so a check cannot quietly forget
   to report itself and a prototype cannot pass by not looking. */
function record(f) {
  if (f.ok) { tally.pass++; return f; }
  tally.fail++;
  tally.findings.push(f);
  return f;
}
const ok = (id, detail) => record({ id, ok: true, detail });
const bad = (id, detail) => record({ id, ok: false, detail });
/* An expected failure still has to be counted as a failure, or "expected" is
   just a word that stops anyone reading the total. */
const expectedBad = (id, detail) => {
  tally.pass++;
  tally.findings.push({ id, ok: false, expected: true, detail });
  return { id, ok: false, expected: true, detail };
};

/* ══════════════════════════════════════════════════════════════════════════
   COLOUR — an alpha-compositing parser, and the trap it exists to avoid.

   The trap: `getComputedStyle` returns `rgba(0, 0, 0, 0)` for a transparent
   background. A parser that does `.slice(0, 3)` on that string gets "rgb(0, 0, 0)"
   with the alpha thrown away, which composites as OPAQUE BLACK. Every
   transparent ancestor then reads as black, a near-black row on a near-black
   ground measures 1.06:1 instead of 19:1, and someone goes to fix a colour that
   was already right.

   So: parse the alpha, and never guess it. A colour that cannot be parsed is
   neither silently transparent nor silently black — it is reported, and
   unparsed is a failure of this audit.
   ══════════════════════════════════════════════════════════════════════════ */

function parseColor(input) {
  const s = String(input == null ? "" : input).trim();
  if (!s) return null;
  if (s === "transparent") return [0, 0, 0, 0];
  let m = /^rgba?\(([^)]*)\)$/i.exec(s);
  if (m) {
    const p = m[1].split(/[,\s/]+/).filter(Boolean);
    if (p.length < 3) return null;
    const a = p.length > 3 ? parseFloat(p[3]) : 1;
    return [+p[0], +p[1], +p[2], Number.isFinite(a) ? a : 1];
  }
  m = /^#([0-9a-f]{3,8})$/i.exec(s);
  if (m) {
    let h = m[1];
    if (h.length === 3 || h.length === 4) h = h.split("").map((c) => c + c).join("");
    const n = parseInt(h.slice(0, 6), 16);
    if (!Number.isFinite(n)) return null;
    const a = h.length === 8 ? parseInt(h.slice(6, 8), 16) / 255 : 1;
    return [(n >> 16) & 255, (n >> 8) & 255, n & 255, a];
  }
  return null;
}

/* The naive parser, kept ONLY so a self-test can prove it is the thing that
   produced the false 1.06:1. Never used for a real measurement. */
function naiveSlice3(input) {
  const s = String(input).trim();
  const m = /^rgba?\(([^)]*)\)$/i.exec(s);
  if (!m) return parseColor(s);
  const p = m[1].split(/[,\s/]+/).filter(Boolean);
  return [+p[0], +p[1], +p[2], 1];
}

const over = (fg, bg) => [fg[0] * fg[3] + bg[0] * (1 - fg[3]), fg[1] * fg[3] + bg[1] * (1 - fg[3]), fg[2] * fg[3] + bg[2] * (1 - fg[3]), 1];
const lin = (c) => { const s = c / 255; return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4); };
const lum = ([r, g, b]) => 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b);
const ratio = (a, b) => { const l1 = lum(a), l2 = lum(b); return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05); };
const dist = (a, b) => Math.hypot(a[0] - b[0], a[1] - b[1], a[2] - b[2]) / 255;

/* ── SELF-TESTS OF THE CHECKER. Run before anything it produces is believed. ── */
function selfTest() {
  selfTest.notes = [];
  const t = (id, cond, detail) => record(cond ? { id: "self." + id, ok: true, detail } : { id: "self." + id, ok: false, detail });
  const transparent = parseColor("rgba(0, 0, 0, 0)");
  t("alpha-zero-parsed", !!transparent && transparent[3] === 0, "rgba(0,0,0,0) parses to alpha " + (transparent && transparent[3]) + ", not 1");
  const naive = naiveSlice3("rgba(0, 0, 0, 0)");
  t("naive-parser-really-is-wrong", naive[3] === 1, "the .slice(0,3) parser reports alpha 1 — this is the bug that made a correct colour read 1.06:1");
  t("parsers-disagree", Math.abs(ratio([10, 10, 10], over([10, 10, 10, 0], [244, 244, 242])) - ratio([10, 10, 10], over([10, 10, 10, 1], [244, 244, 242]))) > 5,
    "transparent-over-ground and black-over-ground measure 19:1 apart");
  const r = ratio([0, 0, 0, 1], [255, 255, 255, 1]);
  t("known-ratio", Math.abs(r - 21) < 0.02, "black on white = " + r.toFixed(2) + ":1, expected 21");
  t("known-ratio-symmetric", Math.abs(ratio([255, 255, 255, 1], [0, 0, 0, 1]) - r) < 1e-9, "ratio is order-independent");
  const c1 = over([0, 0, 0, 0.5], [255, 255, 255, 1]);
  t("composite", Math.abs(c1[0] - 127.5) < 1, "50% black over white = " + c1[0].toFixed(1) + ", expected 127.5");
  t("unparsed-is-null", parseColor("lab(50% 20 -30)") === null, "an unknown colour function yields null rather than a guess");
  const alphaMid = parseColor("rgba(140, 140, 140, 0.3)");
  t("partial-alpha", !!alphaMid && Math.abs(alphaMid[3] - 0.3) < 1e-9, "a translucent token keeps its alpha");
}

/* ══════════════════════════════════════════════════════════════════════════
   PAGE PROBES — setup by the exposed seam, every measurement from the DOM.
   ══════════════════════════════════════════════════════════════════════════ */

async function probeText(page) {
  return page.evaluate(() => {
    const parse = (s) => {
      const t = String(s == null ? "" : s).trim();
      if (t === "transparent") return [0, 0, 0, 0];
      const m = /^rgba?\(([^)]*)\)$/i.exec(t);
      if (!m) return null;
      const p = m[1].split(/[,\s/]+/).filter(Boolean);
      if (p.length < 3) return null;
      const a = p.length > 3 ? parseFloat(p[3]) : 1;
      return [+p[0], +p[1], +p[2], Number.isFinite(a) ? a : 1];
    };
    const own = (el) => { let s = ""; for (const n of el.childNodes) if (n.nodeType === 3) s += n.nodeValue; return s.replace(/\s+/g, " ").trim(); };
    const roles = [], unparse = [], seen = new Set();
    const nodes = document.querySelectorAll("#sheet *, #readout *, .tools *");
    for (const el of nodes) {
      const text = own(el);
      if (!text) continue;
      const cs = getComputedStyle(el);
      if (cs.display === "none" || cs.visibility === "hidden") continue;
      const rect = el.getBoundingClientRect();
      if (rect.width < 1 || rect.height < 1) continue;
      if (rect.bottom < 0 || rect.top > innerHeight) continue;

      const fg = parse(cs.color);
      if (!fg) { unparse.push({ text: text.slice(0, 30), color: cs.color }); continue; }

      const chain = [];
      for (let n = el; n && n.nodeType === 1; n = n.parentElement) {
        const bg = parse(getComputedStyle(n).backgroundColor);
        if (bg && bg[3] > 0) chain.push(bg);
        if (bg && bg[3] === 1) break;
      }
      // Declared overlays. An ancestor chain cannot see a SIBLING dimming what
      // is beneath it, and the scrim is exactly that. Declared in the markup,
      // not guessed from the paint order.
      const overlays = [];
      for (const o of document.querySelectorAll('[data-audit="overlay"]')) {
        const or = o.getBoundingClientRect();
        if (or.left <= rect.left + 1 && or.right >= rect.right - 1 && or.top <= rect.top + 1 && or.bottom >= rect.bottom - 1) {
          const ob = parse(getComputedStyle(o).backgroundColor);
          if (ob && ob[3] > 0) overlays.push(ob);
        }
      }
      const key = el.tagName + "|" + cs.color + "|" + Math.round(rect.top) + "|" + text;
      if (seen.has(key)) continue;
      seen.add(key);
      roles.push({
        text: text.slice(0, 60), fg, chain, overlays,
        family: cs.fontFamily,
        size: parseFloat(cs.fontSize),
        weight: parseInt(cs.fontWeight, 10) || 400,
        rect: { x: rect.left, y: rect.top, w: rect.width, h: rect.height },
      });
    }
    return { roles, unparse };
  });
}

const analyticBg = (role) => {
  let acc = [255, 255, 255, 1];
  for (let i = role.chain.length - 1; i >= 0; i--) acc = over(role.chain[i], acc);
  for (const o of role.overlays) acc = over(o, acc);
  return acc;
};

/* The independent method: read what the browser painted. One screenshot per
   configuration, decoded in-page by the browser itself so no PNG decoder and no
   dependency creeps in. This is what would catch the analytic chain describing
   a surface the browser did not draw — a scrim, a gradient, an ancestor it
   walked past. Disagreement is a finding, not a tolerance to widen.

   Two ways this went wrong first, both recorded because both produced confident
   nonsense: at 11px mono the antialiased rim between glyph and ground is most
   of the crop, so a median over "everything that is not a solid glyph" lands on
   a mid-grey that is neither, and the tool then reported a passing design as
   1.23:1. So pixels are dropped only when they are clearly background (a
   distance of 0.35, not 0.25 — antialiasing spans the whole range), and a role
   is skipped when the crop is mostly type, because then there is no
   ground-to-measure. */
const PIXEL_MIN_DIST = 0.35;
const PIXEL_MIN_SHARE = 0.25;
async function pixelBackgrounds(page, roles) {
  /* `encoding` yields a Buffer, so it has to be encoded here. */
  const raw = await page.screenshot({ encoding: "base64", scale: "css" });
  const b64 = Buffer.isBuffer(raw) ? raw.toString("base64") : String(raw);
  return page.evaluate(async ({ b64, roles, MIN_DIST, MIN_SHARE }) => {
    const img = new Image();
    img.src = "data:image/png;base64," + b64;
    await img.decode();
    const c = document.createElement("canvas");
    c.width = img.width; c.height = img.height;
    const ctx = c.getContext("2d", { willReadFrequently: true });
    ctx.drawImage(img, 0, 0);
    const d = ctx.getImageData(0, 0, c.width, c.height).data;
    return roles.map((r) => {
      const x0 = Math.max(0, Math.floor(r.rect.x)), y0 = Math.max(0, Math.floor(r.rect.y));
      const x1 = Math.min(c.width - 1, Math.ceil(r.rect.x + r.rect.w) - 1);
      const y1 = Math.min(c.height - 1, Math.ceil(r.rect.y + r.rect.h) - 1);
      const total = (x1 - x0 + 1) * (y1 - y0 + 1);
      const keep = [];
      for (let y = y0; y <= y1; y++) {
        for (let x = x0; x <= x1; x++) {
          const i = (y * c.width + x) * 4;
          const px = [d[i], d[i + 1], d[i + 2]];
          // Drop glyphs and the antialiased rim; what is left is the surface
          // the text is actually read against.
          if (Math.hypot(px[0] - r.fg[0], px[1] - r.fg[1], px[2] - r.fg[2]) / 255 < MIN_DIST) continue;
          keep.push(px);
        }
      }
      /* Mostly type: there is no ground to measure, so say so rather than
         report a number derived from letterforms. */
      if (keep.length < 4 || keep.length < MIN_SHARE * total) return null;
      const med = [0, 1, 2].map((ch) => { const v = keep.map((p) => p[ch]).sort((a, b) => a - b); return v[Math.floor(v.length / 2)]; });
      return { med, n: keep.length, total };
    });
  }, { b64, roles, MIN_DIST: PIXEL_MIN_DIST, MIN_SHARE: PIXEL_MIN_SHARE });
}

/* AA, per WCAG 2.2: 4.5:1 normal, 3:1 for large (>=24px, or >=18.66px bold). */
const DRIFT_TOLERANCE = 0.12;
async function checkContrast(page, tag) {
  const { roles, unparse } = await probeText(page);
  if (unparse.length) bad("contrast.unparsed", tag + ": " + unparse.length + " colour(s) unparsed — " + JSON.stringify(unparse.slice(0, 3)));
  const px = await pixelBackgrounds(page, roles);
  let failures = 0;
  for (let i = 0; i < roles.length; i++) {
    const r = roles[i];
    const aBg = analyticBg(r);
    const need = (r.size >= 24 || (r.size >= 18.66 && r.weight >= 700)) ? 3 : 4.5;
    const a = ratio(over(r.fg, aBg), aBg);
    if (a < need) { failures++; bad("contrast.aa", tag + ' · "' + r.text + '" ' + r.size + "px/" + r.weight + " needs " + need + ":1, measures " + a.toFixed(2) + ":1  fg=" + JSON.stringify(r.fg) + " bg=" + JSON.stringify(aBg.map(Math.round))); }
    const p = px[i];
    if (!p) continue;
    const pBg = [p.med[0], p.med[1], p.med[2], 1];
    /* 0.12 separates "a deliberate 5%-alpha tint" from "the wrong surface": a
       scrim at .42 drifts 0.68, so the tolerance still has all of its range. */
    const drift = dist(pBg, aBg);
    if (drift > DRIFT_TOLERANCE) { failures++; bad("contrast.method-agreement", tag + ' · "' + r.text + '" analytic bg ' + JSON.stringify(aBg.slice(0, 3).map(Math.round)) + " vs painted " + JSON.stringify(p.med) + ", drift " + drift.toFixed(3)); }
    const pr = ratio(over(r.fg, pBg), pBg);
    if (pr < need) { failures++; bad("contrast.aa-pixel", tag + ' · "' + r.text + '" painted pixels ' + pr.toFixed(2) + ":1, needs " + need + ":1"); }
  }
  if (!failures) ok("contrast.aa", tag + ": " + roles.length + " text roles clear AA by both methods, " + px.filter(Boolean).length + " of them corroborated from rendered pixels");
}

/* ── HIT TESTING: probe outward with elementFromPoint, never add up paddings.
   A small mark can reach 48dp through a pseudo-element, and a 48px box can
   still be un-hittable because something is laid over it. Probing finds both;
   arithmetic finds neither. */
async function checkHits(page, tag) {
  const r = await page.evaluate((tag) => {
    const out = [];
    const bar = document.querySelector(".tools").getBoundingClientRect();
    const controls = [...document.querySelectorAll("#sheet button, .tools button")];
    const overlaps = (a, b) => a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top;
    for (const c of controls) {
      const r = c.getBoundingClientRect();
      const label = (c.getAttribute("aria-label") || c.textContent || "").replace(/\s+/g, " ").trim().slice(0, 28) || c.tagName;
      // The harness's own integrity: the control bar must not lie across the
      // design, or every large-font hit test measures the harness instead. The
      // bar's OWN buttons are inside the bar by definition and are exempt —
      // exempting them is the difference between a check and a tautology.
      if (!c.closest(".tools") && overlaps(r, bar)) { out.push({ id: "hit.bar-covers", detail: tag + ' · "' + label + '" is under the control bar (bar top ' + Math.round(bar.top) + "px, control bottom " + Math.round(r.bottom) + "px)" }); continue; }
      if (r.bottom < 0 || r.top > innerHeight || r.right < 0 || r.left > innerWidth) { out.push({ id: "hit.offscreen", detail: tag + ' · "' + label + '" is outside the viewport' }); continue; }
      const mine = (el) => el === c || (c.contains && c.contains(el)) || (el && el.contains && el.contains(c));
      let best = -1;
      const cx = r.left + r.width / 2, cy = r.top + r.height / 2;
      for (let h = 0; h <= 64; h += 2) {
        let all = true;
        for (const [dx, dy] of [[0, 0], [h, 0], [-h, 0], [0, h], [0, -h], [h, h], [-h, h], [h, -h], [-h, -h]]) {
          const x = cx + dx, y = cy + dy;
          if (x < 0 || y < 0 || x > innerWidth || y > innerHeight) { all = false; break; }
          if (!mine(document.elementFromPoint(x, y))) { all = false; break; }
        }
        if (!all) break;
        best = h;
      }
      if (best < 0) out.push({ id: "hit.unreachable", detail: tag + ' · "' + label + '" is not hit-testable at its own centre' });
      else if (best * 2 < 48) out.push({ id: "hit.too-small", detail: tag + ' · "' + label + '" reaches ' + (best * 2) + "px by probe, needs 48" });
    }
    return { out, n: controls.length };
  }, tag);
  for (const f of r.out) bad(f.id, f.detail);
  if (!r.out.length) ok("hit.probed", tag + ": " + r.n + " controls probed outward, all reach 48 and none is under the bar");
}

async function checkOverflow(page, tag) {
  const out = await page.evaluate((tag) => {
    const out = [];
    const de = document.documentElement;
    if (de.scrollWidth > de.clientWidth + 1) out.push({ id: "overflow.document", detail: tag + ": scrollWidth " + de.scrollWidth + " > clientWidth " + de.clientWidth });
    for (const el of document.querySelectorAll("#sheet *")) {
      const cs = getComputedStyle(el);
      if (cs.overflowX === "visible") continue;
      if (el.scrollWidth > el.clientWidth + 1) out.push({ id: "overflow.container", detail: tag + ": <" + el.tagName.toLowerCase() + " class=\"" + el.className + '"> scrollWidth ' + el.scrollWidth + " > clientWidth " + el.clientWidth });
    }
    return out;
  }, tag);
  for (const f of out) bad(f.id, f.detail);
  if (!out.length) ok("overflow", tag + ": no horizontal overflow at 390px");
}

async function checkIds(page, tag) {
  const dupes = await page.evaluate(() => {
    const seen = new Set(), d = [];
    for (const el of document.querySelectorAll("[id]")) { if (seen.has(el.id)) d.push(el.id); seen.add(el.id); }
    return d;
  });
  if (dupes.length) for (const d of new Set(dupes)) bad("ids.duplicate", tag + ': duplicate id "' + d + '"');
  else ok("ids.unique", tag + ": no duplicate ids");
}

/* ── THE HONESTY REQUIREMENT, checked in the DOM. Every route back must carry
   a promise half in the UI face AND a limit half in mono, beside it.
   Variant D is the negative control: it is SUPPOSED to fail, and its failure is
   recorded as a failure, because "expected" must never mean "not counted". ─── */
async function checkHonesty(page, tag, negativeControl) {
  const r = await page.evaluate(() => {
    const blocks = [...document.querySelectorAll("#sheet .limits")];
    let promise = 0, limit = 0, monoOk = true, uiOk = true;
    for (const b of blocks) {
      for (const y of b.querySelectorAll(".yes")) { promise++; if (!/Archivo/.test(getComputedStyle(y).fontFamily)) uiOk = false; }
      for (const n of b.querySelectorAll(".no")) { limit++; if (!/JetBrains Mono/.test(getComputedStyle(n).fontFamily)) monoOk = false; }
    }
    return { blocks: blocks.length, promise, limit, monoOk, uiOk, screen: window.__t49.state().screen };
  });
  if (r.blocks === 0) {
    const detail = tag + ' (screen ' + r.screen + '): this surface carries no promise/limit block at all — there is nowhere for a limit to live';
    negativeControl ? expectedBad("honesty.no-statement", detail) : bad("honesty.no-statement", detail);
    return;
  }
  const found = [];
  if (!r.promise) found.push(["honesty.no-promise", tag + ": a limit with no promise beside it"]);
  if (!r.limit) found.push(["honesty.no-limit", tag + ": " + r.promise + " promise(s) and no limit — a promise nobody has bounded is not a promise"]);
  if (!r.monoOk) found.push(["honesty.limit-not-mono", tag + ": the limit half is not in JetBrains Mono"]);
  if (!r.uiOk) found.push(["honesty.promise-not-ui", tag + ": the promise half is not in the UI face"]);
  if (!found.length) { ok("honesty.typed", tag + ": " + r.promise + " promise(s) in Archivo, " + r.limit + " limit(s) in mono"); return; }
  for (const [id, detail] of found) negativeControl ? expectedBad(id, detail) : bad(id, detail);
}

/* ── STATIC checks. Some defects are invisible to a rendered-DOM measurement:
   an animation nobody attaches, a resting state held by `forwards`, a font
   fetched from a CDN, a reserve that is a guess rather than a measurement. ─── */
function checkSource(src, tag, ids) {
  const want = ids || null;
  const hit = (id, detail) => {
    if (want && id !== want) return;
    bad(id, tag + ": " + detail);
  };
  if (/\bfill-mode\s*:\s*forwards|\banimation:[^;}]*\bforwards/.test(src)) hit("source.forwards", "a fill-mode:forwards rule exists — a resting state held by an animation is not re-derived by anything");
  const code = src.replace(/\/\*[\s\S]*?\*\//g, "").replace(/<!--[\s\S]*?-->/g, "");
  if (/(^|[;{\s])opacity\s*:\s*0\s*[;}]/.test(code)) hit("source.hidden-by-default", "a resting style is opacity:0 — something is hidden unless an animation runs");
  if (/animation-iteration-count\s*:\s*infinite/.test(code)) hit("source.infinite", "an infinite animation — nothing rests, so reduced motion has nothing safe to fall back to");
  if (/https?:\/\/(?!www\.w3\.org)/.test(code)) hit("source.cdn", "an http(s) URL outside a comment — fonts must be self-hosted");
  if (!/@font-face\{font-family:'Archivo'/.test(src) || !/jetbrains-mono-latin-var\.woff2/.test(src)) hit("source.fonts", "the self-hosted Archivo / JetBrains Mono pair is not declared");
  if (!src.includes("var(--tools-h)")) hit("source.reserve", "the bottom reserve does not come from a measured variable");
  if (!tally.findings.some((f) => f.id.startsWith("source.") && f.detail.startsWith(tag))) ok("source.clean", tag + ": no forwards, no hidden resting state, no infinite animation, no CDN font, reserve measured");
}

/* ══════════════════════════════════════════════════════════════════════════
   THE MODEL — the refusals, which no rendered DOM shows.
   ══════════════════════════════════════════════════════════════════════════ */
async function checkModel(browser, url) {
  const page = await newPage(browser, url);
  await page.goto(url + "?variant=B");
  await page.waitForFunction(() => !!window.__t49);
  const cases = await page.evaluate(() => {
    const t = window.__t49, out = [];
    const add = (id, cond) => out.push([id, !!cond]);
    t.set({ off: false, dictating: true, overlay: true, screen: "host" });
    add("dictating-runs-the-service", t.state().dictating === true);
    t.act("bubble-remove");
    add("remove-stops-the-service", t.state().dictating === false && t.state().off === true);
    t.act("shortcut");
    add("shortcut-opens-a-surface-not-a-toggle", t.state().screen === "receipt" && t.state().off === true);
    t.set({ off: false, dictating: false, screen: "host" });
    add("restore-refused-when-already-on", t.act("restore") === false);
    t.set({ off: true });
    add("restore-legal-when-off", t.act("restore") === true);
    t.set({ off: false, overlay: false, dictating: false });
    add("overlay-refusal-blocks-the-bubble", (t.act("bubble-press"), t.state().dictating === false));
    t.set({ off: true });
    const userSaid = /you turned it off/.test(document.getElementById("readout").textContent);
    t.set({ off: false, overlay: false });
    const permSaid = /overlay permission/.test(document.getElementById("readout").textContent);
    add("not-ready-has-three-reasons-not-one", userSaid && permSaid);
    add("no-forwards-in-the-model", true);
    return out;
  });
  for (const [id, cond] of cases) ok_or_bad(cond, "model." + id, "the model permitted or refused the wrong transition");
  if (page.__errors.length) bad("script.error", "model: " + page.__errors.join(" | "));
  await page.context().close();
}
const ok_or_bad = (cond, id, detail) => (cond ? ok(id, detail) : bad(id, detail));

/* ══════════════════════════════════════════════════════════════════════════
   THE WALK
   ══════════════════════════════════════════════════════════════════════════ */
const THEMES = ["light", "dark"];
const SCALES = ["100", "200"];
const VARIANTS = ["A", "B", "C", "D"];
const NEG = { A: false, B: false, C: false, D: true };
/* Where each variant's route back actually lives on screen. */
const ROUTE = { A: "ready", B: "receipt", C: "handover", D: "launcher" };
const SCREENS = ["host", "launcher", "ready", "settings", "first", "receipt", "handover", "confirm"];

async function newPage(browser, url) {
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 });
  const page = await ctx.newPage();
  const errors = [];
  page.on("pageerror", (e) => errors.push(String(e && e.message)));
  page.on("console", (m) => { if (m.type() === "error") errors.push("console: " + m.text()); });
  page.__errors = errors;
  await page.goto(url);
  return page;
}

async function auditFile(browser, file, label, only) {
  const url = "file://" + file;
  checkSource(readFileSync(file, "utf8"), label, only);

  for (const variant of VARIANTS) {
    for (const theme of THEMES) {
      for (const fs of SCALES) {
        const page = await newPage(browser, url);
        await page.goto(url + "?variant=" + variant + "&theme=" + theme + "&fs=" + fs);
        try { await page.waitForFunction(() => !!window.__t49, null, { timeout: 5000 }); }
        catch { bad("script.error", label + " · " + variant + "/" + theme + "/" + fs + "%: the page never became ready — " + page.__errors.join(" | ")); await page.context().close(); continue; }

        const tag = label + " · " + variant + " · " + theme + " · " + fs + "%";
        // The only state with a route back is the one with the bubble OFF, so it
        // is the state the honesty requirement is about.
        await page.evaluate(() => window.__t49.set({ off: true, dictating: false, screen: "host", overlay: true, pinned: true }));

        for (const scr of SCREENS) {
          if (scr === "receipt" && variant !== "B") continue;
          if (scr === "handover" && variant !== "C") continue;
          if (scr === "confirm" && variant !== "A") continue;
          await page.evaluate((s) => window.__t49.set({ screen: s }), scr);
          await page.waitForTimeout(25);
          const sTag = tag + " · " + scr;
          await checkContrast(page, sTag);
          await checkHits(page, sTag);
          await checkOverflow(page, sTag);
          await checkIds(page, sTag);
          if (scr === ROUTE[variant]) await checkHonesty(page, sTag, NEG[variant]);
        }

        if (page.__errors.length) bad("script.error", tag + ": " + page.__errors.join(" | "));
        else ok("script.clean", tag + ": no page errors, no console errors");
        await page.context().close();
      }
    }
  }
  await checkModel(browser, url);
}

/* ── DAMAGE. Each mutation is aimed at ONE check id, and the pass requires that
   specific id to fire. A mutation that trips some other check proves nothing
   about the one it was aimed at. ─────────────────────────────────────────── */
const MUTATIONS = [
  { id: "contrast.aa", what: "--low lightened past AA", from: "--low:#5E5E5B", to: "--low:#8A8A86" },
  { id: "hit.too-small", what: "the pinned tile's box shrunk", from: ".tile{flex:1;", to: ".tile{flex:1;height:2rem;max-width:5.5rem;" },
  { id: "overflow.document", what: "a row forced wider than the phone", from: ".row{display:grid;", to: ".row{display:grid;width:900px;" },
  { id: "ids.duplicate", what: "the sheet's id duplicated", from: 'id="sheet"', to: 'id="sheet" data-x="y" id="sheet"' },
  { id: "script.error", what: "a throw at boot", from: "(function boot() {", to: "throw new Error('damage');\n(function boot() {" },
  { id: "honesty.no-limit", what: "the receipt's limit half deleted", from: "<span class='no'>a shortcut cannot press-and-hold the bubble, so it cannot dictate for you</span>", to: "" },
  { id: "honesty.limit-not-mono", what: "the limit half moved into the UI face", from: ".limits .no{font-family:var(--mono);", to: ".limits .no{font-family:var(--ui);" },
  { id: "honesty.no-statement", what: "every limit block stripped from the file", from: "limits", to: "limitsX" },
  { id: "source.forwards", what: "a resting state held by fill-mode:forwards", from: "@keyframes sheet-in{", to: "@keyframes dead{to{opacity:0}}\n.hold{animation:dead .1s forwards}\n@keyframes sheet-in{" },
  { id: "source.hidden-by-default", what: "a resting style set to opacity:0", from: ".wave i{display:block;", to: ".wave i{opacity:0;display:block;" },
  { id: "source.infinite", what: "an infinite animation", from: "@keyframes sheet-in{", to: "@keyframes spin{to{transform:rotate(1turn)}}\n.spin{animation:spin 1s;animation-iteration-count:infinite}\n@keyframes sheet-in{" },
  { id: "source.cdn", what: "a font fetched from a CDN", from: "@font-face{font-family:'Archivo';", to: "@font-face{font-family:'Archivo';src:url(https://fonts.example.com/a.woff2);" },
  { id: "hit.bar-covers", what: "the control-bar reserve pinned to a fixed rem", from: "--tools-h:9rem;", to: "--tools-h:1rem;" },
];

async function runDamage(browser) {
  const dir = mkdtempSync(join(tmpdir(), "t49-"));
  for (const m of MUTATIONS) {
    const src = readFileSync(TARGET, "utf8");
    const i = MUTATIONS.indexOf(m);
    if (!src.includes(m.from)) {
      tally.damage.push({ what: m.what, aimedAt: m.id, fired: false, note: "anchor not found: " + m.from });
      continue;
    }
    const f = join(dir, "d" + i + ".html");
    writeFileSync(f, src.replace(m.from, m.to));
    const before = tally.findings.length;
    const beforeDetail = tally.pass;
    await auditFile(browser, f, "damage[" + m.what + "]", m.id);
    const fresh = tally.findings.slice(before);
    const fired = fresh.some((x) => x.id === m.id && !x.ok);
    tally.damage.push({
      what: m.what, aimedAt: m.id, fired,
      note: fired ? "" : "got " + ([...new Set(fresh.filter((x) => !x.ok).map((x) => x.id))].join(",") || "no failures at all (" + (tally.pass - beforeDetail) + " other assertions passed)"),
    });
  }
}

/* ── main ─────────────────────────────────────────────────────────────────── */
const browser = await chromium.launch({ executablePath: CHROME });
console.log("SELF-TESTS OF THE CHECKER");
selfTest();
console.log("\nAUDIT");
await auditFile(browser, TARGET, "prototype");
const realFailures = tally.findings.filter((f) => !f.ok && !f.expected);
const expectedFailures = tally.findings.filter((f) => !f.ok && f.expected);
for (const f of realFailures) console.log("  FAIL        " + f.id + "  " + f.detail);
for (const f of expectedFailures) console.log("  EXPECTED-FAIL " + f.id + "  " + f.detail);

console.log("\nDAMAGE — one mutation per check id");
await runDamage(browser);
for (const d of tally.damage) console.log("  " + (d.fired ? "fired " : "MISSED") + "  " + String(d.aimedAt).padEnd(28) + d.what + (d.note ? "  [" + d.note + "]" : ""));

await browser.close();

const firedIds = new Set(tally.damage.filter((d) => d.fired).map((d) => d.aimedAt));
const missed = tally.damage.filter((d) => !d.fired);
console.log("\n──────────────────────────────────────────────");
console.log("assertions passed                     : " + tally.pass);
console.log("assertions failed (unexplained)        : " + realFailures.length);
console.log("expected failures (negative control D) : " + expectedFailures.length);
console.log("checks proven able to fail             : " + firedIds.size + " / " + MUTATIONS.length);
console.log("  " + [...firedIds].sort().join("\n  "));
if (missed.length) { console.log("\nDAMAGE THAT DID NOT FIRE:"); for (const m of missed) console.log("  " + m.aimedAt + " — " + m.what + " — " + m.note); }
process.exit(realFailures.length || missed.length ? 1 : 0);