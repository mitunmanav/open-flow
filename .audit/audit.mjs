/* Audit harness for the permissions prototype (ticket 55). TEMPORARY — not committed.
 *
 *   1. STATIC   — source, manifest, permissions-policy.md, privacy-policy.md,
 *                 GLOSSARY.md.
 *   2. RUNTIME  — both themes x every grant state, in a real browser:
 *                 alpha-compositing contrast resolver over the PAINT STACK,
 *                 non-text contrast, hit targets probed outward with
 *                 elementFromPoint, mid-transition contrast, and animation
 *                 residue under reduced motion.
 *   3. DAMAGE   — every check is also run against a deliberately broken copy
 *                 of the prototype, because a checker that has never seen a
 *                 failure has not been shown it can fail.
 *
 * usage: node audit.mjs [prototype.html] [--no-damage] [--dump] [--json out.json]
 */
import { chromium } from '/home/mitun/career-ops/node_modules/playwright/index.mjs';
import fs from 'node:fs';
import path from 'node:path';

const REPO = process.env.REPO || path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const firstArg = process.argv.slice(2).find(a => !a.startsWith('--'));
const TARGET = path.resolve(firstArg ||
  path.join(REPO, '.scratch/openflow-v1/prototype/onboarding.html'));
const NO_DAMAGE = process.argv.includes('--no-damage');
const DUMP = process.argv.includes('--dump');
const JSON_OUT = (() => {
  const i = process.argv.indexOf('--json');
  return i > -1 ? process.argv[i + 1] : null;
})();

const DOCS = {
  manifest: path.join(REPO, 'app/src/main/AndroidManifest.xml'),
  policy: path.join(REPO, 'docs/privacy/permissions-policy.md'),
  privacy: path.join(REPO, 'docs/privacy/privacy-policy.md'),
  glossary: path.join(REPO, 'GLOSSARY.md'),
};

const RESULT = { assertions: 0, failures: {}, fired: {}, all: {} };
const note = (name, ok, detail) => {
  RESULT.assertions++;
  (RESULT.all[name] = RESULT.all[name] || []).push({ ok, detail });
  if (!ok) (RESULT.failures[name] = RESULT.failures[name] || []).push(detail);
};

/* ══════════════════════════════════════════════════════════════════════════
   1. STATIC
   ══════════════════════════════════════════════════════════════════════════ */
function extractRows(src) {
  const rows = [];
  const block = src.match(/const ASKS\s*=\s*\[([\s\S]*?)\n\];/);
  if (!block) return rows;
  for (const m of block[1].matchAll(/\{([\s\S]*?)\}/g)) {
    const body = m[1];
    const field = k => {
      const f = new RegExp(k + ":\\s*(['\"])([\\s\\S]*?)\\1").exec(body);
      return f ? f[2] : null;
    };
    rows.push({
      id: field('id'), name: field('name'), does: field('does'), nots: field('nots'),
      when: field('when'),
      blocking: /blocking:\s*false/.test(body) ? false : true,
    });
  }
  return rows;
}

function staticChecks(src, docs, file) {
  const rel = path.relative(REPO, file);

  /* fonts: committed, self-hosted, never a CDN */
  note('font-not-from-cdn',
    !/fonts\.(googleapis|gstatic)\.com|cdn\.jsdelivr|unpkg\.com|typekit|fonts\.bunny/.test(src),
    `${rel} references an external font host`);
  const faces = [...src.matchAll(/@font-face\s*\{([^}]*)\}/g)].map(m => m[1]);
  note('font-face-present', faces.length >= 2,
    `${rel} declares ${faces.length} @font-face rule(s); two self-hosted faces are required`);
  for (const body of faces) {
    for (const m of body.matchAll(/url\((['"]?)([^'")]+)\1\)/g)) {
      const abs = path.resolve(path.dirname(file), m[2]);
      note('font-face-file-exists', fs.existsSync(abs), `@font-face url(${m[2]}) → ${abs} does not exist`);
    }
  }

  /* the download is not a permission, and its figure is not stale */
  note('no-model-row', !/\{\s*id:\s*'model'/.test(src) && !/id="model"/.test(src),
    `${rel} still defines a 'model' ask row`);
  const stale = /(\d+)\s*MB\s+(once|fetch|download)/i.exec(src) ||
    /fetch(es|ed)?\s+a\s+(\d+)\s*MB/i.exec(src);
  note('no-stale-download-figure', !stale,
    `${rel} still states the fetch as ~${stale ? stale[1] : '?'} MB: ${stale ? stale[0] : ''}`);

  /* the page must not promise an absolute "no network" while setup fetches */
  const abs = /no location,?\s+contacts,?\s+photos,?\s+or\s+network/i.exec(src);
  note('network-promise-is-true', !abs,
    `${rel} promises an unqualified "no network" while first launch fetches the model`);

  /* the accessibility row must disclose the capability it declines */
  note('a11y-declines-capability',
    /(could|can)\s+read[^'"]*we\s+(do\s+not|don'?t)/i.test(src),
    `${rel} never states that the service COULD read the focused field and declines to`);

  /* POST_NOTIFICATIONS is declared in the manifest and needs a row */
  note('notifications-row-present',
    /id:\s*'notify'/.test(src) && /mic is live/.test(src),
    `${rel} has no POST_NOTIFICATIONS row`);

  /* design language, asserted as CSS rather than as prose */
  const radii = [...src.matchAll(/border-radius:\s*([^;}]+)/g)].map(m => m[1].trim());
  note('radius-zero', radii.length > 0 && radii.every(v => /^0(px|%)?$/.test(v)),
    `${rel} sets a non-zero border-radius: ${radii.filter(v => !/^0(px|%)?$/.test(v)).join(', ')}`);
  note('no-shadow', !/box-shadow:\s*(?!none)/.test(src), `${rel} sets a box-shadow other than none`);
  note('perimeter-state-mark',
    /\.row::before\s*\{[^}]*mask-image:\s*conic-gradient/.test(src),
    `${rel} does not draw the granted state as the row's own perimeter (conic mask)`);
  const before = /\.row::before\s*\{([\s\S]*?)\n\}/.exec(src);
  note('perimeter-does-not-fill',
    !!before && !/background:\s*(?!transparent|none)/.test(before[1]),
    `${rel} gives the perimeter pseudo-element a background, which would paint over row text`);
  const doesRule = /\.row \.does\s*\{([^}]*)\}/.exec(src);
  const notsRule = /\.row \.nots\s*\{([^}]*)\}/.exec(src);
  note('mono-for-limit-clause',
    !!notsRule && /font-family:\s*var\(--font-mono\)/.test(notsRule[1]) &&
    !!doesRule && !/font-family:\s*var\(--font-mono\)/.test(doesRule[1]),
    `${rel} does not set the limit clause in mono against the capability clause in the UI face`);
  note('reduced-motion-handled', /@media\s*\(prefers-reduced-motion/.test(src),
    `${rel} has no prefers-reduced-motion block`);
  note('high-contrast-handled', /@media\s*\(prefers-contrast/.test(src),
    `${rel} has no prefers-contrast block`);
  const accents = [...src.matchAll(/--accent:\s*(#[0-9A-Fa-f]{3,8})/g)].map(m => m[1].toLowerCase());
  note('single-accent-per-theme', accents.length === 2 && accents[0] !== accents[1],
    `${rel} declares ${accents.length} accent value(s): ${accents.join(', ')}`);

  /* ── cross-document agreement ───────────────────────────────────────────── */
  const manifest = fs.readFileSync(docs.manifest, 'utf8');
  const policy = fs.readFileSync(docs.policy, 'utf8');
  const privacy = fs.readFileSync(docs.privacy, 'utf8');
  const glossary = fs.readFileSync(docs.glossary, 'utf8');

  /* What each row is an ask FOR. a11y binds through the service's own
     android:permission rather than a uses-permission, so it is checked in the
     policy instead of the manifest. */
  const ROW_PERMISSION = {
    mic: 'RECORD_AUDIO',
    overlay: 'SYSTEM_ALERT_WINDOW',
    a11y: 'BIND_ACCESSIBILITY_SERVICE',
    notify: 'POST_NOTIFICATIONS',
  };
  const declared = [...manifest.matchAll(/<uses-permission android:name="android\.permission\.([A-Z_]+)"/g)].map(m => m[1]);
  for (const [row, perm] of Object.entries(ROW_PERMISSION)) {
    if (row === 'a11y') continue;
    note('row-is-declared-in-manifest', declared.includes(perm),
      `row "${row}" claims ${perm}, which the manifest does not declare`);
    note('row-is-documented-in-policy', policy.includes(perm),
      `${perm} is a row in the prototype but does not appear in permissions-policy.md`);
  }
  note('a11y-not-a-uses-permission',
    !declared.includes('BIND_ACCESSIBILITY_SERVICE') && /BIND_ACCESSIBILITY_SERVICE/.test(policy),
    'the accessibility service binds by android:permission: in the policy, not in uses-permission');
  note('policy-has-no-download-row',
    !/Voice model/.test(policy) || /not a permission/.test(policy),
    'permissions-policy.md lists a model download as a permission');
  note('privacy-discloses-the-fetch',
    /128 MB|127,887,156/.test(privacy) && /voice model/i.test(privacy),
    'privacy-policy.md does not disclose the first-launch model fetch, so "nothing leaves the device" reads as "nothing uses the network"');

  /* GLOSSARY.md: a row name must not be a glossary term for something else.
     "Dictation" is the full capture-to-insertion cycle; the accessibility
     service is not that cycle. */
  const terms = [...glossary.matchAll(/^##\s+(.+)$/gm)].map(m => m[1].trim());
  const rows = extractRows(src);
  for (const row of rows) {
    if (terms.includes(row.name)) {
      note('row-name-is-not-a-glossary-term', row.name !== 'Dictation',
        `row "${row.id}" is named "${row.name}", which GLOSSARY.md defines as the whole capture-to-insertion cycle, not the accessibility service`);
    }
  }
  note('glossary-bubble-is-the-row-name',
    rows.some(r => r.name === 'Bubble') && /_Avoid_:.*overlay/i.test(glossary),
    'the overlay row should be named Bubble (the GLOSSARY.md term, with overlay listed as an avoid-word)');

  /* the count on the page must equal the number of gating asks */
  const gating = rows.filter(r => r.blocking).length;
  note('row-count-has-no-stale-word', !/\bfour\b/i.test(src) && !/\bfive\b/i.test(src),
    'a numeral in the page copy still says "four"');
  note('cta-count-matches-rows', new RegExp(`Grant all ${gating}\\b`).test(src),
    `the primary action does not say "Grant all ${gating}"`);
  note('odo-has-a-digit-per-count', (src.match(/<i>\d<\/i>/g) || []).length === gating + 1,
    `the odometer strip does not carry ${gating + 1} digits`);
  return { rows, gating };
}

/* ══════════════════════════════════════════════════════════════════════════
   2. RUNTIME  (runs inside the page)
   ══════════════════════════════════════════════════════════════════════════ */
const PAGE = String.raw`
window.__A = window.__A || {};
window.__M = window.__M || {};
(function () {
  const push = (name, ok, detail) => { (window.__A[name] = window.__A[name] || []).push({ ok: !!ok, detail: String(detail) }); };
  const M = {};

  function parse(str) {
    if (!str) return { r: 0, g: 0, b: 0, a: 0 };
    const m = str.match(/rgba?\(([^)]+)\)/);
    if (!m) return { r: 0, g: 0, b: 0, a: 0 };
    const p = m[1].split(/[\s,/]+/).filter(Boolean).map(Number);
    return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
  }
  const chan = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
  const lum = c => 0.2126 * chan(c.r) + 0.7152 * chan(c.g) + 0.0722 * chan(c.b);
  /* source-over: src painted ON TOP of dst. Alpha is carried, so a transparent
     layer really is transparent and cannot be read as opaque black. */
  function over(dst, src) {
    const a = src.a + dst.a * (1 - src.a);
    if (a === 0) return { r: 0, g: 0, b: 0, a: 0 };
    return { r: (src.r * src.a + dst.r * dst.a * (1 - src.a)) / a,
             g: (src.g * src.a + dst.g * dst.a * (1 - src.a)) / a,
             b: (src.b * src.a + dst.b * dst.a * (1 - src.a)) / a, a };
  }
  const mix = (c, d, t) => ({ r: c.r + (d.r - c.r) * t, g: c.g + (d.g - c.g) * t,
                               b: c.b + (d.b - c.b) * t, a: c.a + (d.a - c.a) * t });
  function ratio(fg, bg) {
    const a = lum(fg), b = lum(bg), hi = Math.max(a, b), lo = Math.min(a, b);
    return (hi + 0.05) / (lo + 0.05);
  }
  const hex = c => '#' + [c.r, c.g, c.b].map(v => Math.round(v).toString(16).padStart(2, '0')).join('');
  Object.assign(M, { parse, lum, over, mix, ratio, hex });

  /* THE RESOLVER: the paint stack at a point, not the ancestor chain.
     document.elementsFromPoint returns hit order, topmost first, so reversing
     it gives the order the compositor blends in. A selection highlight that is
     a SIBLING of the text lands in that stack; an ancestor-only walk misses it
     and reports the selected tab as ground-on-ground. */
  function paintAt(x, y) {
    const stack = document.elementsFromPoint(x, y);
    if (!stack.length) return null;
    let acc = { r: 0, g: 0, b: 0, a: 0 };
    for (const node of stack.slice().reverse()) {
      const c = parse(getComputedStyle(node).backgroundColor);
      if (c.a > 0) acc = over(acc, c);
    }
    return acc;
  }
  function naiveAt(el) {
    let acc = { r: 0, g: 0, b: 0, a: 0 };
    for (let n = el; n; n = n.parentElement) acc = over(acc, parse(getComputedStyle(n).backgroundColor));
    return acc;
  }
  /* the ground BEHIND an element: the paint stack at a point inside it, minus
     the element's own fill — otherwise the base background is composited twice
     and the midpoint is measured against a colour that was never on screen */
  function groundBehind(el) {
    const r = el.getBoundingClientRect();
    const x = r.left + r.width / 2;
    const y = r.top + Math.min(2, Math.max(1, r.height / 2));
    let acc = { r: 0, g: 0, b: 0, a: 0 };
    for (const node of document.elementsFromPoint(x, y).slice().reverse()) {
      if (node === el) continue;
      const c = parse(getComputedStyle(node).backgroundColor);
      if (c.a > 0) acc = over(acc, c);
    }
    return acc;
  }
  Object.assign(M, { paintAt, naiveAt, groundBehind });

  /* Custom properties resolve to their token, not to rgb(). Borrow a real
     element's computed value rather than parsing the declaration. */
  function token(name, prop) {
    const probe = document.createElement('i');
    probe.style.cssText = 'position:absolute;width:0;height:0;opacity:0;' +
      (prop === 'color' ? 'color' : 'background-color') + ':var(' + name + ')';
    document.body.appendChild(probe);
    const v = getComputedStyle(probe)[prop === 'color' ? 'color' : 'backgroundColor'];
    probe.remove();
    return parse(v);
  }
  M.token = token;

  /* visible, on-screen, text-bearing elements */
  function textRoles() {
    const out = [];
    for (const el of document.querySelectorAll('body *')) {
      const cs = getComputedStyle(el);
      if (cs.visibility === 'hidden' || cs.display === 'none' || Number(cs.opacity) === 0) continue;
      if (![...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim())) continue;
      const rects = [...el.getClientRects()];
      if (!rects.length) continue;
      let clipped = false;
      for (let n = el.parentElement; n; n = n.parentElement) {
        const s = getComputedStyle(n);
        if (s.overflow === 'visible' && s.overflowX === 'visible' && s.overflowY === 'visible') continue;
        const b = n.getBoundingClientRect(), r = rects[0];
        if (r.left < b.left - 0.5 || r.right > b.right + 0.5 || r.top < b.top - 0.5 || r.bottom > b.bottom + 0.5) { clipped = true; break; }
      }
      if (clipped) continue;
      const size = parseFloat(cs.fontSize), weight = Number(cs.fontWeight) || 400;
      out.push({ el, rect: rects[0], cs, size, weight,
        large: size >= 24 || (size >= 18.66 && weight >= 700),
        label: (el.className || el.tagName) + ': ' + el.textContent.trim().slice(0, 30) });
    }
    return out;
  }

  /* every text role, at three points across it, in the run's theme */
  M.contrast = function (mode) {
    for (const t of textRoles()) {
      if (t.cs.backgroundImage && t.cs.backgroundImage !== 'none') { push('contrast', false, 'background-image behind text: ' + t.label); continue; }
      const r = t.rect;
      const pts = [[r.left + r.width / 2, r.top + r.height / 2],
                   [r.left + r.width * 0.2, r.top + r.height / 2],
                   [r.right - r.width * 0.2, r.top + r.height / 2]];
      let worst = Infinity, ground = null, fgHex = null;
      for (const p of pts) {
        const acc = mode === 'naive' ? naiveAt(t.el) : paintAt(p[0], p[1]);
        if (!acc || acc.a === 0) continue;
        const fg = over(acc, parse(t.cs.color));
        const cr = ratio(fg, acc);
        if (cr < worst) { worst = cr; ground = acc; fgHex = fg; }
      }
      if (ground === null) { push('contrast', false, t.label + ' — no paintable ground under it'); continue; }
      const need = t.large ? 3 : 4.5;
      push('contrast', worst >= need, t.label + ' = ' + worst.toFixed(2) + ':1 (' + hex(fgHex) +
        ' on ' + hex(ground) + '), needs ' + need + ':1');
    }
  };

  /* 1.4.11: the perimeter is the state mark. It must read as a shape, not a tint. */
  M.nonText = function () {
    const body = parse(getComputedStyle(document.body).backgroundColor);
    const root = parse(getComputedStyle(document.documentElement).backgroundColor);
    const ground = over(root, body);
    const accent = token('--accent', 'background-color');
    push('non-text-state-mark', ratio(accent, ground) >= 3,
      'accent ' + hex(accent) + ' on ' + hex(ground) + ' = ' + ratio(accent, ground).toFixed(2) + ':1, needs 3:1');
    const bar = token('--bar', 'background-color');
    const track = over(ground, bar);
    push('non-text-meter-track', ratio(accent, track) >= 3,
      'meter fill ' + hex(accent) + ' on its own track ' + hex(track) + ' = ' +
      ratio(accent, track).toFixed(2) + ':1, needs 3:1');
  };

  /* Hit targets: probe OUTWARD with elementFromPoint until something else
     answers. Adding up paddings cannot see a control whose visual is meant to
     stay small and reaches the floor through a pseudo-element. */
  M.touchTargets = function (floor) {
    M.probeWins = [];
    const controls = [...document.querySelectorAll('button, [role="button"], a[href], input, [tabindex="0"]')]
      .filter(el => el.getClientRects().length);
    for (const el of controls) {
      const b = el.getBoundingClientRect();
      const xs = [], ys = [];
      const MAX = 160;
      for (let y = Math.max(0, Math.floor(b.top) - MAX); y <= Math.min(innerHeight - 1, Math.ceil(b.bottom) + MAX); y += 2) {
        for (let x = Math.max(0, Math.floor(b.left) - MAX); x <= Math.min(innerWidth - 1, Math.ceil(b.right) + MAX); x += 2) {
          /* the point belongs to this control when the topmost thing painted
             there IS the control or something inside it. An ancestor must not
             absorb it: a control's reach is its own box, grown by a
             pseudo-element the browser hit-tests to the element itself. */
          const hit = document.elementFromPoint(x, y);
          if (hit && (hit === el || el.contains(hit))) { xs.push(x); ys.push(y); }
        }
      }
      const w = xs.length ? Math.max(...xs) - Math.min(...xs) + 1 : 0;
      const h = ys.length ? Math.max(...ys) - Math.min(...ys) + 1 : 0;
      const name = (el.id || el.className || el.tagName) + ': ' +
        (el.textContent || el.getAttribute('aria-label') || '').trim().slice(0, 20);
      push('touch-target', w >= floor && h >= floor, name + ' reaches ' + w + '×' + h +
        ' (own box ' + Math.round(b.width) + '×' + Math.round(b.height) + '), needs ' + floor + '×' + floor);
      if (w > b.width + 1 || h > b.height + 1) M.probeWins.push(name + ' probed ' + w + '×' + h + ' vs box ' +
        Math.round(b.width) + '×' + Math.round(b.height));
    }
  };

  /* A press is not a transition: where a state's colours cross-fade, the
     midpoint clears AA too, or the label walks through mid-grey while a finger
     is down. Endpoints come from the CSSOM, so the measurement does not depend
     on catching a live frame. */
  M.transitions = function () {
    for (const sheet of document.styleSheets) {
      let rules; try { rules = sheet.cssRules; } catch (e) { continue; }
      if (!rules) continue;
      for (const rule of rules) {
        if (!rule.selectorText || !rule.style) continue;
        if (!/:hover|:active/.test(rule.selectorText)) continue;
        const bare = rule.selectorText.replace(/:hover|:active/g, '').trim();
        if (!bare || /^\*?$/.test(bare)) continue;
        let els = []; try { els = [...document.querySelectorAll(bare)]; } catch (e) { continue; }
        for (const el of els) {
          if (!el.getClientRects().length) continue;
          const base = getComputedStyle(el);
          const tp = base.transitionProperty, td = parseFloat(base.transitionDuration) || 0;
          if (!/(^|,\s*)(all|color|background|background-color)(\s*,|$)/.test(tp) || td <= 0) continue;
          if (!el.textContent.trim()) continue;
          const ground = M.groundBehind(el);
          if (!ground || ground.a === 0) continue;
          const id = 'data-probe-' + Math.random().toString(36).slice(2);
          const st = document.createElement('style');
          /* transition:none is load-bearing, not tidiness: these states
             transition their background, so a computed style read straight after
             injecting the rule returns the value the element is animating FROM —
             every midpoint would measure the resting state and pass. */
          st.textContent = '[' + id + '] { transition: none !important; ' + rule.style.cssText + ' }';
          document.head.appendChild(st);
          const baseFg = parse(base.color), baseBg = parse(base.backgroundColor);
          el.setAttribute(id, '');
          const tgt = getComputedStyle(el);
          const tgtFg = parse(tgt.color), tgtBg = parse(tgt.backgroundColor);
          const size = parseFloat(tgt.fontSize), weight = Number(tgt.fontWeight) || 400;
          const need = (size >= 24 || (size >= 18.66 && weight >= 700)) ? 3 : 4.5;
          let worst = Infinity, at = 0, g = null, f = null;
          for (let i = 0; i <= 20; i++) {
            const t = i / 20;
            const bg = over(ground, mix(baseBg, tgtBg, t));
            const fg = over(bg, mix(baseFg, tgtFg, t));
            const cr = ratio(fg, bg);
            if (cr < worst) { worst = cr; at = t; g = bg; f = fg; }
          }
          el.removeAttribute(id); st.remove();
          push('transition-midpoint', worst >= need, bare + ' under ' + rule.selectorText +
            ' — worst ' + worst.toFixed(2) + ':1 at +' + Math.round(at * td * 1000) + 'ms (' +
            hex(f) + ' on ' + hex(g) + '), needs ' + need + ':1');
        }
      }
    }
  };

  /* Reduced motion is no animation at all: anything still attached once
     everything has settled is a resting style held by a keyframe. */
  M.residue = function () {
    const live = document.getAnimations().filter(a => a.playState === 'running' || a.playState === 'paused');
    push('no-attached-animation', live.length === 0, live.length +
      ' animation(s) still attached after settling: ' +
      live.map(a => a.animationName || 'unnamed').join(', '));
  };

  /* the granted state really is the perimeter, and it completes */
  M.perimeter = function () {
    const before = getComputedStyle(document.querySelector('.row'), '::before');
    const mask = before.webkitMaskImage || before.maskImage || '';
    push('perimeter-mask-live', /conic-gradient/.test(mask), '.row::before mask-image: ' + mask);
    const pseudoBg = parse(before.backgroundColor);
    push('perimeter-interior-clear', pseudoBg.a === 0,
      '.row::before background-color: ' + before.backgroundColor +
      ' (alpha ' + pseudoBg.a + ' — an opaque fill would paint over the row text)');
    for (const row of document.querySelectorAll('.row[data-on="1"]')) {
      const sweep = getComputedStyle(row).getPropertyValue('--sweep').trim();
      push('perimeter-completes', sweep === '360deg' || parseFloat(sweep) > 355,
        'granted row ' + row.dataset.ask + ' settled at --sweep: ' + (sweep || '(unset)'));
    }
  };

  Object.assign(window.__M, M);
})();
`;

const STATES = [
  { id: 'initial', grant: [] },
  { id: 'gating', grant: ['mic', 'overlay', 'a11y'] },
  { id: 'all', grant: ['mic', 'overlay', 'a11y', 'notify'] },
];

const collect = page => page.evaluate(() => window.__A || {});

async function runtimeAudit(file, themes = ['light', 'dark']) {
  const browser = await chromium.launch();
  const out = {};
  try {
    for (const th of themes) {
      for (const st of STATES) {
        const page = await browser.newPage({ viewport: { width: 430, height: 932 } });
        await page.emulateMedia({ reducedMotion: 'no-preference' });
        await page.goto('file://' + file);
        await page.evaluate(t => window.setTheme(t), th);
        for (const id of st.grant) await page.evaluate(k => { const r = document.querySelector('[data-ask="' + k + '"]'); if (r) r.click(); }, id);
        await page.waitForTimeout(1500);
        await page.addScriptTag({ content: PAGE });
        await page.evaluate(() => window.__M.contrast('paint'));
        await page.evaluate(() => window.__M.nonText());
        await page.evaluate(() => window.__M.touchTargets(48));
        await page.evaluate(() => window.__M.transitions());
        await page.evaluate(() => window.__M.perimeter());
        const probeWins = await page.evaluate(() => window.__M.probeWins || []);
        for (const n of probeWins) (out['probe-sees-more-than-the-box'] = out['probe-sees-more-than-the-box'] || [])
          .push({ ok: true, detail: n + ' — only a probe can see this (' + th + '/' + st.id + ')' });
        for (const [k, v] of Object.entries(await collect(page))) {
          for (const r of v) (out[k] = out[k] || []).push({ ...r, detail: r.detail + ' [' + th + '/' + st.id + ']' });
        }
        await page.close();

        /* the same state under reduced motion: what is still attached at rest? */
        const rm = await browser.newPage({ viewport: { width: 430, height: 932 } });
        await rm.emulateMedia({ reducedMotion: 'reduce' });
        await rm.goto('file://' + file);
        await rm.evaluate(t => window.setTheme(t), th);
        for (const id of st.grant) await rm.evaluate(k => { const r = document.querySelector('[data-ask="' + k + '"]'); if (r) r.click(); }, id);
        await rm.waitForTimeout(900);
        await rm.addScriptTag({ content: PAGE });
        await rm.evaluate(() => window.__M.residue());
        await rm.evaluate(() => window.__M.perimeter());
        for (const [k, v] of Object.entries(await collect(rm))) {
          for (const r of v) (out[k] = out[k] || []).push({ ...r, detail: r.detail + ' [reduced-motion/' + th + ']' });
        }
        await rm.close();
      }
    }
  } finally { await browser.close(); }
  for (const [k, list] of Object.entries(out)) {
    for (const r of list) { RESULT.assertions++; (RESULT.all[k] = RESULT.all[k] || []).push(r); if (!r.ok) (RESULT.failures[k] = RESULT.failures[k] || []).push(r.detail); }
  }
  return out;
}

/* ══════════════════════════════════════════════════════════════════════════
   3. RESOLVER UNIT TEST — a selection highlight that is a SIBLING block
   ══════════════════════════════════════════════════════════════════════════ */
async function resolverUnitTest() {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 400, height: 300 } });
  await page.setContent(`<style>
    html,body{margin:0;background:#F4F4F2}
    .ground{padding:40px}
    .row{position:relative}
    .row span{color:#0A0A0A;font:14px sans-serif}
    .hl{position:absolute;inset:0;background:#A78BFA}
  </style>
  <div class="ground"><div class="row"><span id="t">selected</span><i class="hl"></i></div></div>`);
  await page.addScriptTag({ content: PAGE });
  const r = await page.evaluate(() => {
    const el = document.getElementById('t');
    const b = el.getBoundingClientRect();
    return { paint: window.__M.hex(window.__M.paintAt(b.left + b.width / 2, b.top + b.height / 2)),
             naive: window.__M.hex(window.__M.naiveAt(el)) };
  });
  await browser.close();
  note('sibling-highlight-resolved', r.paint === '#a78bfa',
    'a sibling selection highlight must resolve as the ground; the paint stack gave ' + r.paint);
  note('sibling-highlight-not-ground-on-ground', r.naive !== r.paint,
    'the ancestor-only walk returned the same ground as the paint stack (' + r.naive +
    '), so this test cannot tell the two resolvers apart');
  return r;
}

/* ══════════════════════════════════════════════════════════════════════════
   4. DAMAGE SUITE
   ══════════════════════════════════════════════════════════════════════════ */
const DAMAGE = [
  ['dark buttons lose color:inherit', 'contrast', s =>
    s.replace('.tools button {', '.tools button { color: #000;')],
  ['light --low dimmed below AA', 'contrast', s => s.replace('--low:     #6E6E6B;', '--low:     #8B8B86;')],
  ['transparent ancestor read as opaque black (the .slice(0,3) bug)', 'contrast', s =>
    s.replace('* { box-sizing: border-box;', '* { background: rgba(0,0,0,0); box-sizing: border-box;')],
  ['model row comes back', 'no-model-row', s =>
    s.replace(/const ASKS = \[\n/, "const ASKS = [\n  { id:'model', name:'Voice model', does:'45 MB once, then offline', nots:'never uploaded', blocking:true },\n")],
  ['45 MB fetch wording returns', 'no-stale-download-figure', s =>
    s.replace(/does:'[^']*'/, "does:'45 MB once, then offline'")],
  ['absolute "no network" promise returns', 'network-promise-is-true', s =>
    s.replace(/Setup fetches[^<]*/, 'No location, contacts, photos, or network.')],
  ['declined capability hidden', 'a11y-declines-capability', s =>
    s.replace(/nots:"could read what you typed — we do not"/, "nots:'nothing else on screen'")],
  ['notifications row removed', 'notifications-row-present', s =>
    s.replace(/\n  \{ id:'notify'[\s\S]*?\},/, '')],
  ['row renamed to a glossary term', 'row-name-is-not-a-glossary-term', s =>
    s.replace("name:'Dictation service'", "name:'Dictation'")],
  ['control shrunk under the 48dp floor', 'touch-target', s =>
    s.replace(/width: 48px; height: 48px;/, 'width: 18px; height: 18px;')],
  ['tools bar buttons shrunk', 'touch-target', s =>
    s.replace('.tools button {', '.tools button { min-height:0; min-width:0; padding:1px 4px;')],
  ['soft radius', 'radius-zero', s => s.replace('.cta {', '.cta { border-radius: 6px;')],
  ['a shadow appears', 'no-shadow', s => s.replace('.cta {', '.cta { box-shadow: 0 4px 0 rgba(0,0,0,.4);')],
  ['perimeter mask removed', 'perimeter-state-mark', s =>
    s.replace(/mask-image: conic-gradient\([^;]*\);/g, 'mask-image: none;')],
  ['perimeter paints over its own text', 'perimeter-does-not-fill', s =>
    s.replace(/(border: 2px solid var\(--accent\);\n)(\s*(-webkit-)?mask-image)/, '$1  background: var(--accent);\n$2')],
  ['CDN font link returns', 'font-not-from-cdn', s =>
    s.replace('<style>', '<link href="https://fonts.googleapis.com/css2?family=Archivo:wght@400&display=swap" rel="stylesheet">\n<style>')],
  ['font file path broken', 'font-face-file-exists', s =>
    s.replace('archivo-latin-var.woff2', 'archivo-latin-var-NOPE.woff2')],
  ['reduced-motion block deleted', 'reduced-motion-handled', s =>
    s.replace(/@media \(prefers-reduced-motion: reduce\) \{[\s\S]*?\n\}\n/, '')],
  ['limit clause set in the UI face', 'mono-for-limit-clause', s =>
    s.replace(/(\.row \.nots \{[^}]*?)font-family: var\(--font-mono\)/, '$1font-family: var(--font-ui)')],
  ['a second accent in one theme', 'single-accent-per-theme', s =>
    s.replace('--on-accent: #FFFFFF;', '--accent: #00707A;\n  --on-accent: #FFFFFF;')],
  ['CTA hover cross-fades its label', 'transition-midpoint', s =>
    s.replace('.cta:hover { background:', '.cta:hover { transition: background .3s linear, color .3s linear; background:')],
  ['granted row stops completing its sweep', 'perimeter-completes', s =>
    s.replace("(w * 360).toFixed(1)", "(w * 120).toFixed(1)")],
  ['a resting state held by a keyframe', 'no-attached-animation', s =>
    s.replace('.cta {', '.cta { animation: breathe 60s linear forwards;')],
];

const RUNTIME_DAMAGE = new Set(['contrast', 'touch-target', 'transition-midpoint', 'perimeter-completes', 'no-attached-animation']);

async function runAuditOn(file, docs, runtime) {
  const saved = { assertions: RESULT.assertions, failures: RESULT.failures, fired: RESULT.fired, all: RESULT.all };
  RESULT.assertions = 0; RESULT.failures = {}; RESULT.fired = {}; RESULT.all = {};
  const src = fs.readFileSync(file, 'utf8');
  staticChecks(src, docs, file);
  if (runtime) await runtimeAudit(file, ['dark']);
  const sub = { assertions: RESULT.assertions, failures: RESULT.failures, fired: RESULT.fired };
  Object.assign(RESULT, saved);
  return sub;
}

async function damageSuite(src, docs) {
  const dir = path.join(REPO, '.audit', 'damage');
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(dir, { recursive: true });
  const misses = [];
  const detail = [];
  let passed = 0;
  for (const [name, expect, mutate] of DAMAGE) {
    let damaged = mutate(src);
    if (damaged === src) { misses.push(`${name}: the mutation did not apply, so nothing was tested`); continue; }
    /* the damaged copies live outside the prototype directory, so point their
       font URLs at the committed files by absolute path */
    damaged = damaged.replace(/\.\.\/\.\.\/\.\.\/website\/fonts\//g, path.join(REPO, 'website/fonts') + '/');
    const file = path.join(dir, name.replace(/[^a-z0-9]+/gi, '_').toLowerCase() + '.html');
    fs.writeFileSync(file, damaged);
    const before = RESULT.assertions;
    const sub = await runAuditOn(file, docs, RUNTIME_DAMAGE.has(expect));
    RESULT.assertions = before + sub.assertions;
    const tagged = (RESULT.failures[expect] || []).concat(sub.failures[expect] || [])
      .filter(d => d.includes('[damage: ' + name + ']') || sub.failures[expect]);
    if ((sub.failures[expect] || []).length) {
      passed++; RESULT.fired[expect] = (RESULT.fired[expect] || 0) + 1;
      detail.push({ case: name, expect, fired: true, evidence: sub.failures[expect][0] });
    } else {
      misses.push(`${name}: expected "${expect}" to fire`);
      detail.push({ case: name, expect, fired: false });
    }
  }
  return { cases: DAMAGE.length, passed, misses, detail };
}

/* ── run ─────────────────────────────────────────────────────────────────── */
const src = fs.readFileSync(TARGET, 'utf8');
const info = staticChecks(src, DOCS, TARGET);
const unit = await resolverUnitTest();
const runtime = await runtimeAudit(TARGET);
let damage = { cases: 0, passed: 0, misses: ['skipped (--no-damage)'] };
if (!NO_DAMAGE) damage = await damageSuite(src, DOCS);

const report = {
  target: path.relative(REPO, TARGET),
  rows: info.rows, gating: info.gating,
  assertions: RESULT.assertions,
  failures: RESULT.failures,
  resolverUnitTest: unit,
  contrastRoles: (runtime.contrast || []).length,
  controlsProbed: (runtime['touch-target'] || []).length,
  midpoints: (runtime['transition-midpoint'] || []).length,
  damage,
  fired: RESULT.fired,
};
if (DUMP) report.all = RESULT.all;
console.log(JSON.stringify(report, null, 2));
if (JSON_OUT) fs.writeFileSync(JSON_OUT, JSON.stringify(report, null, 2));
process.exitCode = (Object.keys(RESULT.failures).length || damage.misses.length) ? 1 : 0;