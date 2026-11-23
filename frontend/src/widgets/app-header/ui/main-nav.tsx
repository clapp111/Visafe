"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { routes } from "@/shared/config";
import { cn } from "@/shared/lib";
import type { AppHeaderLabels, MemberRole } from "../model/labels";

interface NavItem {
  href: string;
  label: string;
  isCurrent: (pathname: string, view: string) => boolean;
}

const isAdminList = (pathname: string) => pathname === routes.adminContracts;

function navItems(
  role: MemberRole | undefined,
  labels: AppHeaderLabels,
): NavItem[] {
  const lookup: NavItem = {
    href: routes.home,
    label: labels.lookup,
    isCurrent: (pathname) =>
      pathname === routes.home || pathname.startsWith("/lookup/"),
  };
  const contracts: NavItem = {
    href: routes.contracts,
    label: labels.contracts,
    isCurrent: (pathname) =>
      pathname.startsWith(routes.contracts) &&
      !pathname.startsWith(routes.contractNew),
  };

  if (role === "ADMIN") {
    const views = [
      ["PENDING", labels.adminPending],
      ["APPROVED", labels.adminApproved],
      ["REJECTED", labels.adminRejected],
      ["TERMINATED", labels.adminTerminated],
    ] as const;
    return [
      lookup,
      ...views.map(([view, label]) => ({
        href: routes.adminContractsView(view),
        label,
        isCurrent: (pathname: string, current: string) =>
          isAdminList(pathname) && current === view,
      })),
    ];
  }

  if (role === "EMPLOYER") {
    const contractNew: NavItem = {
      href: routes.contractNew,
      label: labels.contractNew,
      isCurrent: (pathname) => pathname.startsWith(routes.contractNew),
    };
    return [lookup, contractNew, contracts];
  }

  return [lookup, contracts];
}

function NavList({
  items,
  label,
  isCurrent,
}: {
  items: NavItem[];
  label: string;
  isCurrent: (item: NavItem) => boolean;
}) {
  return (
    <nav aria-label={label} className="border-t border-border">
      <ul className="mx-auto flex max-w-[1200px] flex-wrap px-6">
        {items.map((item, index) => {
          const current = isCurrent(item);
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={current ? "page" : undefined}
                className={cn(
                  "-mb-0.5 flex min-h-18 items-center text-xl font-bold no-underline",
                  index === 0 ? "pr-7" : "px-7",
                  current
                    ? "border-b-4 border-primary text-primary"
                    : "text-secondary-foreground hover:text-primary",
                )}
              >
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}

/** 현재 경로·보기에 맞춰 선택 표시를 한다. 쿼리를 읽으므로 Suspense 안에서 쓴다. */
export function MainNav({
  role,
  labels,
}: {
  role?: MemberRole;
  labels: AppHeaderLabels;
}) {
  const pathname = usePathname() ?? routes.home;
  const view = useSearchParams()?.get("view") ?? "PENDING";
  return (
    <NavList
      items={navItems(role, labels)}
      label={labels.mainNav}
      isCurrent={(item) => item.isCurrent(pathname, view)}
    />
  );
}

/** Suspense 대기 중에 보이는 같은 메뉴 (선택 표시 없음) */
export function MainNavFallback({
  role,
  labels,
}: {
  role?: MemberRole;
  labels: AppHeaderLabels;
}) {
  return (
    <NavList
      items={navItems(role, labels)}
      label={labels.mainNav}
      isCurrent={() => false}
    />
  );
}
