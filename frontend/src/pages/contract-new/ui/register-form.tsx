"use client";

import { type FormEvent, useState } from "react";
import { CONTRACT_FILE_ACCEPT, checkContractFile } from "@/entities/contract";
import type { components } from "@/shared/api";
import { formatFileSize } from "@/shared/lib";
import {
  Alert,
  ArrowRightIcon,
  Button,
  Dropzone,
  FieldMessage,
  FileItem,
  FileList,
} from "@/shared/ui";
import type { ContractNewLabels } from "../model/labels";
import { WorkerPicker } from "./worker-picker";

type WorkerSearchResponse = components["schemas"]["WorkerSearchResponse"];

const FILE_ID = "contract-new-file";

export interface RegisterValues {
  file: File;
  workerMemberId: number;
}

/** next: [다음: AI 항목 추출] → 2단계, draft: [임시 저장] → 내 계약 */
export type RegisterIntent = "next" | "draft";

export interface RegisterFormProps {
  workers?: WorkerSearchResponse[];
  isSearchingWorkers?: boolean;
  onSearchWorkers: (name: string) => void;
  /** 필수 항목과 파일 형식·크기 확인을 통과한 값으로 불린다 (R1 계약 생성) */
  onSubmit: (values: RegisterValues, intent: RegisterIntent) => void;
  isSubmitting?: boolean;
  /** 폼 위에 보일 실패 안내 */
  errorMessage?: string;
  /** 원본 파일 아래 안내 (413 FILE_TOO_LARGE·415 FILE_UNSUPPORTED_TYPE) */
  fileErrorMessage?: string;
  /** 근로자 아래 안내 (404 WORKER_NOT_FOUND) */
  workerErrorMessage?: string;
  labels: ContractNewLabels;
}

export function RegisterForm({
  workers,
  isSearchingWorkers,
  onSearchWorkers,
  onSubmit,
  isSubmitting,
  errorMessage,
  fileErrorMessage,
  workerErrorMessage,
  labels,
}: RegisterFormProps) {
  const [file, setFile] = useState<File>();
  const [fileError, setFileError] = useState<string>();
  const [workerMemberId, setWorkerMemberId] = useState<number>();
  const [workerError, setWorkerError] = useState<string>();
  const fileMessage = fileError ?? fileErrorMessage;
  const workerMessage = workerError ?? workerErrorMessage;

  function handleFiles([next]: File[]) {
    const problem = checkContractFile(next);
    if (problem) {
      setFileError(
        problem === "type" ? labels.fileTypeInvalid : labels.fileTooLarge,
      );
      return;
    }
    setFileError(undefined);
    setFile(next);
  }

  function submit(intent: RegisterIntent) {
    if (!file) setFileError(labels.fileRequired);
    if (workerMemberId === undefined) setWorkerError(labels.workerRequired);
    if (!file || workerMemberId === undefined || isSubmitting) return;
    onSubmit({ file, workerMemberId }, intent);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    submit("next");
  }

  return (
    <form
      noValidate
      onSubmit={handleSubmit}
      className="mx-auto flex w-full max-w-[880px] flex-col gap-12"
    >
      {errorMessage && (
        <Alert tone="danger" accent="left" title={errorMessage} />
      )}

      <p className="text-sm text-muted">
        <span aria-hidden="true" className="mr-0.5 text-danger">
          *
        </span>
        {labels.requiredNotice}
      </p>

      <section
        aria-labelledby="contract-new-file-title"
        className="flex flex-col gap-6 border-t-2 border-foreground pt-6"
      >
        <h2
          id="contract-new-file-title"
          className="text-2xl leading-normal font-bold"
        >
          {labels.fileTitle}
        </h2>

        <div className="flex min-w-0 flex-col gap-2">
          <span id={`${FILE_ID}-label`} className="font-bold">
            {labels.fileLabel}
            <span aria-hidden="true" className="ml-0.5 text-danger">
              *
            </span>
            <span className="sr-only">{labels.required}</span>
          </span>
          <p id={`${FILE_ID}-hint`} className="text-sm text-muted">
            {labels.fileHint}
          </p>
          <Dropzone
            id={FILE_ID}
            accept={CONTRACT_FILE_ACCEPT}
            disabled={isSubmitting}
            onFiles={handleFiles}
            labelledBy={`${FILE_ID}-label`}
            describedBy={
              fileMessage
                ? `${FILE_ID}-hint ${FILE_ID}-message`
                : `${FILE_ID}-hint`
            }
            dropLabel={labels.dropLabel}
            selectLabel={labels.selectFile}
          />
          {fileMessage && (
            <FieldMessage id={`${FILE_ID}-message`}>{fileMessage}</FieldMessage>
          )}
        </div>

        {file && (
          <FileList label={labels.fileListLabel}>
            <FileItem
              name={file.name}
              meta={formatFileSize(file.size)}
              action={
                <Button
                  variant="secondary"
                  size="md"
                  disabled={isSubmitting}
                  onClick={() => setFile(undefined)}
                >
                  {labels.removeFile}
                </Button>
              }
            />
          </FileList>
        )}

        <div className="flex flex-col gap-2 rounded-lg border border-border px-6 py-5">
          <strong>{labels.photoTipsTitle}</strong>
          <ul className="flex list-disc flex-col gap-1 pl-5 text-sm text-muted">
            {labels.photoTips.map((tip) => (
              <li key={tip}>{tip}</li>
            ))}
          </ul>
        </div>
      </section>

      <section
        aria-labelledby="contract-new-worker-title"
        className="flex flex-col gap-6 border-t-2 border-foreground pt-6"
      >
        <h2
          id="contract-new-worker-title"
          className="text-2xl leading-normal font-bold"
        >
          {labels.workerTitle}
        </h2>
        <WorkerPicker
          workers={workers}
          isSearching={isSearchingWorkers}
          onSearch={(name) => {
            // 다시 검색하면 고른 근로자를 취소한다 (새 결과에 없는 사람이 선택된 채로 남지 않게)
            setWorkerMemberId(undefined);
            onSearchWorkers(name);
          }}
          selectedId={workerMemberId}
          onSelect={(memberId) => {
            setWorkerMemberId(memberId);
            setWorkerError(undefined);
          }}
          errorMessage={workerMessage}
          labels={labels}
        />
      </section>

      <Alert tone="info" title={labels.noticeTitle}>
        {labels.notice}
      </Alert>

      <div className="flex flex-wrap items-center justify-between gap-4 border-t border-border pt-8">
        <Button
          variant="secondary"
          size="lg"
          disabled={isSubmitting}
          onClick={() => submit("draft")}
        >
          {labels.saveDraft}
        </Button>
        <Button
          type="submit"
          size="lg"
          disabled={isSubmitting}
          className="px-8 text-lg"
        >
          {isSubmitting ? labels.submitting : labels.next}
          <ArrowRightIcon />
        </Button>
      </div>
    </form>
  );
}
