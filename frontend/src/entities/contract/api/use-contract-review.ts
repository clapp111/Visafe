"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type components } from "@/shared/api";

type Review = components["schemas"]["ContractReviewResponse"];
type ReviewResponseRequest =
  components["schemas"]["ReviewResponseCreateRequest"];
type StageResponse = components["schemas"]["ContractStageResponse"];

export function useContractReview(contractId: number) {
  return useQuery({
    queryKey: ["contracts", contractId, "review"],
    queryFn: async ({ signal }) => {
      const { data } = await api.get<Review>(
        `/contracts/${contractId}/review`,
        {
          signal,
        },
      );
      return data;
    },
    retry: false,
  });
}

export function useConfirmContractReview(contractId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (translationId: number) => {
      const request: ReviewResponseRequest = {
        translationId,
        action: "UNDERSTOOD",
        readAllChecked: true,
      };
      const { data } = await api.post<StageResponse>(
        `/contracts/${contractId}/review-responses`,
        request,
      );
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["contracts", "list"] });
    },
  });
}
