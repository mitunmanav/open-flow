import { chromium } from '/home/mitun/career-ops/node_modules/playwright/index.mjs';
const f='/home/mitun/open-flow/.worktrees/ticket-55-permissions-list/.scratch/openflow-v1/prototype/onboarding.html';
const b=await chromium.launch(); const p=await b.newPage({viewport:{width:430,height:932}});
await p.goto('file://'+f); await p.evaluate(()=>window.setTheme('dark')); await p.waitForTimeout(300);
console.log(await p.evaluate(()=>{
  const el=document.querySelector('.cta');
  const r=el.getBoundingClientRect();
  return {active: el.matches(':active'), hover: el.matches(':hover'), focus: el.matches(':focus'),
          focusWithin: el.matches(':focus-within'), rect:[r.left|0,r.top|0,r.width|0,r.height|0],
          activeEl: document.activeElement.tagName+'.'+document.activeElement.className,
          bg: getComputedStyle(el).backgroundColor};
}));
await b.close();
