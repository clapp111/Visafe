"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { routes } from "@/shared/config";
import { cn, formatDateTime, shortenHash } from "@/shared/lib";
import {
  Badge,
  CheckIcon,
  LoadingState,
  StepList,
  buttonClass,
} from "@/shared/ui";
import { getReviewDoneFixture } from "../model/fixtures";
import { CONTRACT_REVIEW_DONE_LABELS } from "../model/labels";
import { buildReviewDoneSteps, getReviewDonePhase } from "../model/steps";
import { CopyButton } from "./copy-button";

export function ContractReviewDonePage({ contractId }: { contractId: number }) {
  const labels = CONTRACT_REVIEW_DONE_LABELS;
  const searchParams = useSearchParams();
  // 화면 확인용: ?state=failed | authority | done (기본 recording)
  const detail = getReviewDoneFixture(contractId, searchParams?.get("state"));
  const phase = getReviewDonePhase(detail.stage);

  // 확인 완료 이후 stage가 아니면 다른 화면으로 보낸다 (API 연동 단계)
  if (!phase) {
    return <LoadingState label={labels.loading} />;
  }

  const done = phase === "done";
  const confirmRecord = detail.chainRecords?.find(
    (record) => record.recordType === "CONFIRM",
  );

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
        <span aria-current="page">{labels.current}</span>
      </nav>

      <div className="mx-auto flex max-w-[880px] flex-col gap-8">
        <section
          aria-labelledby="contract-review-done-title"
          className={cn(
            "flex flex-col items-center gap-4 rounded-md border border-t-4 border-border px-6 py-12 text-center",
            done ? "border-t-success" : "border-t-primary-hover",
          )}
        >
          <span
            className={cn(
              "flex size-16 items-center justify-center rounded-full",
              done
                ? "bg-success-subtle text-success"
                : "bg-primary-subtle text-primary-hover",
            )}
          >
            <CheckIcon size={36} />
          </span>
          <h1 id="contract-review-done-title" className="text-4xl font-bold">
            {labels.title[phase]}
          </h1>
          <p className="max-w-[640px] text-lg text-muted">
            {labels.description[phase]}
          </p>

          {done && detail.confirmationNo && (
            <div className="flex flex-wrap items-center justify-center gap-4 rounded-md bg-surface-muted px-6 py-4">
              <span className="text-muted">{labels.confirmationNo}</span>
              <strong className="text-3xl tracking-wide">
                {detail.confirmationNo}
              </strong>
              <CopyButton
                text={detail.confirmationNo}
                label={labels.copy}
                copiedLabel={labels.copied}
              />
            </div>
          )}

          <dl className="mt-2 grid grid-cols-[auto_auto] gap-x-6 gap-y-2 text-left">
            {done && detail.confirmedAt && (
              <>
                <dt className="text-muted">{labels.confirmedAt}</dt>
                <dd className="font-bold">
                  {formatDateTime(detail.confirmedAt)}
                </dd>
              </>
            )}
            <dt className="text-muted">{labels.contractNo}</dt>
            <dd className="font-bold">{detail.contractNo}</dd>
            <dt className="text-muted">{labels.currentStatus}</dt>
            <dd>
              {detail.status && (
                <Badge tone={done ? "success" : "primary"} size="md">
                  {labels.statuses[detail.status]}
                </Badge>
              )}
            </dd>
            {done && confirmRecord?.txHash && (
              <>
                <dt className="text-muted">{labels.chainRecord}</dt>
                <dd className="flex flex-wrap items-center gap-x-3">
                  <span className="font-mono text-sm">
                    {shortenHash(confirmRecord.txHash)}
                  </span>
                  {confirmRecord.explorerUrl && (
                    <a
                      href={confirmRecord.explorerUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-sm text-primary underline"
                    >
                      {labels.explorer}
                    </a>
                  )}
                </dd>
              </>
            )}
          </dl>
        </section>

        <section
          aria-labelledby="contract-review-done-progress"
          className="flex flex-col gap-4"
        >
          <h2 id="contract-review-done-progress" className="text-2xl font-bold">
            {labels.progressTitle}
          </h2>
          <StepList steps={buildReviewDoneSteps(detail, phase, labels)} />
          {!done && (
            <p className="text-sm text-muted">{labels.progressHint[phase]}</p>
          )}
        </section>

        <div className="flex flex-wrap justify-center gap-4">
          {done && detail.confirmationNo ? (
            <Link
              href={routes.lookup(detail.confirmationNo)}
              className={buttonClass({ variant: "secondary" })}
            >
              {labels.lookup}
            </Link>
          ) : (
            <Link
              href={routes.contractReview(contractId)}
              className={buttonClass({ variant: "secondary" })}
            >
              {labels.reviewAgain}
            </Link>
          )}
          <Link href={routes.contracts} className={buttonClass()}>
            {labels.toContracts}
          </Link>
        </div>
      </div>
    </div>
  );
}
