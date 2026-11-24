"use client";

import { useMutation } from "@tanstack/react-query";
import { api, type components } from "@/shared/api";

type StageResponse = components["schemas"]["ContractStageResponse"];

export function useRetryContractTranslation(contractId: number) {
  return useMutation({
    mutationFn: async () => {
      const { data } = await api.post<StageResponse>(
        `/contracts/${contractId}/translations/retry`,
      );
      return data;
    },
  });
}
