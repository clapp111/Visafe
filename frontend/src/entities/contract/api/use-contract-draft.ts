"use client";

import {
  useIsMutating,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { api, type components } from "@/shared/api";

type Draft = components["schemas"]["ContractDraftResponse"];
type StageResponse = components["schemas"]["ContractStageResponse"];
type TermsUpdateRequest = components["schemas"]["TermsUpdateRequest"];
type TermValue = Exclude<
  components["schemas"]["ContractDraftResponseField"]["value"],
  undefined
>;

const draftKey = (contractId: number) =>
  ["contracts", contractId, "draft"] as const;
const draftWriteScope = (contractId: number) => ({
  id: `contract-draft:${contractId}`,
});

export function useContractDraft(contractId: number) {
  return useQuery({
    queryKey: draftKey(contractId),
    queryFn: async ({ signal }) => {
      const { data } = await api.get<Draft>(`/contracts/${contractId}/draft`, {
        signal,
      });
      if (!data?.stage) {
        throw new Error("Contract draft did not include its stage");
      }
      return data;
    },
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    refetchInterval: ({ state }) => {
      const processing = state.data?.processing;
      return state.status !== "error" &&
        processing?.task === "EXTRACTION" &&
        processing.status === "RUNNING"
        ? 2500
        : false;
    },
  });
}

export function useSaveDraftTerm(contractId: number) {
  const queryClient = useQueryClient();
  const queryKey = draftKey(contractId);
  const mutationKey = [...queryKey, "save-term"];
  const mutation = useMutation({
    mutationKey,
    scope: draftWriteScope(contractId),
    mutationFn: async ({ key, value }: { key: string; value: TermValue }) => {
      const request: TermsUpdateRequest = { values: { [key]: value } };
      const { data } = await api.patch<Draft>(
        `/contracts/${contractId}/draft/terms`,
        request,
      );
      return data;
    },
    onSuccess: async (draft) => {
      await queryClient.cancelQueries({ queryKey });
      queryClient.setQueryData(queryKey, draft);
    },
  });
  const isSaving = useIsMutating({ mutationKey, exact: true }) > 0;

  return { ...mutation, isSaving };
}

export function useRetryDraftExtraction(contractId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    scope: draftWriteScope(contractId),
    mutationFn: async () => {
      const { data } = await api.post<StageResponse>(
        `/contracts/${contractId}/extraction/retry`,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: draftKey(contractId) }),
  });
}

export function useReplaceDraftFile(contractId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    scope: draftWriteScope(contractId),
    mutationFn: async (file: File) => {
      const form = new FormData();
      form.append("file", file);
      const { data } = await api.put<StageResponse>(
        `/contracts/${contractId}/file`,
        form,
      );
      return data;
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: draftKey(contractId) }),
  });
}
