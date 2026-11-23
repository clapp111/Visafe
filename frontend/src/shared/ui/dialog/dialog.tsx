"use client";

import { useEffect, useId, useRef, type ReactNode } from "react";
import { cn } from "@/shared/lib";
import { CloseIcon } from "../icons";

export interface DialogProps {
  open: boolean;
  /** 닫기 버튼이나 Esc로 닫을 때 불린다. 쓰는 쪽에서 `open`을 false로 바꾼다 */
  onClose: () => void;
  title: ReactNode;
  description?: ReactNode;
  /** 오른쪽 아래 버튼 (취소·확인 순서) */
  actions?: ReactNode;
  /** md 560px, lg 640px */
  size?: "md" | "lg";
  closeLabel?: string;
  children?: ReactNode;
}

/** 네이티브 `<dialog>` 확인 창. 열려 있는 동안 포커스가 창 안에 머문다. */
export function Dialog({
  open,
  onClose,
  title,
  description,
  actions,
  size = "md",
  closeLabel = "닫기",
  children,
}: DialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const descriptionId = useId();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      className={cn(
        "m-auto w-[calc(100%-2rem)] rounded-lg bg-surface p-0 text-foreground shadow-[0_8px_24px_rgb(0_0_0/0.2)] backdrop:bg-overlay",
        size === "lg" ? "max-w-[640px]" : "max-w-[560px]",
      )}
    >
      <div className="flex items-center justify-between gap-4 px-7 pt-6">
        <h2 id={titleId} className="text-2xl font-bold">
          {title}
        </h2>
        <button
          type="button"
          aria-label={closeLabel}
          onClick={onClose}
          className="flex size-11 shrink-0 cursor-pointer items-center justify-center"
        >
          <CloseIcon size={24} />
        </button>
      </div>
      <div className="flex flex-col gap-5 px-7 pt-5 pb-7">
        {description && (
          <p id={descriptionId} className="text-muted">
            {description}
          </p>
        )}
        {children}
        {actions && (
          <div className="flex flex-wrap justify-end gap-3 pt-2">{actions}</div>
        )}
      </div>
    </dialog>
  );
}
