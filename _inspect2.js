const fs=require('fs');
const f='app/src/main/java/com/payesh/app/ui/screens/CaseMatcherScreen.kt';
const s=fs.readFileSync(f,'utf8');
s.split('\n').forEach((l,i)=>{ if(l.includes('fun CaseMatcherScreen')) console.log('DECL '+(i+1)+': '+l.trim()); });
const ws='app/src/main/java/com/payesh/app/ui/WorkspaceApp.kt';
fs.readFileSync(ws,'utf8').split('\n').forEach((l,i)=>{ if(l.includes('CaseMatcherScreen')) console.log('WS '+(i+1)+': '+l.trim()); });
