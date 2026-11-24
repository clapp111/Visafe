"use client";

import Link from "next/link";
import { useState } from "react";
import { routes } from "@/shared/config";
import { ContractFileViewer } from "@/widgets/contract-file-viewer";
import { RegisterSteps } from "@/widgets/register-steps";
import { getDraftFixture } from "../model/fixtures";
import { CONTRACT_DRAFT_LABELS } from "../model/labels";
import { AttentionSummary } from "./attention-summary";
import { ExtractionState } from "./extraction-state";
import { ReturnedNotice } from "./returned-notice";
import { ReuploadDialog } from "./reupload-dialog";
import { TermFieldsForm } from "./term-fields-form";

export function ContractDraftPage({ contractId }: { contractId: number }) {
  const labels = CONTRACT_DRAFT_LABELS;
  // R4 검토 항목 조회·R5 저장·R3·R2를 연결하기 전까지 예시값으로 화면을 보인다
  const draft = getDraftFixture(contractId);
  const [page, setPage] = useState(1);
  const [reuploadOpen, setReuploadOpen] = useState(false);
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
              onRetry={() => {}}
              onReupload={() => setReuploadOpen(true)}
              labels={labels}
            />
          ) : (
            <TermFieldsForm
              fields={fields}
              reportedKeys={draft.returned?.reportedFields}
              onCommitField={() => {}}
              onShowSource={setPage}
              onSubmit={() => {}}
              onReupload={() => setReuploadOpen(true)}
              labels={labels}
            />
          )}
        </div>
      </div>

      {reuploadOpen && (
        <ReuploadDialog
          open
          onClose={() => setReuploadOpen(false)}
          onSubmit={() => setReuploadOpen(false)}
          labels={labels}
        />
      )}
    </div>
  );
}
