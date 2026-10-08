import { chromium } from '/home/mitun/career-ops/node_modules/playwright/index.mjs';
const f='/home/mitun/open-flow/.worktrees/ticket-55-permissions-list/.scratch/openflow-v1/prototype/onboarding.html';
const b=await chromium.launch(); const p=await b.newPage({viewport:{width:430,height:932}});
p.on('pageerror',e=>console.log('ERR',e.message));
await p.goto('file://'+f); await p.evaluate(()=>window.setTheme('dark')); await p.waitForTimeout(300);
console.log(await p.evaluate(()=>{
  const out={};
  out.before=getComputedStyle(document.body).backgroundColor;
  const s=document.createElement('style'); s.textContent='body{background:#00ff00}'; document.head.appendChild(s);
  out.afterBody=getComputedStyle(document.body).backgroundColor;
  out.sheetsNow=document.styleSheets.length;
  const d=document.createElement('style'); d.textContent='.cta{background:#0000ff}'; document.head.appendChild(d);
  out.cta=getComputedStyle(document.querySelector('.cta')).backgroundColor;
  out.ctaHTML=document.querySelector('.cta').outerHTML.slice(0,120);
  out.ctaRules=[...document.styleSheets].map(sh=>{try{return [...sh.cssRules].filter(r=>r.selectorText&&r.selectorText.includes('cta')).map(r=>r.selectorText)}catch(e){return 'x'}});
  return out;
}));
await b.close();
