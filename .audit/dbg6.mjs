import { chromium } from '/home/mitun/career-ops/node_modules/playwright/index.mjs';
const f='/home/mitun/open-flow/.worktrees/ticket-55-permissions-list/.scratch/openflow-v1/prototype/onboarding.html';
const b=await chromium.launch(); const p=await b.newPage({viewport:{width:430,height:932}});
await p.goto('file://'+f); await p.evaluate(()=>window.setTheme('dark')); await p.waitForTimeout(300);
console.log(await p.evaluate(()=>{
  const out={sheets:document.styleSheets.length};
  let rule=null;
  for(const sheet of document.styleSheets){ let rules; try{rules=sheet.cssRules}catch(e){out.err=String(e).slice(0,60);continue}
    rule=[...rules].find(r=>r.selectorText==='.cta:hover'); if(rule) out.foundIn=rules.length; }
  if(!rule) return out;
  const cssText=rule.style.cssText;
  const el=document.querySelector('.cta');
  const id='data-probe-x';
  const st=document.createElement('style');
  st.textContent='['+id+'] { '+cssText+' }';
  document.head.appendChild(st);
  el.setAttribute(id,'');
  const after=getComputedStyle(el);
  out.cssText=cssText; out.injected=st.textContent;
  out.bg=after.backgroundColor; out.color=after.color;
  out.matches=el.matches('['+id+']');
  out.accBefore=after.backgroundColor;
  el.removeAttribute(id); st.remove();
  out.afterRemove=getComputedStyle(el).backgroundColor;
  return out;
}));
await b.close();
