import type { ReactNode } from "react";
import { AppFooter } from "@/widgets/app-footer";
import { AppHeader } from "@/widgets/app-header";

/** 모든 화면의 헤더·본문·푸터 틀. 본문은 `#main`이라 페이지는 `<main>`을 따로 두지 않는다. */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <>
      {/* 로그인 상태·언어 전환은 #52·#54 연동에서 AppHeader props로 넘긴다 */}
      <AppHeader />
      <main id="main" className="flex-1">
        {children}
      </main>
      <AppFooter />
    </>
  );
}
