"use client";

import { isAxiosError } from "axios";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  useContractDraft,
  useReplaceDraftFile,
  useRetryDraftExtraction,
  useSaveDraftTerm,
} from "@/entities/contract";
import {
  getApiErrorCode,
  getApiErrorFeedback,
  type components,
} from "@/shared/api";
import { routes } from "@/shared/config";
import { ErrorState, FieldMessage, LoadingState } from "@/shared/ui";
import { ContractFileViewer } from "@/widgets/contract-file-viewer";
import { RegisterSteps } from "@/widgets/register-steps";
import { CONTRACT_DRAFT_LABELS } from "../model/labels";
import { AttentionSummary } from "./attention-summary";
import { ExtractionState } from "./extraction-state";
import { ReturnedNotice } from "./returned-notice";
import { ReuploadDialog } from "./reupload-dialog";
import { TermFieldsForm } from "./term-fields-form";
import type { TermValue } from "./term-field-input";

function getDraftErrorMessage(error: unknown) {
  const code = getApiErrorCode(error);
  if (
    code === "TERM_VALUE_INVALID" &&
    isAxiosError<components["schemas"]["ErrorResponse"]>(error) &&
    error.response?.status === 400
  ) {
    const reason = error.response.data?.message;
    if (typeof reason === "string" && reason.trim()) {
      return reason;
    }
  }
  return getApiErrorFeedback(code, "ko").message;
}

export function ContractDraftPage({ contractId }: { contractId: number }) {
  const labels = CONTRACT_DRAFT_LABELS;
  const router = useRouter();
  const draftQuery = useContractDraft(contractId);
  const saveTerm = useSaveDraftTerm(contractId);
  const retryExtraction = useRetryDraftExtraction(contractId);
  const replaceFile = useReplaceDraftFile(contractId);
  const [page, setPage] = useState(1);
  const [reuploadOpen, setReuploadOpen] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const draft = draftQuery.data;
  const shouldRedirect =
    (draft !== undefined &&
      draft.stage !== "UPLOADED" &&
      draft.stage !== "REVIEWING") ||
    getApiErrorCode(draftQuery.error) === "CONTRACT_STAGE_CONFLICT";

  useEffect(() => {
    if (shouldRedirect) {
      router.replace(routes.contract(contractId));
    }
  }, [contractId, router, shouldRedirect]);

  async function handleCommitField(key: string, value: TermValue) {
    if (draft?.stage !== "REVIEWING" || value === undefined) {
      return;
    }
    try {
      await saveTerm.mutateAsync({ key, value });
      setFieldErrors((current) => {
        const next = { ...current };
        delete next[key];
        return next;
      });
    } catch (error) {
      setFieldErrors((current) => ({
        ...current,
        [key]: getDraftErrorMessage(error),
      }));
      if (getApiErrorCode(error) === "CONTRACT_STAGE_CONFLICT") {
        void draftQuery.refetch();
      }
    }
  }

  function openReupload() {
    replaceFile.reset();
    setReuploadOpen(true);
  }

  function closeReupload() {
    if (!replaceFile.isPending) {
      setReuploadOpen(false);
    }
  }

  function handleReupload(file: File) {
    replaceFile.mutate(file, {
      onSuccess: () => {
        retryExtraction.reset();
        setFieldErrors({});
        setPage(1);
        setReuploadOpen(false);
      },
    });
  }

  if (shouldRedirect || draftQuery.isPending) {
    return <LoadingState label={labels.loading} />;
  }
  if (draftQuery.isError || !draft) {
    return (
      <ErrorState
        title={labels.loadError}
        message={getDraftErrorMessage(draftQuery.error)}
        onRetry={() => void draftQuery.refetch()}
      />
    );
  }

  const fields = draft.fields ?? [];
  const attention = draft.attention ?? [];
  const extraction =
    draft.processing?.task === "EXTRACTION" ? draft.processing : undefined;

  return (
    <div className="mx-auto max-w-[1200px] px-6 pt-6 pb-20">
      <nav aria-label={labels.breadcrumb} className="mb-8 text-sm text-muted">
        <Link href={routes.home} className="text-muted underline">
          {labels.home}
        </Link>
        <span aria-hidden="true" className="mx-2">
          &gt;
        </span>
        <span aria-current="page">{labels.register}</span>
      </nav>

      <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">
        {labels.title}
      </h1>
      <p className="mb-8 max-w-[820px] text-lg text-muted">
        {labels.description[0]}
        <br />
        {labels.description[1]}
      </p>

      <RegisterSteps current={2} />

      {draft.returned && (
        <ReturnedNotice
          returned={draft.returned}
          fields={fields}
          labels={labels}
        />
      )}
      {!extraction && attention.length > 0 && (
        <AttentionSummary
          attention={attention}
          fields={fields}
          labels={labels}
        />
      )}

      <div className="flex flex-wrap items-start gap-10">
        {draft.contractId !== undefined && draft.file?.fileId !== undefined && (
          <ContractFileViewer
            id="contract-draft-original"
            contractId={draft.contractId}
            fileId={draft.file.fileId}
            page={page}
            onPageChange={setPage}
            hint={labels.viewerHint}
            labels={{
              preview: labels.viewerPreview,
              download: labels.viewerDownload,
            }}
            className="flex-[1_1_440px] lg:sticky lg:top-6"
          />
        )}

        <div className="flex min-w-0 flex-[1_1_560px] flex-col">
          {extraction ? (
            <ExtractionState
              failed={extraction.status === "FAILED"}
              onRetry={() => retryExtraction.mutate()}
              isRetrying={retryExtraction.isPending}
              onReupload={openReupload}
              labels={labels}
            />
          ) : (
            <TermFieldsForm
              fields={fields}
              reportedKeys={draft.returned?.reportedFields}
              fieldErrors={fieldErrors}
              onCommitField={handleCommitField}
              onShowSource={setPage}
              onSubmit={() => {}}
              onReupload={openReupload}
              isSaving={saveTerm.isSaving}
              labels={labels}
            />
          )}
          {extraction?.status === "FAILED" && retryExtraction.isError && (
            <FieldMessage status="error">
              {getDraftErrorMessage(retryExtraction.error)}
            </FieldMessage>
          )}
        </div>
      </div>

      {reuploadOpen && (
        <ReuploadDialog
          open
          onClose={closeReupload}
          onSubmit={handleReupload}
          isUploading={replaceFile.isPending}
          errorMessage={
            replaceFile.isError
              ? getDraftErrorMessage(replaceFile.error)
              : undefined
          }
          labels={labels}
        />
      )}
    </div>
  );
}
