"use client";

import dynamic from "next/dynamic";
import { useRef, useState } from "react";
import type { ReactNode } from "react";
import {
  useDownloadContractFile,
  useFileDownloadUrl,
} from "@/entities/contract";
import { getApiErrorCode, getApiErrorFeedback } from "@/shared/api";
import { cn, formatFileSize } from "@/shared/lib";
import {
  DocumentViewer,
  ErrorState,
  LoadingState,
  type DocumentViewerProps,
} from "@/shared/ui";

const loading = <LoadingState label="원본 계약서를 불러오는 중입니다" />;
const PdfPreview = dynamic(
  () => import("./pdf-preview").then((module) => module.PdfPreview),
  { ssr: false, loading: () => loading },
);

export type ContractFileViewerProps = Pick<
  DocumentViewerProps,
  "id" | "page" | "onPageChange" | "hint" | "labels" | "className"
> & {
  contractId: number;
  fileId: number;
  title?: DocumentViewerProps["title"];
};

type Preview = NonNullable<ReturnType<typeof useFileDownloadUrl>["data"]>;

function FileViewer({
  contractId,
  fileId,
  page,
  title = "원본 계약서",
  hint,
  ...viewerProps
}: ContractFileViewerProps) {
  const query = useFileDownloadUrl(contractId, fileId);
  const download = useDownloadContractFile(contractId, fileId);
  const [loaded, setLoaded] = useState<{ file: Preview; pageCount: number }>();
  const [failedUrl, setFailedUrl] = useState<string>();
  const renewed = useRef(false);
  const file = loaded?.file ?? query.data;
  const isImage =
    file?.mimeType === "image/jpeg" || file?.mimeType === "image/png";
  const pageCount = isImage ? 1 : loaded?.pageCount;
  const currentPage = Math.max(1, Math.min(Math.trunc(page), pageCount ?? 1));

  function retryPreview() {
    renewed.current = false;
    setLoaded(undefined);
    setFailedUrl(undefined);
    void query.refetch();
  }

  function handleReadError(source: Preview) {
    if (
      !loaded &&
      !renewed.current &&
      Date.parse(source.expiresAt) <= Date.now()
    ) {
      renewed.current = true;
      void query.refetch();
      return;
    }
    setFailedUrl(source.url);
  }

  let content: ReactNode;
  if (!loaded && query.isFetching) {
    content = loading;
  } else if (!loaded && query.isError) {
    content = (
      <ErrorState
        title="원본 계약서를 불러오지 못했습니다"
        message={
          getApiErrorFeedback(getApiErrorCode(query.error), "ko").message
        }
        onRetry={retryPreview}
      />
    );
  } else if (!file) {
    content = loading;
  } else if (failedUrl === file.url) {
    content = (
      <ErrorState title="원본 파일을 읽지 못했습니다" onRetry={retryPreview} />
    );
  } else if (file.mimeType === "application/pdf") {
    content = (
      <PdfPreview
        key={file.url}
        url={file.url}
        page={currentPage}
        onReady={(pageCount) => setLoaded({ file, pageCount })}
        onError={() => handleReadError(file)}
      />
    );
  } else if (isImage) {
    content = (
      <>
        {!loaded && loading}
        {/* eslint-disable-next-line @next/next/no-img-element -- Load the signed image URL directly from storage. */}
        <img
          key={file.url}
          src={file.url}
          alt={file.fileName ?? "원본 계약서"}
          className={cn(
            "max-h-full max-w-full object-contain",
            !loaded && "hidden",
          )}
          onLoad={() => setLoaded({ file, pageCount: 1 })}
          onError={() => handleReadError(file)}
        />
      </>
    );
  } else {
    content = <ErrorState title="미리보기를 지원하지 않는 파일 형식입니다" />;
  }

  const fileInfo = file
    ? [
        file.fileName,
        pageCount === undefined ? undefined : `${pageCount}쪽`,
        file.sizeBytes === undefined
          ? undefined
          : formatFileSize(file.sizeBytes),
      ]
        .filter(Boolean)
        .join(" · ")
    : undefined;

  return (
    <DocumentViewer
      {...viewerProps}
      title={title}
      page={currentPage}
      pageCount={pageCount}
      fileInfo={fileInfo}
      onDownload={() => download.mutate()}
      isDownloading={download.isPending}
      hint={
        download.isError ? (
          <>
            {hint}
            <span role="alert" className="block text-danger">
              {
                getApiErrorFeedback(getApiErrorCode(download.error), "ko")
                  .message
              }
            </span>
          </>
        ) : (
          hint
        )
      }
    >
      {content}
    </DocumentViewer>
  );
}

export function ContractFileViewer(props: ContractFileViewerProps) {
  return <FileViewer key={`${props.contractId}:${props.fileId}`} {...props} />;
}
