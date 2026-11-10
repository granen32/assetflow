"use client";
import { useState } from "react";
import ProfileEditor from "@/components/ProfileEditor";
export default function Preview() {
 const [open, setOpen] = useState(true);
 const [fail, setFail] = useState(false);
 if (process.env.NODE_ENV !== "development") return null;
 return <main><p>개발용 미리보기 · DB에 저장되지 않습니다.</p><label><input type="checkbox" checked={fail} onChange={e => setFail(e.target.checked)}/>실패 응답</label><button onClick={() => setOpen(true)}>프로필 열기</button>{open && <ProfileEditor member={{id:0,email:"preview@example.com",nickname:"학습자"}} onClose={() => setOpen(false)} onSave={async nickname => { await new Promise(resolve => setTimeout(resolve,800)); if(fail) throw new Error("미리보기 저장 실패"); return {id:0,email:"preview@example.com",nickname:nickname.trim()}; }}/>}</main>;
}
