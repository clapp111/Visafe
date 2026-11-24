"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { api, type components, type operations } from "@/shared/api";

type ListResponse = components["schemas"]["ContractListResponse"];
type ListParams = NonNullable<operations["list"]["parameters"]["query"]>;

const PAGE_SIZE = 20;

export function useContractList(status: ListParams["status"], page: number) {
  const queryKey = ["contracts", "list", status, page] as const;

  return useQuery({
    queryKey,
    queryFn: async ({ signal }) => {
      const params: ListParams = { status, page, size: PAGE_SIZE };
      const { data } = await api.get<ListResponse>("/contracts", {
        params,
        signal,
      });
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}
