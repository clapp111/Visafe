"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type components, getApiErrorCode } from "@/shared/api";

type Review = components["schemas"]["ContractReviewResponse"];
type ReviewResponseRequest =
  components["schemas"]["ReviewResponseCreateRequest"];
type StageResponse = components["schemas"]["ContractStageResponse"];

const reviewKey = (contractId: number) =>
  ["contracts", contractId, "review"] as const;

export function useContractReview(contractId: number) {
  return useQuery({
    queryKey: reviewKey(contractId),
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

export function useReportContractReview(contractId: number) {
  const queryClient = useQueryClient();
  const queryKey = reviewKey(contractId);

  return useMutation({
    mutationFn: async ({
      translationId,
      keys,
      reason,
    }: {
      translationId: number;
      keys: string[];
      reason?: string;
    }) => {
      const request: ReviewResponseRequest = {
        translationId,
        action: "REPORTED",
        reportItems: keys.map((key) => ({ type: "ITEM_MISMATCH", key })),
        reason,
      };
      const { data } = await api.post<StageResponse>(
        `/contracts/${contractId}/review-responses`,
        request,
      );
      return data;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey });
      void queryClient.invalidateQueries({ queryKey: ["contracts", "list"] });
    },
    onError: (error) => {
      const code = getApiErrorCode(error);
      if (
        code === "TRANSLATION_OUTDATED" ||
        code === "CONTRACT_STAGE_CONFLICT" ||
        code === "TERM_KEY_UNKNOWN"
      ) {
        void queryClient.invalidateQueries({ queryKey });
      }
    },
  });
}
