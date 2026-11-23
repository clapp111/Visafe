import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/shared/lib";

export type ButtonVariant = "primary" | "secondary" | "outline" | "danger";
export type ButtonSize = "md" | "lg";

const VARIANT_CLASS: Record<ButtonVariant, string> = {
  primary:
    "bg-primary font-bold text-primary-foreground hover:bg-primary-hover",
  secondary:
    "border border-border-strong bg-surface font-medium text-foreground hover:bg-surface-muted",
  outline:
    "border border-primary bg-surface font-bold text-primary hover:bg-primary-subtle",
  danger: "bg-danger font-bold text-primary-foreground hover:bg-danger/90",
};

// lg(56px)는 화면의 주요 버튼, md(48px)는 확인 창·파일 선택 버튼
const SIZE_CLASS: Record<ButtonSize, string> = {
  md: "h-12 px-6",
  lg: "h-14 px-7",
};

/** 버튼 모양 className. 링크(`<Link>`)를 버튼처럼 보일 때도 쓴다. */
export function buttonClass({
  variant = "primary",
  size = "lg",
}: { variant?: ButtonVariant; size?: ButtonSize } = {}) {
  return cn(
    "inline-flex cursor-pointer items-center justify-center gap-2 rounded-md text-base no-underline disabled:cursor-not-allowed disabled:opacity-50",
    VARIANT_CLASS[variant],
    SIZE_CLASS[size],
  );
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
}

export function Button({
  variant,
  size,
  className,
  type = "button",
  ...props
}: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(buttonClass({ variant, size }), className)}
      {...props}
    />
  );
}
