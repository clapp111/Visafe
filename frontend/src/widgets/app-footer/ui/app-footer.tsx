"use client";

import { type UiLanguage, useCurrentLanguage } from "@/shared/i18n";

const NOTICE = {
  ko: "Visafe는 블록체인 프로젝트 수업의 프로토타입이며, 정부·공공기관의 공식 서비스가 아닙니다.",
  vi: "Visafe là bản thử nghiệm của môn học dự án blockchain, không phải dịch vụ chính thức của chính phủ hay cơ quan nhà nước.",
} satisfies Record<UiLanguage, string>;

export function AppFooter({ notice }: { notice?: string }) {
  const language = useCurrentLanguage();
  return (
    <footer className="border-t border-border bg-surface-muted">
      <div className="mx-auto max-w-[1200px] px-6 pt-8 pb-10 text-sm text-muted">
        <p>{notice ?? NOTICE[language]}</p>
      </div>
    </footer>
  );
}
