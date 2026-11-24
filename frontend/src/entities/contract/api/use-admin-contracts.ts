"use client";

import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  api,
  type components,
  getApiErrorCode,
  type paths,
} from "@/shared/api";

type List = components["schemas"]["AdminContractListResponse"];
type Detail = components["schemas"]["AdminContractDetailResponse"];
type StageResponse = components["schemas"]["ContractStageResponse"];
type RejectRequest = components["schemas"]["AuthorityRejectRequest"];
type ListParams = NonNullable<
  paths["/api/v1/admin/contracts"]["get"]["parameters"]["query"]
>;

const listKey = ["admin", "contracts", "list"] as const;
const detailKey = (contractId: number) =>
  ["admin", "contracts", contractId, "detail"] as const;
const PAGE_SIZE = 20;

export function useAdminContractList(
  view: NonNullable<ListParams["view"]>,
  q: string,
  page: number,
) {
  return useQuery({
    queryKey: [...listKey, view, q, page],
    queryFn: async ({ signal }) => {
      const params: ListParams = {
        view,
        q: q || undefined,
        page,
        size: PAGE_SIZE,
      };
      const { data } = await api.get<List>("/admin/contracts", {
        params,
        signal,
      });
      return data;
    },
    placeholderData: keepPreviousData,
    retry: false,
  });
}

export function useAdminContractDetail(contractId: number) {
  return useQuery({
    queryKey: detailKey(contractId),
    queryFn: async ({ signal }) => {
      const { data } = await api.get<Detail>(`/admin/contracts/${contractId}`, {
        signal,
      });
      return data;
    },
    retry: false,
  });
}

function useAdminDecision<Variables>(
  contractId: number,
  mutationFn: (variables: Variables) => Promise<StageResponse>,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: listKey });
    },
    onError: (error) => {
      const code = getApiErrorCode(error);
      if (
        code === "CONTRACT_STAGE_CONFLICT" ||
        code === "AUTHORITY_ALREADY_REVIEWED"
      ) {
        void queryClient.invalidateQueries({ queryKey: detailKey(contractId) });
      }
    },
  });
}

export function useApproveAdminContract(contractId: number) {
  return useAdminDecision<void>(contractId, async () => {
    const { data } = await api.post<StageResponse>(
      `/admin/contracts/${contractId}/approve`,
    );
    return data;
  });
}

export function useRejectAdminContract(contractId: number) {
  return useAdminDecision(contractId, async (reason: string) => {
    const request: RejectRequest = { reason };
    const { data } = await api.post<StageResponse>(
      `/admin/contracts/${contractId}/reject`,
      request,
    );
    return data;
  });
}
