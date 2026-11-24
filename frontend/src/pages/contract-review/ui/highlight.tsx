import type { ReactNode } from "react";

/** 고용주가 고치거나 입력한 값의 형광펜 */
export function Highlight({ children }: { children: ReactNode }) {
  return (
    <mark className="rounded-xs bg-highlight px-1 text-inherit shadow-[inset_0_-2px_0_var(--color-highlight-border)]">
      {children}
    </mark>
  );
}
