"use client";

import Link from "next/link";
import { routes } from "@/shared/config";
import { cn } from "@/shared/lib";
import { Alert, Badge, Button, CheckIcon, buttonClass } from "@/shared/ui";
import { getSubmittedFixture } from "../model/fixtures";
import { CONTRACT_SUBMITTED_LABELS } from "../model/labels";
import { ProgressSteps } from "./progress-steps";

export function ContractSubmittedPage({ contractId }: { contractId: number }) {
  const labels = CONTRACT_SUBMITTED_LABELS;
  // M2 진행 상황 폴링·R7 번역 다시 시도를 연결하기 전까지 예시값으로 화면을 보인다
  const detail = getSubmittedFixture(contractId);
  const phase = detail.stage === "TRANSLATING" ? "translating" : "awaiting";
  const translationFailed =
    detail.stage === "TRANSLATING" && detail.processing?.status === "FAILED";
  const [before, after] = labels.description[phase];

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

      <div className="mx-auto flex max-w-[880px] flex-col gap-8">
        <section
          aria-labelledby="contract-submitted-title"
          className="flex flex-col items-center gap-4 rounded-md border border-t-4 border-border border-t-primary-hover px-6 py-12 text-center"
        >
          <span className="flex size-16 items-center justify-center rounded-full bg-primary-subtle text-primary-hover">
            <CheckIcon size={36} />
          </span>
          <h1 id="contract-submitted-title" className="text-4xl font-bold">
            {labels.title[phase]}
          </h1>
          <p className="max-w-[640px] text-lg text-muted">
            {before}
            <strong className="text-foreground">{detail.worker?.name}</strong>
            {after}
          </p>
          <dl className="mt-2 grid grid-cols-[auto_auto] gap-x-6 gap-y-2 text-left">
            <dt className="text-muted">{labels.contractNo}</dt>
            <dd className="font-bold">{detail.contractNo}</dd>
            <dt className="text-muted">{labels.currentStatus}</dt>
            <dd>
              <Badge tone="primary" size="md">
                {labels.inProgress}
              </Badge>
            </dd>
          </dl>
        </section>

        <section
          aria-labelledby="contract-submitted-progress"
          className="flex flex-col gap-4"
        >
          <h2 id="contract-submitted-progress" className="text-2xl font-bold">
            {labels.progressTitle}
          </h2>
          <ProgressSteps detail={detail} labels={labels} />
          {translationFailed && (
            <Alert
              tone="danger"
              accent="left"
              title={labels.translationFailedTitle}
              action={
                <Button size="md" onClick={() => {}}>
                  {labels.retryTranslation}
                </Button>
              }
            >
              {labels.translationFailedHint}
            </Alert>
          )}
          <p className="text-sm text-muted">
            {labels.progressHint[0]}
            <br />
            {labels.progressHint[1]}
          </p>
        </section>

        <div className="flex flex-wrap justify-center gap-4">
          <Link
            href={routes.contractNew}
            className={buttonClass({ variant: "secondary" })}
          >
            {labels.newContract}
          </Link>
          <Link
            href={routes.contract(contractId)}
            className={cn(buttonClass(), "px-8 text-lg")}
          >
            {labels.viewContract}
          </Link>
        </div>
      </div>
    </div>
  );
}
