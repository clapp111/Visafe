"use client";

import { useQuery } from "@tanstack/react-query";
import { authApi, type components } from "@/shared/api";

type LookupResponse = components["schemas"]["LookupResponse"];

export function useLookup(confirmationNo: string) {
  return useQuery({
    queryKey: ["contracts", "lookup", confirmationNo],
    queryFn: async ({ signal }) => {
      const { data } = await authApi.get<LookupResponse>(
        `/lookup/${encodeURIComponent(confirmationNo)}`,
        { signal },
      );
      return data;
    },
    networkMode: "always",
    retry: false,
    refetchOnWindowFocus: false,
  });
}
