import type { InputHTMLAttributes, ReactNode } from "react";
import { cn } from "@/shared/lib";

export interface CheckboxProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
  "type"
> {
  label: ReactNode;
}

/** 라벨 전체를 눌러 체크할 수 있는 체크박스 (높이 44px 이상) */
export function Checkbox({ label, className, ...props }: CheckboxProps) {
  return (
    <label
      className={cn(
        "flex min-h-11 cursor-pointer items-start gap-2.5",
        className,
      )}
    >
      <input
        type="checkbox"
        className="mt-[3px] size-5 shrink-0 accent-primary"
        {...props}
      />
      <span>{label}</span>
    </label>
  );
}
