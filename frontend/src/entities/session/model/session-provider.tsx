"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { createContext, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
  reissueAccessToken,
  setAccessToken,
  setSessionExpiredHandler,
  type components,
} from "@/shared/api";
import { routes } from "@/shared/config";
import {
  fetchCurrentMember,
  useLoginMutation,
  useLogoutMutation,
} from "../api/session";
import { getLoginPath } from "./navigation";
import type { SessionMember } from "./types";

type Session =
  | { status: "restoring" | "guest" | "signed-out"; member?: undefined }
  | { status: "authenticated"; member: SessionMember };

interface SessionContextValue {
  session: Session;
  login: (
    values: components["schemas"]["LoginRequest"],
  ) => Promise<SessionMember>;
  logout: () => Promise<void>;
  isLoggingIn: boolean;
  loginError: unknown;
}

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [session, setSession] = useState<Session>({ status: "restoring" });
  const loginMutation = useLoginMutation();
  const logoutMutation = useLogoutMutation();

  useEffect(() => {
    let active = true;
    const unregister = setSessionExpiredHandler(() => {
      queryClient.clear();
      setSession({ status: "guest" });
      router.replace(getLoginPath(window.location));
    });

    async function restoreSession() {
      try {
        await reissueAccessToken();
        if (active) {
          const member = await fetchCurrentMember();
          if (active) {
            setSession({ status: "authenticated", member });
          }
        }
      } catch {
        if (active) {
          setAccessToken(null);
          setSession({ status: "guest" });
        }
      }
    }

    void restoreSession();
    return () => {
      active = false;
      unregister();
    };
  }, [queryClient, router]);

  async function login(values: components["schemas"]["LoginRequest"]) {
    const member = await loginMutation.mutateAsync(values);
    loginMutation.reset();
    queryClient.clear();
    setSession({ status: "authenticated", member });
    return member;
  }

  async function logout() {
    if (logoutMutation.isPending) {
      return;
    }

    await logoutMutation.mutateAsync();
    queryClient.clear();
    setSession({ status: "signed-out" });
    router.replace(routes.login);
  }

  return (
    <SessionContext.Provider
      value={{
        session,
        login,
        logout,
        isLoggingIn: loginMutation.isPending,
        loginError: loginMutation.error,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}

export function useSession() {
  const value = useContext(SessionContext);
  if (value === null) {
    throw new Error("SessionProvider is required");
  }

  return value;
}
