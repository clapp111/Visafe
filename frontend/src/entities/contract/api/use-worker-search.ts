"use client";

import { useQuery } from "@tanstack/react-query";
import { api, type components } from "@/shared/api";

type WorkerSearchResponse = components["schemas"]["WorkerSearchResponse"];

export function useWorkerSearch(name: string) {
  return useQuery({
    queryKey: ["contracts", "workers", name],
    queryFn: async ({ signal }) => {
      const { data } = await api.get<WorkerSearchResponse[]>("/workers", {
        params: { name },
        signal,
      });
      return data;
    },
    enabled: name.length > 0,
    networkMode: "always",
    retry: false,
    refetchOnWindowFocus: false,
  });
}
