import { routes } from "@/shared/config";
import type { MemberRole } from "./types";

function isWithin(pathname: string, root: string): boolean {
  return pathname === root || pathname.startsWith(`${root}/`);
}

export function canAccessProtectedRoute(pathname: string, role: MemberRole) {
  if (isWithin(pathname, routes.adminContracts)) {
    return role === "ADMIN";
  }
  if (isWithin(pathname, routes.contractNew)) {
    return role === "EMPLOYER";
  }

  return isWithin(pathname, routes.contracts) && role !== "ADMIN";
}

export function getPostLoginPath(role: MemberRole, returnTo?: string | null) {
  const fallback = role === "ADMIN" ? routes.adminContracts : routes.contracts;
  if (
    returnTo?.startsWith("/") &&
    !returnTo.startsWith("//") &&
    !returnTo.includes("\\")
  ) {
    const origin = "https://visafe.invalid";
    try {
      const target = new URL(returnTo, origin);
      if (
        target.origin === origin &&
        canAccessProtectedRoute(target.pathname, role)
      ) {
        return target.pathname + target.search + target.hash;
      }
    } catch {
      return fallback;
    }
  }

  return fallback;
}

export function getLoginPath(location: Location) {
  const { pathname, search, hash } = location;
  if (
    isWithin(pathname, routes.contracts) ||
    isWithin(pathname, routes.adminContracts)
  ) {
    return `${routes.login}?next=${encodeURIComponent(pathname + search + hash)}`;
  }
  return routes.login;
}
