const fs=require('fs');
const p='app/src/main/java/com/payesh/app/ui/WorkspaceApp.kt';
let s=fs.readFileSync(p,'utf8');
const before=s;
s=s.replace('"matcher" -> CaseMatcherScreen(law)','"matcher" -> CaseMatcherScreen(law)');
// find the matcher line and print it
const lines=s.split('\n');
lines.forEach((l,i)=>{ if(l.includes('matcher')) console.log((i+1)+': '+l); });
// Fix: CaseMatcherScreen takes only (repository) in restored version -> call with law.repository
s=s.replace(/"matcher" -> CaseMatcherScreen\(law\)/,'"matcher" -> CaseMatcherScreen(law.repository)');
fs.writeFileSync(p,s,'utf8');
console.log('changed='+(before!==s));
const l2=fs.readFileSync(p,'utf8').split('\n');
l2.forEach((l,i)=>{ if(l.includes('CaseMatcherScreen')) console.log('NOW '+(i+1)+': '+l); });
