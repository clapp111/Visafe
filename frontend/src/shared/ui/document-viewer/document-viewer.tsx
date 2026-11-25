"use client";

import type { ReactNode } from "react";
import { messages, useCurrentLanguage } from "@/shared/i18n";
import { cn } from "@/shared/lib";
import { ChevronLeftIcon, ChevronRightIcon } from "../icons";

export interface DocumentViewerLabels {
  previousPage: string;
  nextPage: string;
  /** 예: "1 / 2쪽" */
  pageStatus: (page: number, pageCount: number) => string;
  /** 미리보기 틀의 이름. 예: "원본 계약서 1쪽 미리보기" */
  preview: (page: number) => string;
  download: string;
}

const PAGE_BUTTON =
  "flex size-10 cursor-pointer items-center justify-center rounded-md border border-border-strong bg-surface text-foreground hover:bg-surface-muted disabled:cursor-not-allowed disabled:opacity-50";

export interface DocumentViewerProps {
  /** 제목 id. 미리보기를 감싸는 section이 이 제목으로 불린다 */
  id: string;
  title: ReactNode;
  /** 1부터 센 지금 쪽 */
  page: number;
  /** 전체 쪽 수. 문서를 읽기 전이면 undefined (쪽 넘기기를 막는다) */
  pageCount?: number;
  onPageChange: (page: number) => void;
  /** 쪽 내용 (PDF 쪽·이미지, 또는 읽는 중·실패 안내). A4 비율 틀 안에 놓인다 */
  children: ReactNode;
  /** 파일 정보 줄. 예: "원본_근로계약서.pdf · 2쪽 · 1.2MB" */
  fileInfo?: ReactNode;
  /** 있으면 파일 정보 줄 오른쪽에 내려받기 버튼을 둔다 */
  onDownload?: () => void;
  isDownloading?: boolean;
  /** 맨 아래 안내 */
  hint?: ReactNode;
  labels?: Partial<DocumentViewerLabels>;
  /** 예: 스크롤을 따라 고정할 때 "lg:sticky lg:top-6" (감싸는 줄도 lg부터 두 칸이 나란히 놓이게 lg:flex-nowrap) */
  className?: string;
}

/** 원본 문서를 쪽 단위로 보여 주는 틀. 문서를 읽고 그리는 일은 쓰는 쪽에서 children으로 넘긴다. */
export function DocumentViewer({
  id,
  title,
  page,
  pageCount,
  onPageChange,
  children,
  fileInfo,
  onDownload,
  isDownloading,
  hint,
  labels: labelOverrides,
  className,
}: DocumentViewerProps) {
  const text = messages[useCurrentLanguage()];
  const labels: DocumentViewerLabels = {
    previousPage: text.previousPage,
    nextPage: text.nextPage,
    pageStatus: text.pageStatus,
    preview: text.pagePreview,
    download: text.download,
    ...labelOverrides,
  };

  return (
    <section
      aria-labelledby={id}
      className={cn("flex min-w-0 flex-col gap-3", className)}
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id={id} className="text-xl font-bold">
          {title}
        </h2>
        <div className="flex items-center gap-2 text-sm">
          <button
            type="button"
            aria-label={labels.previousPage}
            disabled={pageCount === undefined || page <= 1}
            onClick={() => onPageChange(page - 1)}
            className={PAGE_BUTTON}
          >
            <ChevronLeftIcon size={18} />
          </button>
          <span aria-live="polite" className="min-w-14 text-center">
            {pageCount !== undefined && labels.pageStatus(page, pageCount)}
          </span>
          <button
            type="button"
            aria-label={labels.nextPage}
            disabled={pageCount === undefined || page >= pageCount}
            onClick={() => onPageChange(page + 1)}
            className={PAGE_BUTTON}
          >
            <ChevronRightIcon size={18} />
          </button>
        </div>
      </div>

      <figure
        aria-label={labels.preview(page)}
        className="rounded-md border border-border bg-surface-muted p-5"
      >
        <div className="flex aspect-[210/297] items-center justify-center overflow-hidden border border-divider bg-surface">
          {children}
        </div>
      </figure>

      {(fileInfo || onDownload) && (
        <p className="flex flex-wrap justify-between gap-3 text-sm text-muted">
          <span className="min-w-0 break-all">{fileInfo}</span>
          {onDownload && (
            <button
              type="button"
              disabled={isDownloading}
              onClick={onDownload}
              className="cursor-pointer text-primary underline disabled:cursor-not-allowed disabled:opacity-50"
            >
              {labels.download}
            </button>
          )}
        </p>
      )}
      {hint && <p className="text-sm text-muted">{hint}</p>}
    </section>
  );
}
