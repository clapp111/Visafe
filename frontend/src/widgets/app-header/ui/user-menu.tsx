"use client";

import { useEffect, useId, useRef, useState } from "react";
import { Badge, ChevronDownIcon, UserIcon } from "@/shared/ui";
import type { AppHeaderLabels } from "../model/labels";

export interface UserMenuProps {
  name: string;
  roleLabel: string;
  onLogout?: () => void;
  labels: AppHeaderLabels;
}

/** 이름·역할 배지 버튼을 누르면 아래에 로그아웃이 열린다. Esc·바깥 클릭으로 닫힌다. */
export function UserMenu({ name, roleLabel, onLogout, labels }: UserMenuProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const panelId = useId();

  useEffect(() => {
    if (!open) return;
    function close(event: MouseEvent | KeyboardEvent) {
      if (event instanceof KeyboardEvent && event.key !== "Escape") return;
      if (
        event instanceof MouseEvent &&
        ref.current?.contains(event.target as Node)
      )
        return;
      setOpen(false);
    }
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", close);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", close);
    };
  }, [open]);

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        aria-label={`${labels.userMenu}: ${name} ${roleLabel}`}
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((value) => !value)}
        className="flex min-h-11 cursor-pointer items-center gap-2 pl-4"
      >
        <UserIcon />
        <span>
          <strong>{name}</strong>
          {labels.userSuffix && ` ${labels.userSuffix}`}
        </span>
        <Badge tone="primary">{roleLabel}</Badge>
        <ChevronDownIcon size={16} />
      </button>
      {open && (
        <div
          id={panelId}
          className="absolute top-full right-0 z-10 mt-2 min-w-40 rounded-md border border-border bg-surface py-1 shadow-[0_8px_24px_rgb(0_0_0/0.12)]"
        >
          <button
            type="button"
            onClick={() => {
              setOpen(false);
              onLogout?.();
            }}
            className="flex min-h-11 w-full cursor-pointer items-center px-4 text-left hover:bg-surface-muted"
          >
            {labels.logout}
          </button>
        </div>
      )}
    </div>
  );
}
