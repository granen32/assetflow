"use client";

import { ReactNode, useEffect, useRef } from "react";

export default function ModalFrame({ children, labelledBy, busy, onClose }: {
  children: ReactNode;
  labelledBy: string;
  busy: boolean;
  onClose: () => void;
}) {
  const dialog = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialog.current?.querySelector<HTMLElement>("input, button, select")?.focus();
    return () => { document.body.style.overflow = previousOverflow; previous?.focus(); };
  }, []);

  return <div className="modal-backdrop" onMouseDown={event => {
    if (!busy && event.target === event.currentTarget) onClose();
  }}><div ref={dialog} className="modal-card" role="dialog" aria-modal="true" aria-labelledby={labelledBy} tabIndex={-1} onKeyDown={event => {
    if (event.key === "Escape" && !busy) onClose();
    if (event.key !== "Tab") return;
    const fields = Array.from(dialog.current?.querySelectorAll<HTMLElement>("input, button, select, [tabindex='0']") ?? []).filter(field => !field.matches(":disabled"));
    const first = fields[0], last = fields[fields.length - 1];
    if (!first) { event.preventDefault(); dialog.current?.focus(); }
    else if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog.current)) { event.preventDefault(); last?.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }}>{children}</div></div>;
}
