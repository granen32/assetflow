"use client";
import { useEffect, useRef, useState } from "react";
import type { Member } from "@/lib/api";

export default function ProfileEditor({ member, onSave, onClose }: { member: Member; onSave: (nickname: string) => Promise<Member>; onClose: () => void }) {
  const [nickname, setNickname] = useState(member.nickname);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const dialog = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    dialog.current?.querySelector("input")?.focus();
    return () => previous?.focus();
  }, []);
  return <div className="modal-backdrop"><div ref={dialog} className="modal-card" role="dialog" aria-modal="true" aria-labelledby="profile-title" onKeyDown={event => {
    if (event.key === "Escape" && !saving) onClose();
    if (event.key === "Tab") {
      const fields = Array.from(dialog.current?.querySelectorAll<HTMLElement>("input:not(:disabled),button:not(:disabled)") ?? []);
      const first = fields[0], last = fields[fields.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    }
  }}><h2 id="profile-title">프로필 수정</h2><form className="modal-form" onSubmit={async event => {
    event.preventDefault();
    if (!nickname.trim() || nickname.length > 80) { setMessage("닉네임은 공백만 입력할 수 없으며 최대 80자입니다."); return; }
    setSaving(true); setMessage("");
    try { const updated = await onSave(nickname); setNickname(updated.nickname); setMessage("닉네임을 저장했습니다."); }
    catch (error) { setMessage(error instanceof Error ? error.message : "저장에 실패했습니다."); }
    finally { setSaving(false); }
  }}><label>닉네임<input value={nickname} maxLength={80} disabled={saving} onChange={event => setNickname(event.target.value)} required/></label><label>이메일<input value={member.email} readOnly/></label><p role="status">{message}</p><div className="modal-actions"><button type="button" className="outline-button" disabled={saving} onClick={onClose}>취소 / 닫기</button><button className="primary-button" disabled={saving}>{saving ? "저장 중..." : "저장"}</button></div></form></div></div>;
}
