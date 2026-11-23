import { useMutation } from "@tanstack/react-query";
import {
  api,
  authApi,
  logoutSession,
  setAccessToken,
  type components,
} from "@/shared/api";
import type { SessionMember } from "../model/types";

export async function fetchCurrentMember(): Promise<SessionMember> {
  const { data } =
    await api.get<components["schemas"]["MemberResponse"]>("/members/me");
  const { name, role } = data ?? {};
  if (
    typeof name !== "string" ||
    (role !== "EMPLOYER" && role !== "WORKER" && role !== "ADMIN")
  ) {
    throw new Error("Unable to verify the current member");
  }

  return { ...data, name, role };
}

async function login(values: components["schemas"]["LoginRequest"]) {
  const { data } = await authApi.post<components["schemas"]["TokenResponse"]>(
    "/auth/login",
    values,
  );

  try {
    if (!data?.accessToken) {
      throw new Error("Unable to read the access token");
    }

    setAccessToken(data.accessToken);
    return await fetchCurrentMember();
  } catch (error) {
    setAccessToken(null);
    await logoutSession().catch(() => undefined);
    throw error;
  }
}

export function useLoginMutation() {
  return useMutation({ mutationFn: login });
}

export function useLogoutMutation() {
  return useMutation({ mutationFn: logoutSession });
}
