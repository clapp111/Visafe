"use client";

import { useQuery } from "@tanstack/react-query";
import { api, type components } from "@/shared/api";

type Detail = components["schemas"]["ContractDetailResponse"];

const POLL_INTERVAL_MS = 2500;
const FAILED_RECORD_POLL_INTERVAL_MS = 10000;

export function useContractDetail(contractId: number) {
  return useQuery({
    queryKey: ["contracts", contractId, "detail"],
    queryFn: async ({ signal }) => {
      const { data } = await api.get<Detail>(`/contracts/${contractId}`, {
        signal,
      });
      if (!data?.stage) {
        throw new Error("Contract detail did not include its stage");
      }
      return data;
    },
    retry: false,
    refetchOnWindowFocus: false,
    refetchInterval: ({ state }) => {
      const detail = state.data;
      if (
        state.status === "error" ||
        (detail?.stage !== "RECORDING" && detail?.stage !== "AUTHORITY_REVIEW")
      ) {
        return false;
      }
      return detail.processing?.task === "CHAIN_RECORD" &&
        detail.processing.status === "FAILED"
        ? FAILED_RECORD_POLL_INTERVAL_MS
        : POLL_INTERVAL_MS;
    },
  });
}
