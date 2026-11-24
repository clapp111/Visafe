"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  useConfirmContractReview,
  useContractReview,
} from "@/entities/contract";
import { getApiErrorCode, getApiErrorFeedback } from "@/shared/api";
import { routes } from "@/shared/config";
import { Alert, buttonClass, ErrorState, LoadingState } from "@/shared/ui";
import { ContractFileViewer } from "@/widgets/contract-file-viewer";
import { CONTRACT_REVIEW_LABELS } from "../model/labels";
import { ClausesSection } from "./clauses-section";
import { KeyTermsSection } from "./key-terms-section";
import { ReviewConfirm } from "./review-confirm";
import { ReviewItemsSection } from "./review-items-section";
import { ReviewSummary } from "./review-summary";

export function ContractReviewPage({ contractId }: { contractId: number }) {
  const labels = CONTRACT_REVIEW_LABELS;
  const router = useRouter();
  const reviewQuery = useContractReview(contractId);
  const confirmation = useConfirmContractReview(contractId);
  const [page, setPage] = useState(1);

  if (reviewQuery.isPending) {
    return <LoadingState label={labels.loading} />;
  }

  const translationId = reviewQuery.data?.translationId;
  if (reviewQuery.isError || typeof translationId !== "number") {
    return (
      <ErrorState
        title={labels.loadError}
        message={
          getApiErrorFeedback(getApiErrorCode(reviewQuery.error), "ko").message
        }
        onRetry={() => void reviewQuery.refetch()}
      />
    );
  }

  const review = reviewQuery.data;
  const items = review.items ?? [];
  const warnings = review.warnings ?? [];
  const employerKeys = new Set(
    items
      .filter((item) => item.valueSource && item.valueSource !== "AI")
      .map((item) => item.key ?? ""),
  );
  const warningKeys = new Set(warnings.map((warning) => warning.key ?? ""));
  const minWage = warnings.find((warning) => warning.code === "MIN_WAGE");
  const responseErrorCode = getApiErrorCode(confirmation.error);
  const canReload =
    responseErrorCode === "TRANSLATION_OUTDATED" ||
    responseErrorCode === "CONTRACT_STAGE_CONFLICT";

  return (
    <div className="mx-auto max-w-[1200px] px-6 pt-6 pb-20">
      <nav aria-label={labels.breadcrumb} className="mb-8 text-sm text-muted">
        <Link href={routes.home} className="text-muted underline">
          {labels.home}
        </Link>
        <span aria-hidden="true" className="mx-2">
          &gt;
        </span>
        <Link href={routes.contracts} className="text-muted underline">
          {labels.contracts}
        </Link>
        <span aria-hidden="true" className="mx-2">
          &gt;
        </span>
        <span aria-current="page">{labels.title}</span>
      </nav>

      <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">
        {labels.title}
      </h1>
      <p className="mb-2 max-w-[820px] text-lg text-muted">
        {labels.description}
      </p>

      <ReviewSummary review={review} labels={labels} />

      <div className="flex flex-wrap items-start gap-10">
        {review.file?.fileId !== undefined && (
          <ContractFileViewer
            id="contract-review-original"
            contractId={contractId}
            fileId={review.file.fileId}
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
          {review.keyTerms && (
            <KeyTermsSection
              keyTerms={review.keyTerms}
              employerKeys={employerKeys}
              warningKeys={warningKeys}
              labels={labels}
            />
          )}

          {minWage && (
            <Alert tone="warning" accent="top" title={labels.minWageTitle}>
              <p lang={review.language}>{minWage.message}</p>
              <p className="mt-1">{labels.minWageHelp}</p>
            </Alert>
          )}

          <ReviewItemsSection
            items={items}
            warnings={warnings}
            language={review.language}
            onShowSource={setPage}
            labels={labels}
          />

          {(review.clauses?.length ?? 0) > 0 && (
            <ClausesSection
              clauses={review.clauses ?? []}
              language={review.language}
              onShowSource={setPage}
              labels={labels}
            />
          )}

          {review.canRespond ? (
            <ReviewConfirm
              key={translationId}
              onConfirm={() =>
                confirmation.mutate(translationId, {
                  onSuccess: () =>
                    router.push(routes.contractReviewDone(contractId)),
                })
              }
              onReport={() => {}}
              isSubmitting={confirmation.isPending}
              error={
                confirmation.isError
                  ? {
                      message: getApiErrorFeedback(responseErrorCode, "ko")
                        .message,
                      onReload: canReload
                        ? () => {
                            confirmation.reset();
                            void reviewQuery.refetch();
                          }
                        : undefined,
                    }
                  : undefined
              }
              labels={labels}
            />
          ) : (
            <Alert
              tone="info"
              title={labels.cannotRespondTitle}
              action={
                <Link
                  href={routes.contract(contractId)}
                  className={buttonClass({ variant: "secondary", size: "md" })}
                >
                  {labels.contractDetail}
                </Link>
              }
            >
              {labels.cannotRespondDescription}
            </Alert>
          )}
        </div>
      </div>
    </div>
  );
}
