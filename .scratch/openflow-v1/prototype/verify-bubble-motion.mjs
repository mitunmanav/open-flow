/* Verifier for bubble-motion.html (ticket 20). Headless, both themes.
   Run: node verify-bubble-motion.mjs [--damage]

   Every technique in here was added because the naive version of it PASSED on
   a broken file. Each is noted at the point it matters.

   Note on scratch: this lives in the worktree, not in /tmp, because a restart
   wiped an earlier copy of it and a verifier you cannot re-run is not
   evidence. It is a prototype harness, not product code. */
import { chromium } from '/home/mitun/.local/node22/lib/node_modules/@playwright/mcp/node_modules/playwright-core/index.mjs';
import { pathToFileURL } from 'node:url';
import path from 'node:path';

const HERE = '/home/mitun/open-flow/.worktrees/ticket-20-bubble-motion/.scratch/openflow-v1/prototype';
const FILE = pathToFileURL(path.join(HERE, 'bubble-motion.html')).href;
const DAMAGE = process.argv.includes('--damage');

/* ───────────────────────────────────────────────────────────────────────────
   Page-side helpers, injected before any page script.

   CONTRAST. `rgba(0,0,0,0).slice(0,3)` is "rgb(0, 0" — and naive parsers
   read a fully transparent layer as opaque black. That is how a translucent
   card gets audited at 1.06:1 in dark and 19:1 in light on the same code. So:
   parse properly, composite every translucent ancestor layer up the real
   chain, then measure. Alpha is carried, never dropped.
   ─────────────────────────────────────────────────────────────────────────── */
const PARSE = `
function parseColor(c){
  c = (c||'').trim();
  let m;
  if((m = c.match(/^rgba?\\(([^)]+)\\)$/))){
    const p = m[1].split(/[,\\s/]+/).filter(Boolean).map(Number);
    return {r:p[0],g:p[1],b:p[2],a:p.length>3?p[3]:1};
  }
  if((m = c.match(/^#([0-9a-f]{3,8})$/i))){
    let h = m[1];
    if(h.length===3||h.length===4) h = h.split('').map(x=>x+x).join('');
    return {r:parseInt(h.slice(0,2),16), g:parseInt(h.slice(2,4),16),
            b:parseInt(h.slice(4,6),16), a:h.length===8?parseInt(h.slice(6,8),16)/255:1};
  }
  return null;
}
function over(fg,bg){
  const a = fg.a + bg.a*(1-fg.a);
  if(a === 0) return {r:0,g:0,b:0,a:0};
  return { r:(fg.r*fg.a + bg.r*bg.a*(1-fg.a))/a,
           g:(fg.g*fg.a + bg.g*bg.a*(1-fg.a))/a,
           b:(fg.b*fg.a + bg.b*bg.a*(1-fg.a))/a, a };
}
function lum(c){
  const f = v => { v/=255; return v <= 0.03928 ? v/12.92 : Math.pow((v+0.055)/1.055, 2.4); };
  return 0.2126*f(c.r) + 0.7152*f(c.g) + 0.0722*f(c.b);
}
function ratio(a,b){
  const l1 = lum(a), l2 = lum(b);
  return (Math.max(l1,l2)+0.05)/(Math.min(l1,l2)+0.05);
}
/* The painting model, in two rules that a naive ancestor walk gets wrong in
   BOTH directions:

   1. AN ANCESTOR'S BACKGROUND ONLY PAINTS INSIDE ITS OWN PADDING BOX. The
      dot's and the ring's state words sit BELOW their mark: inside it in the
      DOM, outside it in the box model. Walking the DOM chain reported the
      mark's accent as the label's background and failed an 11px label that is
      genuinely on the page ground at ~17:1. So an ancestor layer is skipped
      unless its padding box contains the text's own rect.

   2. A POSITIONED DESCENDANT PAINTS OVER ITS ANCESTOR'S BACKGROUND. The chip
      paints its accent fill on a .face sibling of the label, because
      clip-path on the control clipped the control's own 48dp hit floor. An
      ancestor-only walk cannot see a sibling's paint, so the chip's white
      label read as white-on-ground at 1.10:1 while it is white-on-accent.
      So each ancestor's abspos/fixed descendants that cover the text are
      composited too, later DOM order on top.

   Both errors are in the direction of over-reporting a fault, which is the
   safe direction for a checker that is supposed to catch a change.
*/
function padBox(n){
  const r = n.getBoundingClientRect();
  const cs = getComputedStyle(n);
  const pl = parseFloat(cs.paddingLeft)||0, pr = parseFloat(cs.paddingRight)||0;
  const pt = parseFloat(cs.paddingTop)||0,  pb = parseFloat(cs.paddingBottom)||0;
  return {l:r.left+pl, t:r.top+pt, r:r.right-pr, b:r.bottom-pb};
}
function layerOf(n){
  const cs = getComputedStyle(n);
  let a = 1;
  const o = parseFloat(cs.opacity);
  if(!Number.isNaN(o)) a *= o;
  const c = parseColor(cs.backgroundColor);
  if(!c || c.a === 0) return a < 1 ? {r:0,g:0,b:0,a:1-a} : null;
  return {r:c.r, g:c.g, b:c.b, a:c.a*a};
}
function covers(box, rect){
  return rect.left >= box.l - 0.5 && rect.right <= box.r + 0.5 &&
         rect.top >= box.t - 0.5 && rect.bottom <= box.b + 0.5;
}
function effBg(el){
  const rect = el.getBoundingClientRect();
  const stack = [];
  let n = el;
  while(n && n.nodeType === 1){
    const box = padBox(n);
    if (covers(box, rect)) {
      const own = layerOf(n);
      if (own) stack.push(own);
      /* positioned descendants of this level paint over it, in DOM order */
      for (const d of n.querySelectorAll('*')) {
        if (n.contains(el) && (d === el || d.contains(el))) continue; /* own subtree */
        const dcs = getComputedStyle(d);
        if (dcs.position !== 'absolute' && dcs.position !== 'fixed') continue;
        if (dcs.display === 'none' || dcs.visibility === 'hidden') continue;
        const dr = d.getBoundingClientRect();
        if (dr.width < 1 || dr.height < 1) continue;
        const dl = layerOf(d);
        if (dl) stack.push(dl);
      }
    }
    n = n.parentElement;
  }
  let base = {r:255,g:255,b:255,a:1};
  for(let i = stack.length - 1; i >= 0; i--) base = over(stack[i], base);
  return base;
}
function maxPx(el){
  let m = 0;
  for(const n of el.querySelectorAll('*')){
    if(!(n instanceof HTMLElement)) continue;
    if(!n.textContent.trim()) continue;
    const cs = getComputedStyle(n);
    if(cs.visibility === 'hidden' || cs.display === 'none') continue;
    const r = n.getBoundingClientRect();
    if(r.width < 1 || r.height < 1) continue;
    const bold = (parseInt(cs.fontWeight,10) || 400) >= 700;
    m = Math.max(m, parseFloat(cs.fontSize));
  }
  const cs = getComputedStyle(el);
  const r = el.getBoundingClientRect();
  if(m === 0 && r.width > 0 && r.height > 0) m = parseFloat(cs.fontSize);
  return m;
}
function auditText(){
  const out = [];
  for(const el of document.querySelectorAll('body *')){
    if(!(el instanceof HTMLElement)) continue;
    const cs = getComputedStyle(el);
    if(cs.display === 'none' || cs.visibility === 'hidden') continue;
    const own = Array.from(el.childNodes).some(c => c.nodeType === 3 && c.textContent.trim());
    if(!own) continue;
    const r = el.getBoundingClientRect();
    if(r.width < 1 || r.height < 1) continue;
    const px = maxPx(el);
    const bold = (parseInt(cs.fontWeight,10) || 400) >= 700;
    const need = (px >= 24 || (bold && px >= 18.66)) ? 3.0 : 4.5;
    const fg = parseColor(cs.color);
    if(!fg) continue;
    const bg = effBg(el);
    out.push({
      sel: el.tagName.toLowerCase() + (el.id ? '#'+el.id : '') +
           (typeof el.className === 'string' && el.className.trim()
             ? '.' + el.className.trim().split(/\\s+/)[0] : '') +
           ' > ' + el.textContent.trim().slice(0,26),
      r: ratio(fg.a < 1 ? over(fg,bg) : fg, bg),
      need, px, fg: cs.color,
      bg: 'rgb(' + Math.round(bg.r) + ',' + Math.round(bg.g) + ',' + Math.round(bg.b) + ')',
      op: cs.opacity
    });
  }
  return out;
}
/* HIT-TEST BY PROBE. Padding arithmetic is not evidence: a ::after's hit area
   never appears in offsetHeight or in getBoundingClientRect of the mark, and
   that is the entire reason the floor is a pseudo-element at all.

   Three corrections, each of which first made this check unfalsifiable:
   1. Walk outward from the CENTRE, and take the run of CONSECUTIVE answers.
      Unioning every answered ring step stitched a false target across gaps.
   2. Only the element or a DESCENDANT counts as answering. Counting ancestors
      meant every point over a rig cell "answered", so the walk could never
      fail however small the mark was.
   3. Sampling on integer offsets, N answers from -a..+b span a+b+1 px.
      Reporting (b-a) undercounts every target by one, which turns a real
      48px floor into a phantom 47px failure. */
function probeHit(){
  const out = [];
  for(const el of document.querySelectorAll('button,[role="button"],a[href]')){
    /* Two scrolls, both required. scrollIntoView(block:'center') walks every
       scrollable ancestor, which is how a control inside the harness bar's
       OWN overflow scroller gets revealed; then a window scroll re-centres
       it, because scrollIntoView leaves a control at an edge and the edge is
       where the fixed bar is. Measuring at an edge measures the harness. */
    el.scrollIntoView({block:'center', inline:'center'});
    let r = el.getBoundingClientRect();
    window.scrollBy(0, (r.top + r.height/2) - window.innerHeight/2);
    r = el.getBoundingClientRect();
    if(r.width < 1 || r.height < 1) continue;
    const cx = r.left + r.width/2, cy = r.top + r.height/2;
    const name = el.tagName.toLowerCase() + (el.id ? '#'+el.id : '.' + String(el.className).split(' ')[0]);
    function answers(px,py){
      const e = document.elementFromPoint(px,py);
      return !!e && (e === el || el.contains(e));
    }
    if(!answers(cx,cy)){ out.push({sel:'PROBE-CENTRE-MISS '+name, w:0, h:0, mark:'0x0'}); continue; }
    const edge = {};
    for(const [k,dx,dy] of [['l',-1,0],['r',1,0],['t',0,-1],['b',0,1]]){
      let d = 0;
      for(; d <= 96; d += 1){ if(!answers(cx+dx*d, cy+dy*d)) break; }
      edge[k] = d - 1;
    }
    out.push({sel:name, w: edge.l + edge.r + 1, h: edge.t + edge.b + 1,
              mark: Math.round(r.width) + 'x' + Math.round(r.height)});
  }
  return out;
}
function dupIds(){
  const seen = new Map();
  for(const el of document.querySelectorAll('[id]')){
    if(!el.id) continue;
    seen.set(el.id, (seen.get(el.id)||0)+1);
  }
  return [...seen].filter(function(e){return e[1] > 1;}).map(function(e){return e[0]+' x'+e[1];});
}
/* Two evidences, because a nowrap label inside a fixed-width box has a
   perfectly in-bounds rect and still spills. scrollWidth is what catches it.
   A deliberate internal scroller is not page overflow. */
function overflowX(){
  const cw = document.documentElement.clientWidth;
  const who = [...document.querySelectorAll('body *')].filter(function(e){
    if(e.getClientRects().length === 0) return false;
    const r = e.getBoundingClientRect();
    if(r.width < 1) return false;
    const cs = getComputedStyle(e);
    if(cs.overflowX === 'auto' || cs.overflowX === 'scroll') return false;
    return r.right > cw + 1 || e.scrollWidth > e.clientWidth + 1;
  }).slice(0,6).map(function(e){
    return e.tagName.toLowerCase() + (e.id ? '#'+e.id : '.' + String(e.className).split(' ')[0]);
  });
  return {sw: document.documentElement.scrollWidth, cw, who};
}
const DECLARED = ['breathe','pull-in','thicken','march','march-mid','arc-step',
                  'flash-out','tick-draw','sheet-in','mark-in'];
function restingAnims(){
  const bad = [], loops = [], trans = [];
  for(const el of document.querySelectorAll('body, body *')){
    if(!el.getAnimations) continue;
    for(const a of el.getAnimations()){
      const tm = (a.effect && a.effect.getTiming) ? a.effect.getTiming() : {};
      const nm = a.animationName || 'transition';
      const where = el.tagName.toLowerCase() + (el.id ? '#'+el.id : '.' + String(el.className).split(' ')[0]);
      if(tm.fill === 'forwards' || tm.fill === 'both'){
        bad.push(where + ' fill=' + tm.fill + ' name=' + nm);
      } else if(nm === 'transition') {
        trans.push(where);
      } else {
        loops.push(nm);
        if(!DECLARED.includes(nm)) bad.push('UNDECLARED ' + where + ' ' + nm);
      }
    }
  }
  return {bad, loops: [...new Set(loops)], trans};
}
function anyAnim(){
  const out = [];
  for(const el of document.querySelectorAll('body, body *')){
    if(!el.getAnimations) continue;
    for(const a of el.getAnimations()){
      out.push(el.tagName.toLowerCase() + (el.id ? '#'+el.id : '') + ':' + (a.animationName || 'transition'));
    }
  }
  return out;
}
function flashLeftovers(){ return document.querySelectorAll('.flash').length; }
/* The compositing model is only valid while nothing paints behind text that
   it cannot account for. If something does, the contrast numbers are fiction. */
function unpaintable(){
  return [...document.querySelectorAll('body *')].filter(function(e){
    const cs = getComputedStyle(e);
    return (cs.backdropFilter && cs.backdropFilter !== 'none') ||
           (cs.backgroundImage && cs.backgroundImage !== 'none') ||
           (cs.webkitBackdropFilter && cs.webkitBackdropFilter !== 'none');
  }).map(function(e){ return e.tagName.toLowerCase() + (e.id ? '#'+e.id : ''); });
}
/* Nothing resting on a hidden rule: an element that is invisible at rest and
   was only ever made visible by an animation is a state that vanishes if the
   animation is suppressed. Under reduced motion there are no animations, so
   any zero-opacity TEXT is a text nobody can read. */
function hiddenText(){
  const out = [];
  for(const el of document.querySelectorAll('body *')){
    if(!(el instanceof HTMLElement)) continue;
    const own = Array.from(el.childNodes).some(c => c.nodeType === 3 && c.textContent.trim());
    if(!own) continue;
    const r = el.getBoundingClientRect();
    if(r.width < 1 || r.height < 1) continue;
    const cs = getComputedStyle(el);
    if(parseFloat(cs.opacity) < 0.99 || parseFloat(cs.visibility) < 0.99){
      out.push(el.tagName.toLowerCase() + (el.id ? '#'+e.id : '') + ' op=' + cs.opacity +
               ' vis=' + cs.visibility + ' "' + el.textContent.trim().slice(0,20) + '"');
    }
  }
  return out;
}
window.__t = {auditText, probeHit, dupIds, overflowX, restingAnims, anyAnim,
              flashLeftovers, unpaintable, hiddenText};
`;

const A = [];
const ok = (name, cond, detail) =>
  A.push({ name, pass: !!cond, detail: detail === undefined ? '' : String(detail) });

const STATES = ['idle', 'listening', 'processing', 'done', 'problem', 'cancelled'];

async function newPage(browser, { theme, fs, motion }) {
  const ctx = await browser.newContext({
    viewport: { width: 390, height: 844 },
    reducedMotion: motion === 'reduce' ? 'reduce' : 'no-preference',
    deviceScaleFactor: 1,
  });
  const page = await ctx.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('console: ' + m.text()); });
  /* No addInitScript for theme or font scale: the page reads both off its own
     URL, and an init script also runs against about:blank where
     documentElement is null — which threw, and looked like a page fault. */
  await page.addInitScript(PARSE);
  await page.goto(FILE + '?theme=' + (theme || 'light') + '&fs=' + (fs || '100'), { waitUntil: 'load' });
  await page.waitForTimeout(700);           /* let the entrance settle */
  return { ctx, page, errors };
}

const browser = await chromium.launch();
let failures = 0;

/* ══════════ PASS 1 — full motion, both themes, 100% and 200% font ══════════ */
for (const theme of ['light', 'dark']) {
  for (const fs of ['100', '200']) {
    const { ctx, page, errors } = await newPage(browser, { theme, fs });
    const tag = theme + '/' + fs + '%';

    /* Hover every control before auditing. A label's contrast with a pointer
       over it is a different label from the same label at rest, and hovering
       is the default state of a click. A selected button whose :hover
       out-specified its own selection rendered ink-on-ink at 1.52:1 this way. */
    await page.evaluate(() => {
      for (const el of document.querySelectorAll('button,[role="button"],a[href]')) {
        el.dispatchEvent(new MouseEvent('mouseover', { bubbles: true }));
      }
    });

    for (const st of STATES) {
      await page.click('.tools [data-state="' + st + '"]');
      await page.waitForTimeout(150);

      const text = await page.evaluate(() => window.__t.auditText());
      for (const t of text) {
        ok(`AA ${tag} ${st} ${t.sel}`, t.r >= t.need,
          `${t.r.toFixed(2)}:1 need ${t.need} @${t.px.toFixed(1)}px fg=${t.fg} bg=${t.bg} op=${t.op}`);
      }
      if (st === 'idle') ok(`AA ${tag} text roles audited`, text.length > 12, `${text.length} roles`);

      const restS = await page.evaluate(() => window.__t.restingAnims());
      ok(`REST ${tag}/${st} nothing rests on a keyframe`, restS.bad.length === 0, restS.bad.join(','));

      /* A terminal's one-shot must be GONE once it has played, not parked in
         the DOM waiting for someone to notice. Checked only after the 700ms
         backstop has passed — asserting at 150ms would be asserting that the
         animation has not started yet. */
      if (st === 'done' || st === 'problem' || st === 'cancelled') {
        await page.waitForTimeout(800);
        const flS = await page.evaluate(() => window.__t.flashLeftovers());
        ok(`ONESHOT ${tag}/${st} one-shot cleaned up`, flS === 0, `${flS} leftovers`);
      }
    }

    /* Re-enter a terminal and confirm the 260ms one-shot is GONE from the DOM
       rather than parked there invisible. */
    await page.click('.tools [data-state="done"]');
    await page.waitForTimeout(50);
    const during = await page.evaluate(() => window.__t.flashLeftovers());
    ok(`ONESHOT ${tag} flash exists while it plays`, during >= 1, `${during} during`);
    await page.waitForTimeout(900);
    const after = await page.evaluate(() => window.__t.flashLeftovers());
    ok(`ONESHOT ${tag} flash removed after it plays`, after === 0, `${after} after`);

    await page.click('.tools [data-state="idle"]');
    const hits = await page.evaluate(() => window.__t.probeHit());
    for (const h of hits) {
      ok(`HIT ${tag} ${h.sel}`, h.w >= 48 && h.h >= 48,
        `${h.w}x${h.h} (mark ${h.mark})`);
    }
    ok(`HIT ${tag} controls probed`, hits.length >= 12, `${hits.length} controls`);

    const of = await page.evaluate(() => window.__t.overflowX());
    ok(`OVERFLOW ${tag}`, of.sw <= of.cw + 1, `scrollW ${of.sw} > clientW ${of.cw} ${of.who.join(',')}`);

    const dups = await page.evaluate(() => window.__t.dupIds());
    ok(`IDS ${tag} unique`, dups.length === 0, dups.join(','));

    ok(`SCRIPT ${tag} no errors`, errors.length === 0, errors.join(' | '));

    const unp = await page.evaluate(() => window.__t.unpaintable());
    ok(`CONTRAST-MODEL ${tag} compositable`, unp.length === 0, unp.join(','));

    await ctx.close();
  }
}

/* ══════════ PASS 2 — reduced motion, both themes, every state ══════════ */
for (const theme of ['light', 'dark']) {
  const { ctx, page, errors } = await newPage(browser, { theme, fs: '100', motion: 'reduce' });

  for (const st of STATES) {
    await page.click('.tools [data-state="' + st + '"]');
    await page.waitForTimeout(250);
    const anims = await page.evaluate(() => window.__t.anyAnim());
    ok(`REDUCED ${theme}/${st} zero animations on every element`, anims.length === 0,
      anims.slice(0, 6).join(',') + (anims.length > 6 ? ` +${anims.length - 6}` : ''));
    const fl = await page.evaluate(() => window.__t.flashLeftovers());
    ok(`REDUCED ${theme}/${st} no one-shot node created`, fl === 0, `${fl}`);
    /* A state that is only legible while moving is a state that does not
       exist for some people. Audit the resting mark at AA. */
    const text = await page.evaluate(() => window.__t.auditText());
    for (const t of text) {
      ok(`AA-REDUCED ${theme}/${st} ${t.sel}`, t.r >= t.need,
        `${t.r.toFixed(2)}:1 need ${t.need} fg=${t.fg} bg=${t.bg}`);
    }
    const hid = await page.evaluate(() => window.__t.hiddenText());
    ok(`HIDDEN ${theme}/${st} no text resting invisible`, hid.length === 0, hid.join(','));
  }

  const rest = await page.evaluate(() => window.__t.restingAnims());
  ok(`REDUCED ${theme} nothing rests on a keyframe`, rest.bad.length === 0, rest.bad.join(','));
  ok(`REDUCED ${theme} no script errors`, errors.length === 0, errors.join(' | '));

  /* The ring's geometries, asserted as geometry rather than eyeballed. The
     state loop above leaves the bubble on `cancelled`, and a terminal
     auto-returns, so each read parks the state it is reading first. */
  const readRing = async (st, field) => {
    await page.click('.tools [data-state="' + st + '"]');
    await page.waitForTimeout(180);
    return page.evaluate(field);
  };
  const idleRing = await readRing('idle', () => {
    const b = document.querySelector('.s-ring[data-state="idle"]');
    const a = getComputedStyle(b.querySelector('.arc'));
    const t = getComputedStyle(b.querySelector('.track'));
    return { dash: a.strokeDasharray, sw: a.strokeWidth, anim: a.animationName,
             trackSw: t.strokeWidth, trackStroke: t.stroke };
  });
  /* Idle is a CLOSED circle: a full track at hairline and an undashed arc, so
     there is no gap anywhere and nothing is claimed as in-progress. A dashed
     arc at rest reads as a stalled progress bar. */
  ok(`GEOM idle ring is a closed circle`,
    idleRing.trackSw === '1px' && idleRing.dash === 'none' && idleRing.trackStroke !== 'none',
    `track ${idleRing.trackSw} arc ${idleRing.dash} anim ${idleRing.anim}`);

  /* And the arc must NOT be closed in the two states that have work. */
  const busyRing = await readRing('processing', () => {
    const b = document.querySelector('.s-ring[data-state="processing"]');
    return { dash: getComputedStyle(b.querySelector('.arc')).strokeDasharray };
  });
  ok(`GEOM processing ring is a partial arc, not closed`,
    busyRing.dash.includes('12') && busyRing.dash.includes('71'), busyRing.dash);

  await page.click('.tools [data-state="problem"]');
  await page.waitForTimeout(200);
  const pr = await page.evaluate(() => {
    const b = document.querySelector('.s-ring[data-state="problem"]');
    const cs = getComputedStyle(b.querySelector('.arc'));
    return { dash: cs.strokeDasharray, anim: cs.animationName,
             word: b.querySelector('.word').textContent };
  });
  ok(`GEOM problem ring keeps a STATIC gap`, pr.dash.includes('8') && pr.dash.includes('24'), pr.dash);
  ok(`GEOM problem ring is not animating`, pr.anim === 'none', pr.anim);
  ok(`GEOM problem carries the word`, pr.word === 'Problem', pr.word);

  await page.click('.tools [data-state="done"]');
  await page.waitForTimeout(200);
  const dn = await page.evaluate(() => {
    const b = document.querySelector('.s-ring[data-state="done"]');
    const p = b.querySelector('.mark path');
    return { off: getComputedStyle(p).strokeDashoffset, anim: getComputedStyle(p).animationName };
  });
  ok(`GEOM done tick rests drawn, not animated`,
    dn.anim === 'none' && parseFloat(dn.off) === 0, `off=${dn.off} anim=${dn.anim}`);

  /* Legible with the accent removed entirely — the honesty rule, as a test. */
  await page.click('[data-ink]');
  await page.click('.tools [data-state="processing"]');
  await page.waitForTimeout(200);
  const noAccent = await page.evaluate(() => window.__t.auditText());
  for (const t of noAccent) {
    ok(`NO-ACCENT ${theme}/${t.sel}`, t.r >= t.need,
      `${t.r.toFixed(2)}:1 need ${t.need} fg=${t.fg} bg=${t.bg}`);
  }
  const shapeWord = await page.evaluate(() => {
    const w = [...document.querySelectorAll('.rig .bubble .word')].map(e => e.textContent);
    const m = [...document.querySelectorAll('.rig .s-chip .march')].length;
    return { w, march: m };
  });
  ok(`NO-ACCENT ${theme} every shape names the state`,
    shapeWord.w.length === 4 && shapeWord.w.every(t => t === 'Processing'),
    shapeWord.w.join('|'));

  await ctx.close();
}

/* ══════════ PASS 3 — damage. A checker that has never seen a failure has
   not been shown it can fail. One injected fault per check. ══════════ */
if (DAMAGE) {
  const FAULTS = [
    ['AA', 'dim a text role to 2.1:1', `
       document.querySelector('.sheethead p').style.color = 'rgba(10,10,10,0.06)'`,
     r => r.some(t => t.name.startsWith('AA') && !t.pass)],
    ['AA', 'a translucent layer whose alpha a slicing parser would drop', `
       document.querySelector('.dragzone').style.background = 'rgba(10,10,10,0.06)';
       document.querySelector('.dragzone .dzlabel').style.color = 'rgba(10,10,10,0.30)'`,
     r => r.some(t => t.name.startsWith('AA') && !t.pass && /rgba/.test(t.detail))],
    ['AA', 'opacity on a label — the property backgroundColor never shows', `
       document.querySelector('.dragzone .readout').style.opacity = '0.22'`,
     r => r.some(t => t.name.startsWith('AA') && !t.pass && /op=0/.test(t.detail))],
    ['HIT', 'remove the pseudo-element floor from the 40px dot', `
       const s = document.createElement('style');
       s.textContent = '.s-dot::after{display:none!important}';
       document.head.appendChild(s);`,
     r => r.some(t => t.name.startsWith('HIT') && !t.pass)],
    ['HIT', 'clip-path back onto the control, cutting its own hit floor', `
       const s = document.createElement('style');
       s.textContent = '.s-ring{clip-path:polygon(0 0,calc(100% - 22px) 0,100% 22px,100% 100%,22px 100%,0 calc(100% - 22px))}';
       document.head.appendChild(s);`,
     r => r.some(t => t.name.startsWith('HIT') && !t.pass)],
    ['OVERFLOW', 'a nowrap label in a fixed-width box', `
       document.getElementById('sheet').style.width = '900px';
       document.querySelector('.s-chip .word').style.cssText =
         'white-space:nowrap;overflow-wrap:normal;width:400px';`,
     r => r.some(t => t.name.startsWith('OVERFLOW') && !t.pass)],
    ['IDS', 'clone a rig, id and all, into the tree', `
       document.getElementById('rigs').appendChild(
         document.getElementById('rig0').parentNode.cloneNode(true));`,
     r => r.some(t => t.name.startsWith('IDS') && !t.pass)],
    ['SCRIPT', 'throw from a click handler', `
       document.addEventListener('click', function(){
         setTimeout(function(){ throw new Error('injected'); }, 0);
       }, {once:true});
       document.querySelector('.tools button').click();`,
     r => r.some(t => t.name.startsWith('SCRIPT') && !t.pass)],
    ['REDUCED', 'one animation that outlives the reduced-motion class', `
       const s = document.createElement('style');
       s.textContent = '.s-dot .dotmark{animation:breathe 4s infinite!important}';
       document.head.appendChild(s);`,
     r => r.some(t => t.name.startsWith('REDUCED') && !t.pass)],
    ['REDUCED', 'the override trick: .01ms duration still attaches an Animation', `
       const s = document.createElement('style');
       s.textContent = '*{animation-duration:.01ms!important;animation-iteration-count:1!important}';
       document.head.appendChild(s);`,
     r => r.some(t => t.name.startsWith('REDUCED') && !t.pass)],
    ['REST', 'hold a resting state on animation-fill-mode: forwards', `
       const s = document.createElement('style');
       s.textContent = '.s-ring .arc{animation:arc-step 1600s steps(1,end) forwards!important}';
       document.head.appendChild(s);`,
     r => r.some(t => t.name.startsWith('REST') && !t.pass)],
    ['ONESHOT', 'a one-shot element left in the DOM after it plays', `
       document.querySelector('.s-pill').insertAdjacentHTML('beforeend',
         '<span class="flash" style="position:static;opacity:1;width:0;height:0"></span>');`,
     r => r.some(t => t.name.startsWith('ONESHOT') && !t.pass)],
    ['CONTRAST-MODEL', 'a backdrop-filter the compositing model cannot see through', `
       document.querySelector('.sheet').style.backdropFilter = 'blur(4px)';`,
     r => r.some(t => t.name.startsWith('CONTRAST-MODEL') && !t.pass)],
    ['HIDDEN', 'text that only an animation ever made visible', `
       const s = document.createElement('style');
       s.textContent = '.stateline .adr{opacity:0}';
       document.head.appendChild(s);`,
     r => r.some(t => t.name.startsWith('HIDDEN') && !t.pass)],
  ];

  for (const [id, label, inject, fired] of FAULTS) {
    const ctx = await browser.newContext({ viewport: { width: 390, height: 844 },
                                           reducedMotion: 'reduce' });
    const p2 = await ctx.newPage();
    const p2err = [];
    p2.on('pageerror', e => p2err.push(e.message));
    p2.on('console', m => { if (m.type() === 'error') p2err.push(m.text()); });
    await p2.addInitScript(PARSE);
    await p2.goto(FILE + '?theme=light&fs=100', { waitUntil: 'load' });
    await p2.waitForTimeout(400);
    await p2.evaluate(inject);
    await p2.waitForTimeout(600);

    const res = [];
    for (const t of await p2.evaluate(() => window.__t.auditText())) {
      res.push({ name: 'AA ' + t.sel, pass: t.r >= t.need,
                 detail: `${t.r.toFixed(2)}:1 need ${t.need} fg=${t.fg} bg=${t.bg} op=${t.op}` });
    }
    for (const h of await p2.evaluate(() => window.__t.probeHit())) {
      res.push({ name: 'HIT ' + h.sel, pass: h.w >= 48 && h.h >= 48,
                 detail: `${h.w}x${h.h} (mark ${h.mark})` });
    }
    const of = await p2.evaluate(() => window.__t.overflowX());
    res.push({ name: 'OVERFLOW', pass: of.sw <= of.cw + 1,
               detail: `scrollW ${of.sw} > clientW ${of.cw} ${of.who.join(',')}` });
    const d = await p2.evaluate(() => window.__t.dupIds());
    res.push({ name: 'IDS', pass: d.length === 0, detail: d.join(',') });
    const rest = await p2.evaluate(() => window.__t.restingAnims());
    res.push({ name: 'REST', pass: rest.bad.length === 0, detail: rest.bad.join(',') });
    const an = await p2.evaluate(() => window.__t.anyAnim());
    res.push({ name: 'REDUCED', pass: an.length === 0, detail: an.slice(0, 4).join(',') });
    const fl = await p2.evaluate(() => window.__t.flashLeftovers());
    res.push({ name: 'ONESHOT', pass: fl === 0, detail: String(fl) });
    const unp = await p2.evaluate(() => window.__t.unpaintable());
    res.push({ name: 'CONTRAST-MODEL', pass: unp.length === 0, detail: unp.join(',') });
    const hid = await p2.evaluate(() => window.__t.hiddenText());
    res.push({ name: 'HIDDEN', pass: hid.length === 0, detail: hid.join(',') });
    res.push({ name: 'SCRIPT', pass: p2err.length === 0, detail: p2err.join(' | ') });

    ok(`DAMAGE/${id} fires — ${label}`, fired(res),
      fired(res) ? 'detected' : 'MISSED; available: ' +
        res.filter(x => !x.pass).map(x => x.name).slice(0, 5).join(', '));
    await ctx.close();
  }
}

await browser.close();

for (const a of A) if (!a.pass) failures++;
const byKind = {};
for (const a of A) {
  const k = a.name.split(' ')[0];
  byKind[k] = byKind[k] || { n: 0, bad: 0 };
  byKind[k].n++; if (!a.pass) byKind[k].bad++;
}
console.log(`\nASSERTIONS: ${A.length}   FAILING: ${failures}`);
for (const [k, v] of Object.entries(byKind)) {
  console.log(`  ${k.padEnd(16)} ${String(v.n).padStart(5)} checked, ${v.bad} failing`);
}
if (failures) {
  console.log('\nFAILURES:');
  for (const a of A) if (!a.pass) console.log(`  x ${a.name} — ${a.detail}`);
}
process.exit(failures ? 1 : 0);
