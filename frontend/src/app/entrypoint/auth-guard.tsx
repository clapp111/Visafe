"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import {
  canAccessProtectedRoute,
  getLoginPath,
  getPostLoginPath,
  useSession,
} from "@/entities/session";
import { routes } from "@/shared/config";

export function AuthGuard({ children }: { children: ReactNode }) {
  const { session } = useSession();
  const pathname = usePathname();
  const router = useRouter();
  const allowed =
    pathname !== null &&
    session.status === "authenticated" &&
    canAccessProtectedRoute(pathname, session.member.role);

  useEffect(() => {
    if (pathname === null) {
      return;
    }

    if (session.status === "signed-out") {
      router.replace(routes.login);
    } else if (session.status === "guest") {
      router.replace(getLoginPath(window.location));
    } else if (session.status === "authenticated" && !allowed) {
      router.replace(getPostLoginPath(session.member.role));
    }
  }, [allowed, pathname, router, session]);

  return allowed ? children : null;
}
