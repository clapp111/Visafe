import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

export type BadgeTone =
  "primary" | "success" | "neutral" | "warning" | "danger" | "highlight";

const TONE_CLASS: Record<BadgeTone, string> = {
  primary: "border-primary-border bg-primary-subtle text-primary",
  success: "border-success-border bg-success-subtle text-success",
  neutral: "border-neutral-border bg-surface-muted text-muted",
  warning: "border-warning-border bg-warning-subtle text-warning",
  danger: "border-danger-border bg-danger-subtle text-danger",
  highlight:
    "border-highlight-border bg-highlight-subtle text-highlight-foreground",
};

// sm: 역할·'고용주 입력' 같은 작은 태그, md: 확정·종료 같은 상태 배지(아이콘과 함께)
const SIZE_CLASS = {
  sm: "px-2 py-0.5 text-xs",
  md: "min-w-[84px] justify-center gap-1.5 px-2.5 py-1 text-sm",
};

export interface BadgeProps {
  tone?: BadgeTone;
  size?: keyof typeof SIZE_CLASS;
  /** 색만으로 구분하지 않도록 상태 배지에는 아이콘을 함께 둔다 */
  icon?: ReactNode;
  className?: string;
  children: ReactNode;
}

export function Badge({
  tone = "neutral",
  size = "sm",
  icon,
  className,
  children,
}: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center whitespace-nowrap rounded-xs border font-bold",
        TONE_CLASS[tone],
        SIZE_CLASS[size],
        className,
      )}
    >
      {icon}
      <span>{children}</span>
    </span>
  );
}
