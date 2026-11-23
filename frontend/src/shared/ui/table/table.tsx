import type { ReactNode } from "react";
import { cn } from "@/shared/lib";

export interface TableProps {
  /** 화면에는 숨기고 스크린리더에 읽히는 표 제목 */
  caption: string;
  className?: string;
  /** `<thead>`·`<tbody>` */
  children: ReactNode;
}

/** 와이어프레임 표 모양. 좁은 화면에서는 표만 가로로 스크롤한다. */
export function Table({ caption, className, children }: TableProps) {
  return (
    <div className="overflow-x-auto">
      <table
        className={cn(
          "w-full border-collapse text-base",
          "[&_thead_th]:whitespace-nowrap [&_thead_th]:border-t-2 [&_thead_th]:border-b [&_thead_th]:border-t-foreground [&_thead_th]:border-b-border [&_thead_th]:bg-surface-muted [&_thead_th]:px-3 [&_thead_th]:py-3.5 [&_thead_th]:text-left [&_thead_th]:font-bold",
          "[&_tbody_td]:border-b [&_tbody_td]:border-divider [&_tbody_td]:px-3 [&_tbody_td]:py-3.5 [&_tbody_td]:align-middle",
          "[&_tbody_th]:border-b [&_tbody_th]:border-divider [&_tbody_th]:px-3 [&_tbody_th]:py-3.5 [&_tbody_th]:text-left [&_tbody_th]:align-middle [&_tbody_th]:text-sm [&_tbody_th]:font-normal [&_tbody_th]:text-muted",
          className,
        )}
      >
        <caption className="sr-only">{caption}</caption>
        {children}
      </table>
    </div>
  );
}
