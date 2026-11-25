"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  useAdminContractDetail,
  useApproveAdminContract,
  useRejectAdminContract,
  termLabel,
} from "@/entities/contract";
import { getApiErrorCode, getApiErrorFeedback } from "@/shared/api";
import { routes } from "@/shared/config";
import { Button, ErrorState, LoadingState } from "@/shared/ui";
import { ContractFileViewer } from "@/widgets/contract-file-viewer";
import { HistoryList } from "@/widgets/contract-records";
import { useCurrentLanguage } from "@/shared/i18n";
import { ADMIN_CONTRACT_REVIEW_LABELS } from "../model/labels";
import { ApproveDialog } from "./approve-dialog";
import { AttentionAlert } from "./attention-alert";
import { EditLogs } from "./edit-logs";
import { FieldsTable } from "./fields-table";
import { RejectDialog } from "./reject-dialog";

export function AdminContractReviewPage({
  contractId,
}: {
  contractId: number;
}) {
  const language = useCurrentLanguage();
  const labels = ADMIN_CONTRACT_REVIEW_LABELS[language];
  const router = useRouter();
  const detailQuery = useAdminContractDetail(contractId);
  const approve = useApproveAdminContract(contractId);
  const reject = useRejectAdminContract(contractId);
  const [page, setPage] = useState(1);
  const [dialog, setDialog] = useState<"approve" | "reject">();

  if (detailQuery.isPending) {
    return <LoadingState label={labels.loading} />;
  }
  if (detailQuery.isError) {
    return (
      <ErrorState
        title={labels.loadError}
        message={
          getApiErrorFeedback(getApiErrorCode(detailQuery.error), language)
            .message
        }
        onRetry={() => void detailQuery.refetch()}
      />
    );
  }

  const detail = detailQuery.data;
  // 서버 항목 이름은 한국어뿐이라 화면 언어에 맞게 바꿔 둔다
  const fields = (detail.fields ?? []).map((field) => ({
    ...field,
    label: termLabel(field.key, field.label, language),
  }));
  const fieldLabels = new Map(fields.map((field) => [field.key, field.label]));
  const canDecide = detail.stage === "AUTHORITY_REVIEW";

  function openDecision(next: "approve" | "reject") {
    if (approve.isPending || reject.isPending) return;
    if (next === "approve") approve.reset();
    else reject.reset();
    setDialog(next);
  }

  const summary = [
    { label: labels.summary.contractNo, value: detail.contractNo },
    { label: labels.summary.employer, value: detail.employer?.companyName },
    { label: labels.summary.worker, value: detail.worker?.name, lang: "vi" },
    {
      label: labels.summary.stage,
      value: detail.stage && labels.stages[detail.stage],
    },
  ];

  return (
    <div className="mx-auto max-w-[1200px] px-6 pt-6 pb-20">
      <nav aria-label={labels.breadcrumb} className="mb-8 text-sm text-muted">
        <Link href={routes.home} className="text-muted underline">
          {labels.home}
        </Link>
        <span aria-hidden="true" className="mx-2">
          &gt;
        </span>
        <Link href={routes.adminContracts} className="text-muted underline">
          {labels.list}
        </Link>
        <span aria-hidden="true" className="mx-2">
          &gt;
        </span>
        <span aria-current="page">{labels.title}</span>
      </nav>

      <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">
        {labels.title}
      </h1>
      <p className="mb-6 text-lg text-muted">{labels.description}</p>

      <dl className="mb-8 flex flex-wrap gap-x-10 gap-y-3 rounded-md border border-border px-6 py-5">
        {summary.map(
          (row) =>
            row.value && (
              <div key={row.label} className="flex gap-3">
                <dt className="text-muted">{row.label}</dt>
                <dd lang={row.lang} className="font-bold">
                  {row.value}
                </dd>
              </div>
            ),
        )}
      </dl>

      <div className="flex flex-wrap items-start gap-10 lg:flex-nowrap">
        {detail.file?.fileId !== undefined && (
          <ContractFileViewer
            id="admin-contract-original"
            contractId={contractId}
            fileId={detail.file.fileId}
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

        <div className="flex min-w-0 flex-[1_1_560px] flex-col gap-10">
          <AttentionAlert fields={fields} labels={labels} />

          <section
            aria-labelledby="admin-review-fields"
            className="flex flex-col gap-4"
          >
            <h2
              id="admin-review-fields"
              className="text-2xl leading-normal font-bold"
            >
              {labels.fieldsTitle}
            </h2>
            <FieldsTable
              fields={fields}
              onShowSource={setPage}
              labels={labels}
            />
          </section>

          <section
            aria-labelledby="admin-review-edits"
            className="flex flex-col gap-4 border-t-2 border-foreground pt-6"
          >
            <h2
              id="admin-review-edits"
              className="text-2xl leading-normal font-bold"
            >
              {labels.editLogsTitle}
            </h2>
            <EditLogs
              logs={detail.editLogs ?? []}
              fieldLabels={fieldLabels}
              labels={labels}
            />
          </section>

          {(detail.timeline?.length ?? 0) > 0 && (
            <section
              aria-labelledby="admin-review-history"
              className="flex flex-col gap-4 border-t-2 border-foreground pt-6"
            >
              <h2
                id="admin-review-history"
                className="text-2xl leading-normal font-bold"
              >
                {labels.historyTitle}
              </h2>
              <HistoryList history={detail.timeline ?? []} />
            </section>
          )}

          <section
            aria-labelledby="admin-review-action"
            className="flex flex-col gap-4 rounded-lg border-2 border-primary p-7"
          >
            <h2 id="admin-review-action" className="text-2xl font-bold">
              {canDecide ? labels.actionTitle : labels.reviewedTitle}
            </h2>
            <p className="text-muted">
              {canDecide
                ? labels.actionDescription
                : labels.reviewedDescription}
            </p>
            {canDecide && (
              <div className="flex flex-wrap justify-end gap-3">
                <Button variant="danger" onClick={() => openDecision("reject")}>
                  {labels.reject}
                </Button>
                <Button onClick={() => openDecision("approve")}>
                  {labels.approve}
                </Button>
              </div>
            )}
          </section>
        </div>
      </div>

      {dialog === "approve" && (
        <ApproveDialog
          detail={detail}
          onClose={() => setDialog(undefined)}
          onConfirm={() =>
            approve.mutate(undefined, {
              onSuccess: () =>
                router.push(routes.adminContractDone(contractId)),
            })
          }
          isSubmitting={approve.isPending}
          errorMessage={
            approve.isError
              ? getApiErrorFeedback(getApiErrorCode(approve.error), language)
                  .message
              : undefined
          }
          labels={labels}
        />
      )}
      {dialog === "reject" && (
        <RejectDialog
          onClose={() => setDialog(undefined)}
          onSubmit={(reason) =>
            reject.mutate(reason, {
              onSuccess: () => router.push(routes.adminContracts),
            })
          }
          isSubmitting={reject.isPending}
          errorMessage={
            reject.isError
              ? getApiErrorFeedback(getApiErrorCode(reject.error), language)
                  .message
              : undefined
          }
          labels={labels}
        />
      )}
    </div>
  );
}
