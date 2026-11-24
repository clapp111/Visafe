import type { ReactNode } from "react";

/** 위에 굵은 선을 두른 결과 절 (핵심 근로조건·블록체인 기록·확인 이력) */
export function ResultSection({
  id,
  title,
  children,
}: {
  id: string;
  title: ReactNode;
  children: ReactNode;
}) {
  return (
    <section
      aria-labelledby={id}
      className="flex flex-col gap-4 border-t-2 border-foreground pt-6"
    >
      <h2 id={id} className="text-2xl leading-normal font-bold">
        {title}
      </h2>
      {children}
    </section>
  );
}
