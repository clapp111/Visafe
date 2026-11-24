"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api, type components, getApiErrorCode } from "@/shared/api";
import { contractDetailKey } from "./use-contract-detail";

type TerminationRequest = components["schemas"]["TerminationCreateRequest"];
type TerminationDecision = components["schemas"]["TerminationRespondRequest"];
type TerminationResponse = components["schemas"]["TerminationResponse"];

function useTerminationMutation<Variables>(
  contractId: number,
  mutationFn: (variables: Variables) => Promise<TerminationResponse>,
) {
  const queryClient = useQueryClient();
  const queryKey = contractDetailKey(contractId);

  return useMutation({
    mutationFn,
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey }),
        queryClient.invalidateQueries({ queryKey: ["contracts", "list"] }),
      ]);
    },
    onError: (error) => {
      const code = getApiErrorCode(error);
      if (
        code === "TERMINATION_STATUS_CONFLICT" ||
        code === "TERMINATION_ALREADY_ACTIVE" ||
        code === "CONTRACT_STAGE_CONFLICT"
      ) {
        void queryClient.invalidateQueries({ queryKey });
      }
    },
  });
}

export function useRequestContractTermination(contractId: number) {
  return useTerminationMutation(
    contractId,
    async (request: TerminationRequest) => {
      const { data } = await api.post<TerminationResponse>(
        `/contracts/${contractId}/terminations`,
        request,
      );
      return data;
    },
  );
}

export function useRespondContractTermination(contractId: number) {
  return useTerminationMutation(
    contractId,
    async ({
      terminationId,
      decision,
    }: TerminationDecision & { terminationId: number }) => {
      const request: TerminationDecision = { decision };
      const { data } = await api.post<TerminationResponse>(
        `/contracts/${contractId}/terminations/${terminationId}/response`,
        request,
      );
      return data;
    },
  );
}

export function useCancelContractTermination(contractId: number) {
  return useTerminationMutation(contractId, async (terminationId: number) => {
    const { data } = await api.post<TerminationResponse>(
      `/contracts/${contractId}/terminations/${terminationId}/cancel`,
    );
    return data;
  });
}
