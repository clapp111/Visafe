import type { ReactNode } from "react";
import { cn } from "@/shared/lib";
import { CircleCheckIcon, ErrorIcon, InfoIcon, WarningIcon } from "../icons";

export type AlertTone = "info" | "success" | "warning" | "danger";

const TONE = {
  info: {
    box: "border-primary-border bg-primary-subtle",
    left: "border-l-primary",
    top: "border-t-primary",
    icon: "text-primary",
    title: "text-foreground",
    Icon: InfoIcon,
  },
  success: {
    box: "border-success-border bg-success-subtle",
    left: "border-l-success",
    top: "border-t-success",
    icon: "text-success",
    title: "text-success",
    Icon: CircleCheckIcon,
  },
  warning: {
    box: "border-warning-border bg-warning-surface",
    left: "border-l-warning-accent",
    top: "border-t-warning-accent",
    icon: "text-warning",
    title: "text-foreground",
    Icon: WarningIcon,
  },
  danger: {
    box: "border-danger-border bg-danger-subtle",
    left: "border-l-danger",
    top: "border-t-danger",
    icon: "text-danger",
    title: "text-danger",
    Icon: ErrorIcon,
  },
} as const;

export interface AlertProps {
  tone?: AlertTone;
  /** left: 결과 안내, top: 할 일 배너, none: 일반 안내 상자 */
  accent?: "left" | "top" | "none";
  title?: ReactNode;
  /** 제목·본문 오른쪽에 두는 버튼 등 */
  action?: ReactNode;
  className?: string;
  children?: ReactNode;
}

export function Alert({
  tone = "info",
  accent = "none",
  title,
  action,
  className,
  children,
}: AlertProps) {
  const style = TONE[tone];
  return (
    <div
      role={tone === "danger" ? "alert" : "status"}
      className={cn(
        "flex flex-wrap items-start gap-3 rounded-md border px-6 py-5",
        style.box,
        accent === "left" && cn("border-l-4", style.left),
        accent === "top" && cn("border-t-4", style.top),
        className,
      )}
    >
      <style.Icon size={24} className={cn("mt-0.5 shrink-0", style.icon)} />
      <div className="flex min-w-0 flex-[1_1_240px] flex-col gap-1">
        {title && (
          <strong className={cn("text-lg", style.title)}>{title}</strong>
        )}
        {children && <div className="text-sm">{children}</div>}
      </div>
      {action && <div className="shrink-0 self-center">{action}</div>}
    </div>
  );
}
