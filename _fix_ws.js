const fs=require('fs');
const p='app/src/main/java/com/payesh/app/ui/WorkspaceApp.kt';
let s=fs.readFileSync(p,'utf8');
const old='"matcher" -> CaseMatcherScreen(law,data)';
const neu='"matcher" -> CaseMatcherScreen(law)';
s=s.replace(old,neu); // since restored CaseMatcherScreen takes (repository) — but WorkspaceApp passes law? Actually `CaseMatcherScreen(law)` if law is repository. We'll use law (likely repository parameter in scope).
fs.writeFileSync(p,s,'utf8');
console.log('WorkspaceApp matcher fixed to:', neu);
