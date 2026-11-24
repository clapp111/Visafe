"use client";

import { useMutation } from "@tanstack/react-query";
import { api, type components } from "@/shared/api";

type ContractCreateRequest = components["schemas"]["ContractCreateRequest"];
type ContractStageResponse = components["schemas"]["ContractStageResponse"];
type CreateContractInput = Pick<ContractCreateRequest, "workerMemberId"> & {
  file: File;
};

export function useCreateContract() {
  return useMutation({
    mutationFn: async ({ file, workerMemberId }: CreateContractInput) => {
      const request: ContractCreateRequest = { workerMemberId };
      const form = new FormData();
      form.append("file", file);
      form.append(
        "data",
        new Blob([JSON.stringify(request)], { type: "application/json" }),
      );

      const { data } = await api.post<ContractStageResponse>(
        "/contracts",
        form,
      );
      const contractId = data?.contractId;
      if (contractId === undefined) {
        throw new Error("Contract creation response did not include an ID");
      }

      return { ...data, contractId };
    },
    networkMode: "always",
    retry: false,
  });
}
