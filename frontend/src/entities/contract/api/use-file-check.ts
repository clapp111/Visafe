"use client";

import { useMutation } from "@tanstack/react-query";
import { authApi, type components } from "@/shared/api";

type FileCheckRequest = components["schemas"]["FileCheckRequest"];
type FileCheckResponse = components["schemas"]["FileCheckResponse"];

export function useFileCheck(confirmationNo: string) {
  return useMutation({
    mutationFn: async (request: FileCheckRequest) => {
      const { data } = await authApi.post<FileCheckResponse>(
        `/lookup/${encodeURIComponent(confirmationNo)}/file-check`,
        request,
      );
      return data;
    },
    networkMode: "always",
  });
}
