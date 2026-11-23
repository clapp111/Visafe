"use client";

import type { ReactNode } from "react";
import { useSession } from "@/entities/session";
import { useUiLanguage } from "@/features/change-language";
import { getApiErrorCode, getApiErrorFeedback } from "@/shared/api";
import { Toast } from "@/shared/ui";
import { AppFooter } from "@/widgets/app-footer";
import { AppHeader, HEADER_LABELS } from "@/widgets/app-header";

/** 모든 화면의 헤더·본문·푸터 틀. 본문은 `#main`이라 페이지는 `<main>`을 따로 두지 않는다. */
export function AppShell({ children }: { children: ReactNode }) {
  const { session, logout, updateMemberLanguage } = useSession();
  const locale = useUiLanguage(
    session.status === "restoring" || session.status === "authenticated"
      ? {
          mode: "member",
          member: session.member,
          onMemberUpdated: updateMemberLanguage,
        }
      : { mode: "guest" },
  );
  const { language, messages } = locale;
  const nextLanguage = language === "ko" ? "vi" : "ko";

  function handleLogout() {
    void logout().catch((error: unknown) => {
      const code = getApiErrorCode(error);
      window.alert(
        code
          ? getApiErrorFeedback(code, language).message
          : messages.logoutFailed,
      );
    });
  }

  return (
    <>
      <AppHeader
        user={session.member}
        onLogout={handleLogout}
        language={language}
        labels={HEADER_LABELS[language]}
        languageToggle={{
          label: messages.switchLanguage,
          lang: nextLanguage,
          ariaLabel: messages.switchLanguageLabel,
          disabled: !locale.canChangeLanguage,
          onToggle: () => locale.changeLanguage(nextLanguage),
        }}
      />
      {locale.hasLanguageChangeError && (
        <div lang={language}>
          <Toast
            tone="danger"
            closeLabel={messages.close}
            onClose={locale.dismissLanguageError}
          >
            {locale.errorCode
              ? getApiErrorFeedback(locale.errorCode, language).message
              : messages.languageChangeFailed}
          </Toast>
        </div>
      )}
      <main id="main" className="flex-1">
        {children}
      </main>
      <AppFooter />
    </>
  );
}
