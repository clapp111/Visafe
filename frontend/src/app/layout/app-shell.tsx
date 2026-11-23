"use client";

import type { ReactNode } from "react";
import { useSession } from "@/entities/session";
import { AppFooter } from "@/widgets/app-footer";
import { AppHeader } from "@/widgets/app-header";

/** 모든 화면의 헤더·본문·푸터 틀. 본문은 `#main`이라 페이지는 `<main>`을 따로 두지 않는다. */
export function AppShell({ children }: { children: ReactNode }) {
  const { session, logout } = useSession();

  function handleLogout() {
    void logout().catch(() => {
      window.alert("로그아웃하지 못했습니다. 다시 시도해 주세요.");
    });
  }

  return (
    <>
      {/* 화면 언어 전환은 #54 연동에서 AppHeader props로 넘긴다. */}
      <AppHeader user={session.member} onLogout={handleLogout} />
      <main id="main" className="flex-1">
        {children}
      </main>
      <AppFooter />
    </>
  );
}
