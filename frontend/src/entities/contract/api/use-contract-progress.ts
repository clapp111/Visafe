"use client";

import { skipToken, useQuery } from "@tanstack/react-query";
import { api, type components } from "@/shared/api";

type StageResponse = components["schemas"]["ContractStageResponse"];
type Detail = components["schemas"]["ContractDetailResponse"];
const POLL_INTERVAL_MS = 2500;

function getProgress(
  detail: Detail | undefined,
  initialStage: StageResponse["stage"],
) {
  if (detail?.processing?.status === "FAILED") {
    return "failed";
  }
  if (detail?.stage && detail.stage !== initialStage) {
    return "stage-changed";
  }
  return "polling";
}

/** 202 응답의 데이터와 mutation.submittedAt을 넘긴다. 같은 단계의 재시도도 새 조회로 시작한다. */
export function useContractProgress(
  accepted: StageResponse | undefined,
  submittedAt: number,
) {
  const contractId = accepted?.contractId;
  const initialStage = accepted?.stage;
  const active = contractId !== undefined && initialStage !== undefined;

  const query = useQuery({
    queryKey: ["contracts", "progress", contractId, initialStage, submittedAt],
    queryFn: active
      ? async ({ signal }) => {
          const { data } = await api.get<Detail>(`/contracts/${contractId}`, {
            signal,
          });
          if (!data?.stage) {
            throw new Error("Contract detail did not include its stage");
          }
          return data;
        }
      : skipToken,
    retry: false,
    retryOnMount: false,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    refetchInterval: ({ state }) =>
      active &&
      state.status !== "error" &&
      getProgress(state.data, initialStage) === "polling"
        ? POLL_INTERVAL_MS
        : false,
  });

  const status = !active
    ? "idle"
    : query.isError
      ? "request-error"
      : getProgress(query.data, initialStage);

  return {
    status,
    data: query.data,
    error: query.error,
    refetch: active ? query.refetch : undefined,
  };
}
