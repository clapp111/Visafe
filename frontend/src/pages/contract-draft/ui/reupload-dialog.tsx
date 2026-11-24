"use client";

import { useState } from "react";
import { CONTRACT_FILE_ACCEPT, checkContractFile } from "@/entities/contract";
import { formatFileSize } from "@/shared/lib";
import {
  Button,
  Dialog,
  Dropzone,
  FieldMessage,
  FileItem,
  FileList,
} from "@/shared/ui";
import type { ContractDraftLabels } from "../model/labels";

const FILE_ID = "contract-draft-reupload";

export interface ReuploadDialogProps {
  open: boolean;
  onClose: () => void;
  /** 형식·크기 확인을 통과한 새 원본으로 불린다 (R2 원본 다시 올리기) */
  onSubmit: (file: File) => void;
  isUploading?: boolean;
  /** 서버 안내 (413 FILE_TOO_LARGE·415 FILE_UNSUPPORTED_TYPE·409 CONTRACT_STAGE_CONFLICT) */
  errorMessage?: string;
  labels: ContractDraftLabels;
}

/** 원본을 다시 올리는 창. 올리면 항목이 지워지고 다시 추출한다. */
export function ReuploadDialog({
  open,
  onClose,
  onSubmit,
  isUploading,
  errorMessage,
  labels,
}: ReuploadDialogProps) {
  const [file, setFile] = useState<File>();
  const [fileError, setFileError] = useState<string>();
  const message = fileError ?? errorMessage;

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

  function handleSubmit() {
    if (!file) {
      setFileError(labels.fileRequired);
      return;
    }
    onSubmit(file);
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={labels.reuploadTitle}
      description={labels.reuploadDescription}
      size="lg"
      actions={
        <>
          <Button variant="secondary" size="md" onClick={onClose}>
            {labels.cancel}
          </Button>
          <Button size="md" disabled={isUploading} onClick={handleSubmit}>
            {isUploading ? labels.uploading : labels.upload}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-2">
        <span id={`${FILE_ID}-label`} className="font-bold">
          {labels.reuploadFileLabel}
        </span>
        <p id={`${FILE_ID}-hint`} className="text-sm text-muted">
          {labels.reuploadFileHint}
        </p>
        <Dropzone
          id={FILE_ID}
          accept={CONTRACT_FILE_ACCEPT}
          disabled={isUploading}
          onFiles={handleFiles}
          labelledBy={`${FILE_ID}-label`}
          describedBy={
            message ? `${FILE_ID}-hint ${FILE_ID}-message` : `${FILE_ID}-hint`
          }
          dropLabel={labels.dropLabel}
          selectLabel={labels.selectFile}
        />
        {message && (
          <FieldMessage id={`${FILE_ID}-message`}>{message}</FieldMessage>
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
                disabled={isUploading}
                onClick={() => setFile(undefined)}
              >
                {labels.removeFile}
              </Button>
            }
          />
        </FileList>
      )}
    </Dialog>
  );
}
