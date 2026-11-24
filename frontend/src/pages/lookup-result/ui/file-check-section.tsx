"use client";

import { useRef, useState } from "react";
import type { components } from "@/shared/api";
import { formatFileSize, shortenHash } from "@/shared/lib";
import { Alert, Button, FileIcon } from "@/shared/ui";
import type { LookupResultLabels } from "../model/labels";

type FileCheckResponse = components["schemas"]["FileCheckResponse"];

// 등록할 때 받는 형식과 같게 둔다
const ACCEPT = "application/pdf,image/jpeg,image/png";

export interface FileCheckSectionProps {
  /** 마지막으로 대조한 결과 (L2 응답) */
  result?: FileCheckResponse;
  isChecking?: boolean;
  /** 대조 실패 안내 (예: 400 FILE_HASH_INVALID_FORMAT) */
  errorMessage?: string;
  /** [대조하기]를 누르면 고른 파일로 불린다. 해시 계산과 L2 요청은 부르는 쪽에서 한다 */
  onCheck: (file: File) => void;
  labels: LookupResultLabels;
}

/** 가진 계약서 파일이 기록된 원본과 같은지 대조한다. 다른 파일을 고르면 이전 결과는 숨긴다. */
export function FileCheckSection({
  result,
  isChecking,
  errorMessage,
  onCheck,
  labels,
}: FileCheckSectionProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File>();
  const [checkedFile, setCheckedFile] = useState<File>();
  const showResult = !isChecking && file !== undefined && file === checkedFile;

  function handleCheck() {
    if (!file) {
      return;
    }
    setCheckedFile(file);
    onCheck(file);
  }

  return (
    <>
      <p className="text-sm text-muted">{labels.fileCheckHint}</p>
      <div className="flex flex-wrap items-center gap-3 rounded-md border border-dashed border-border-strong px-5 py-4">
        <FileIcon size={24} className="shrink-0 text-muted" />
        <span className="min-w-0 flex-auto">
          {file ? (
            <>
              <strong className="break-all">{file.name}</strong>{" "}
              <span className="text-sm text-muted">
                · {formatFileSize(file.size)}
              </span>
            </>
          ) : (
            <span className="text-muted">{labels.noFile}</span>
          )}
        </span>
        <input
          ref={inputRef}
          type="file"
          accept={ACCEPT}
          aria-label={labels.fileCheckInput}
          className="sr-only"
          tabIndex={-1}
          onChange={(event) => {
            const next = event.target.files?.[0];
            if (next) setFile(next);
            // 같은 파일을 다시 골라도 onChange가 불리도록 비운다
            event.target.value = "";
          }}
        />
        <Button
          variant="secondary"
          size="md"
          disabled={isChecking}
          onClick={() => inputRef.current?.click()}
        >
          {file ? labels.changeFile : labels.selectFile}
        </Button>
        <Button size="md" disabled={!file || isChecking} onClick={handleCheck}>
          {isChecking ? labels.checking : labels.check}
        </Button>
      </div>

      {showResult && errorMessage && (
        <Alert tone="danger" accent="left" title={errorMessage} />
      )}
      {showResult && !errorMessage && result && (
        <Alert
          tone={result.match ? "success" : "danger"}
          accent="left"
          title={result.match ? labels.matchTitle : labels.mismatchTitle}
        >
          <div className="flex flex-col gap-2">
            <span className="text-base">
              {result.match
                ? labels.matchDescription
                : labels.mismatchDescription}
            </span>
            <dl className="grid grid-cols-[140px_1fr] gap-x-3 gap-y-1">
              <dt className="text-muted">{labels.uploadedFileHash}</dt>
              <dd
                title={result.uploadedFileHash}
                className="font-mono break-all"
              >
                {result.uploadedFileHash &&
                  shortenHash(result.uploadedFileHash)}
              </dd>
              <dt className="text-muted">{labels.recordedFileHash}</dt>
              <dd
                title={result.recordedFileHash}
                className="font-mono break-all"
              >
                {result.recordedFileHash &&
                  shortenHash(result.recordedFileHash)}
              </dd>
            </dl>
            {!result.match && (
              <ul className="mt-1 flex list-disc flex-col gap-0.5 pl-5 text-foreground">
                {labels.mismatchTips.map((tip) => (
                  <li key={tip}>{tip}</li>
                ))}
              </ul>
            )}
          </div>
        </Alert>
      )}
    </>
  );
}
