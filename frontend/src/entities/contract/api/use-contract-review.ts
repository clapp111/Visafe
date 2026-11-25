"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type components, getApiErrorCode } from "@/shared/api";

type Review = components["schemas"]["ContractReviewResponse"];
type ReviewResponseRequest =
  components["schemas"]["ReviewResponseCreateRequest"];
type StageResponse = components["schemas"]["ContractStageResponse"];
type Question = components["schemas"]["QuestionResponse"];
type QuestionCreateRequest = components["schemas"]["QuestionCreateRequest"];

const reviewKey = (contractId: number) =>
  ["contracts", contractId, "review"] as const;
const questionsKey = (contractId: number) =>
  ["contracts", contractId, "questions"] as const;

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

export function useContractQuestions(contractId: number) {
  return useQuery({
    queryKey: questionsKey(contractId),
    queryFn: async ({ signal }) => {
      const { data } = await api.get<Question[]>(
        `/contracts/${contractId}/questions`,
        { signal },
      );
      return data;
    },
    retry: false,
  });
}

export function useAskContractQuestion(contractId: number) {
  const queryClient = useQueryClient();
  const queryKey = questionsKey(contractId);

  return useMutation({
    mutationFn: async (question: string) => {
      const request: QuestionCreateRequest = { question };
      const { data } = await api.post<Question>(
        `/contracts/${contractId}/questions`,
        request,
      );
      return data;
    },
    onSuccess: async () => {
      await queryClient.cancelQueries({ queryKey });
      await queryClient.invalidateQueries({ queryKey });
    },
    onError: (error) => {
      if (getApiErrorCode(error) === "TRANSLATION_NOT_READY") {
        void queryClient.invalidateQueries({ queryKey: reviewKey(contractId) });
      }
    },
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
