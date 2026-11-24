"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { api, type components } from "@/shared/api";

type FileDownloadUrl = components["schemas"]["FileDownloadUrlResponse"];
const PREVIEW_CACHE_MS = 4 * 60 * 1000;
const EXPIRY_MARGIN_MS = 30 * 1000;

async function getFileDownloadUrl(
  contractId: number,
  fileId: number,
  inline: boolean,
  signal?: AbortSignal,
) {
  const { data } = await api.get<FileDownloadUrl>(
    `/contracts/${contractId}/files/${fileId}/download-url`,
    { params: inline ? { inline: true } : undefined, signal },
  );
  if (
    !data?.url ||
    !data.expiresAt ||
    !Number.isFinite(Date.parse(data.expiresAt))
  ) {
    throw new Error("Invalid file download URL response");
  }
  return { ...data, url: data.url, expiresAt: data.expiresAt };
}

export function useFileDownloadUrl(contractId: number, fileId: number) {
  return useQuery({
    queryKey: ["contracts", contractId, "files", fileId, "preview-url"],
    queryFn: ({ signal }) =>
      getFileDownloadUrl(contractId, fileId, true, signal),
    staleTime: ({ state }) =>
      state.data
        ? Math.max(
            0,
            Math.min(
              PREVIEW_CACHE_MS,
              Date.parse(state.data.expiresAt) -
                state.dataUpdatedAt -
                EXPIRY_MARGIN_MS,
            ),
          )
        : 0,
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}

export function useDownloadContractFile(
  contractId: number,
  fileId: number | undefined,
) {
  return useMutation({
    mutationFn: () => {
      if (fileId === undefined) {
        throw new Error("Contract detail did not include a file ID");
      }
      return getFileDownloadUrl(contractId, fileId, false);
    },
    onSuccess: ({ url }) => window.location.assign(url),
  });
}
