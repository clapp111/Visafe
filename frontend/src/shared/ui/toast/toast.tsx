import type { ReactNode } from "react";
import { cn } from "@/shared/lib";
import { CircleCheckIcon, CloseIcon, ErrorIcon, InfoIcon } from "../icons";

export type ToastTone = "info" | "success" | "danger";

const TONE = {
  info: { Icon: InfoIcon, icon: "text-primary-border" },
  success: { Icon: CircleCheckIcon, icon: "text-success-border" },
  danger: { Icon: ErrorIcon, icon: "text-danger-border" },
} as const;

export interface ToastProps {
  tone?: ToastTone;
  onClose?: () => void;
  closeLabel?: string;
  className?: string;
  children: ReactNode;
}

/** 짧은 결과 알림. 화면 위치와 표시 시간은 쓰는 쪽에서 정한다. */
export function Toast({
  tone = "info",
  onClose,
  closeLabel = "닫기",
  className,
  children,
}: ToastProps) {
  const { Icon, icon } = TONE[tone];
  return (
    <div
      role={tone === "danger" ? "alert" : "status"}
      className={cn(
        "flex items-center gap-3 rounded-md bg-foreground py-3.5 pr-2 pl-5 text-primary-foreground shadow-[0_8px_24px_rgb(0_0_0/0.2)]",
        className,
      )}
    >
      <Icon size={22} className={cn("shrink-0", icon)} />
      <span className="min-w-0 flex-1">{children}</span>
      {onClose && (
        <button
          type="button"
          aria-label={closeLabel}
          onClick={onClose}
          className="flex size-11 shrink-0 cursor-pointer items-center justify-center"
        >
          <CloseIcon size={20} />
        </button>
      )}
    </div>
  );
}
