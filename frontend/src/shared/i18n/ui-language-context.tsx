"use client";

import { createContext, type ReactNode, useContext } from "react";
import { DEFAULT_UI_LANGUAGE, type UiLanguage } from "./language";

const UiLanguageContext = createContext<UiLanguage>(DEFAULT_UI_LANGUAGE);

/** 앱 틀(AppShell)이 정한 화면 언어를 아래 화면·컴포넌트에 내려 준다. */
export function UiLanguageProvider({
  language,
  children,
}: {
  language: UiLanguage;
  children: ReactNode;
}) {
  return <UiLanguageContext value={language}>{children}</UiLanguageContext>;
}

/** 지금 화면 언어. 로그인했으면 회원 설정, 아니면 이 브라우저에 저장한 값 */
export function useCurrentLanguage(): UiLanguage {
  return useContext(UiLanguageContext);
}
