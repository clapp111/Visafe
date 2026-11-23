"use client";

import Link from "next/link";
import { Suspense } from "react";
import { routes } from "@/shared/config";
import type { UiLanguage } from "@/shared/i18n";
import { GlobeIcon, ShieldCheckIcon, UserIcon } from "@/shared/ui";
import {
  DEFAULT_HEADER_LABELS,
  type AppHeaderLabels,
  type MemberRole,
} from "../model/labels";
import { MainNav, MainNavFallback } from "./main-nav";
import { UserMenu } from "./user-menu";

export interface AppHeaderProps {
  /** 로그인한 회원. 없으면 로그인 전 헤더([로그인] 링크) */
  user?: { name: string; role: MemberRole } | null;
  onLogout?: () => void;
  language?: UiLanguage;
  /** 다른 화면 언어로 바꾸는 버튼. 버튼에는 바꿀 언어의 이름을 그 언어로 쓴다 (예: "Tiếng Việt") */
  languageToggle?: {
    label: string;
    lang: string;
    ariaLabel: string;
    disabled?: boolean;
    onToggle: () => void;
  };
  labels?: AppHeaderLabels;
}

export function AppHeader({
  user,
  onLogout,
  language,
  languageToggle,
  labels = DEFAULT_HEADER_LABELS,
}: AppHeaderProps) {
  return (
    <>
      <a
        href="#main"
        lang={language}
        className="absolute -top-12 left-4 z-20 rounded-md bg-foreground px-4 py-2 text-primary-foreground focus:top-2"
      >
        {labels.skipToContent}
      </a>
      <header lang={language} className="border-b-2 border-border bg-surface">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-x-8 gap-y-4 px-6 pt-4 pb-5">
          <Link
            href={routes.home}
            aria-label={labels.home}
            className="flex items-center gap-3.5 text-foreground no-underline"
          >
            <span className="flex size-14 shrink-0 items-center justify-center rounded-lg bg-primary">
              <ShieldCheckIcon size={34} className="text-primary-foreground" />
            </span>
            <span className="flex flex-col leading-[1.15]">
              <span className="text-5xl leading-[1.15] font-medium tracking-[-0.5px] text-secondary-foreground">
                Visafe
              </span>
              <span className="text-xs leading-[1.15] font-medium text-muted">
                {labels.tagline}
              </span>
            </span>
          </Link>

          <ul className="ml-auto flex flex-wrap items-center justify-end">
            {languageToggle && (
              <>
                <li>
                  <button
                    type="button"
                    aria-label={languageToggle.ariaLabel}
                    disabled={languageToggle.disabled}
                    onClick={languageToggle.onToggle}
                    className="flex min-h-11 cursor-pointer items-center gap-1.5 px-4"
                  >
                    <GlobeIcon />
                    <span lang={languageToggle.lang}>
                      {languageToggle.label}
                    </span>
                  </button>
                </li>
                <li aria-hidden="true" className="h-[18px] w-px bg-border" />
              </>
            )}
            <li>
              {user ? (
                <UserMenu
                  name={user.name}
                  roleLabel={labels.roles[user.role]}
                  onLogout={onLogout}
                  labels={labels}
                />
              ) : (
                <Link
                  href={routes.login}
                  className="flex min-h-11 items-center gap-2 pl-4 font-bold text-primary no-underline"
                >
                  <UserIcon />
                  {labels.login}
                </Link>
              )}
            </li>
          </ul>
        </div>

        <Suspense
          fallback={<MainNavFallback role={user?.role} labels={labels} />}
        >
          <MainNav role={user?.role} labels={labels} />
        </Suspense>
      </header>
    </>
  );
}
