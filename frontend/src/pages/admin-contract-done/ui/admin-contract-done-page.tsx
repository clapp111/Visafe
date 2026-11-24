"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import type { components } from "@/shared/api";
import { routes } from "@/shared/config";
import { formatDateTime } from "@/shared/lib";
import {
  CheckIcon,
  CopyButton,
  StepList,
  buttonClass,
  type StepListItem,
} from "@/shared/ui";
import { getAdminDoneFixture } from "../model/fixtures";
import {
  ADMIN_CONTRACT_DONE_LABELS,
  type AdminContractDoneLabels,
} from "../model/labels";

type Detail = components["schemas"]["ContractDetailResponse"];
type EventType = NonNullable<NonNullable<Detail["timeline"]>[number]["type"]>;

/** 기관 확인을 거친 계약의 다섯 단계. 확정 기록이 끝나기 전에는 마지막 단계가 진행 중이다 */
function buildSteps(
  detail: Detail,
  labels: AdminContractDoneLabels,
): StepListItem[] {
  const done = detail.stage === "DONE";
  const at = (type: EventType) => {
    const occurredAt = detail.timeline?.findLast(
      (entry) => entry.type === type,
    )?.occurredAt;
    return occurredAt && formatDateTime(occurredAt);
  };
  const finished = (id: string, title: string, type: EventType) => ({
    id,
    title,
    description: at(type),
    state: "done" as const,
    meta: labels.stepStatuses.done,
  });
  return [
    finished("submitted", labels.steps.submitted, "SUBMITTED"),
    finished("translation", labels.steps.translation, "TRANSLATION_COMPLETED"),
    finished("worker", labels.steps.worker, "WORKER_UNDERSTOOD"),
    finished("authority", labels.steps.authority, "AUTHORITY_APPROVED"),
    done
      ? finished("confirm", labels.steps.confirm, "CONFIRMED")
      : {
          id: "confirm",
          title: labels.steps.confirm,
          state: "current",
          meta: labels.stepStatuses.current,
        },
  ];
}

export function AdminContractDonePage({ contractId }: { contractId: number }) {
  const labels = ADMIN_CONTRACT_DONE_LABELS;
  const searchParams = useSearchParams();
  // 화면 확인용: ?state=recording 블록체인 기록 중 (기본 확정)
  const detail = getAdminDoneFixture(contractId, searchParams?.get("state"));
  const phase = detail.stage === "DONE" ? "done" : "recording";
  const record = detail.chainRecords?.find(
    (item) => item.recordType === "CONFIRM",
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
        <Link href={routes.adminContracts} className="text-muted underline">
          {labels.list}
        </Link>
        <span aria-hidden="true" className="mx-2">
          &gt;
        </span>
        <span aria-current="page">{labels.current}</span>
      </nav>

      <div className="mx-auto flex max-w-[880px] flex-col gap-10">
        <div className="flex flex-col items-center gap-4 text-center">
          <span
            aria-hidden="true"
            className="flex size-20 items-center justify-center rounded-full bg-primary-subtle text-primary-hover"
          >
            <CheckIcon size={44} />
          </span>
          <h1 className="text-5xl font-bold text-primary-hover">
            {labels.title[phase]}
          </h1>
          <p className="text-lg text-muted">{labels.description[phase]}</p>
        </div>

        {detail.confirmationNo && (
          <section
            aria-labelledby="admin-done-number"
            className="flex flex-col items-center gap-3 rounded-lg border-2 border-primary-hover p-8"
          >
            <h2
              id="admin-done-number"
              className="text-base font-bold text-muted"
            >
              {labels.confirmationNo}
            </h2>
            <strong className="font-mono text-5xl tracking-[2px]">
              {detail.confirmationNo}
            </strong>
            <div className="flex flex-wrap justify-center gap-3">
              <CopyButton
                text={detail.confirmationNo}
                label={labels.copy}
                copiedLabel={labels.copied}
              />
              <Link
                href={routes.lookup(detail.confirmationNo)}
                className={buttonClass({ variant: "secondary", size: "md" })}
              >
                {labels.lookup}
              </Link>
            </div>
          </section>
        )}

        {record && (
          <section
            aria-labelledby="admin-done-chain"
            className="flex flex-col gap-4 border-t-2 border-foreground pt-6"
          >
            <h2
              id="admin-done-chain"
              className="text-2xl leading-normal font-bold"
            >
              {labels.chainTitle}
            </h2>
            <dl className="grid grid-cols-[160px_1fr] gap-x-4 gap-y-3">
              <dt className="text-muted">{labels.chainStatus}</dt>
              <dd className="font-bold">
                {record.status && labels.chainStatuses[record.status]}
              </dd>
              {detail.confirmedAt && (
                <>
                  <dt className="text-muted">{labels.confirmedAt}</dt>
                  <dd>{formatDateTime(detail.confirmedAt)}</dd>
                </>
              )}
              {record.network && (
                <>
                  <dt className="text-muted">{labels.network}</dt>
                  <dd>
                    {labels.networkNames[record.network] ?? record.network}
                  </dd>
                </>
              )}
              {record.txHash && (
                <>
                  <dt className="text-muted">{labels.txHash}</dt>
                  <dd className="font-mono text-sm break-all">
                    {record.txHash}
                  </dd>
                </>
              )}
            </dl>
            {record.explorerUrl && (
              <a
                href={record.explorerUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="self-start font-bold text-primary underline"
              >
                {labels.explorer}
              </a>
            )}
            <p className="text-sm text-muted">{labels.chainHint}</p>
          </section>
        )}

        <section
          aria-labelledby="admin-done-progress"
          className="flex flex-col gap-4"
        >
          <h2 id="admin-done-progress" className="text-2xl font-bold">
            {labels.progressTitle}
          </h2>
          <StepList steps={buildSteps(detail, labels)} />
        </section>

        <div className="flex justify-center">
          <Link href={routes.adminContracts} className={buttonClass()}>
            {labels.toList}
          </Link>
        </div>
      </div>
    </div>
  );
}
