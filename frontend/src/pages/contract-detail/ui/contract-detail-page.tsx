"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState } from "react";
import { useSession } from "@/entities/session";
import { routes } from "@/shared/config";
import { ContractFileViewer } from "@/widgets/contract-file-viewer";
import { ChainRecordsTable, HistoryList } from "@/widgets/contract-records";
import { getDetailFixture } from "../model/fixtures";
import { CONTRACT_DETAIL_LABELS } from "../model/labels";
import { StatusCard } from "./status-card";
import { TermsSection } from "./terms-section";

export function ContractDetailPage({ contractId }: { contractId: number }) {
  const labels = CONTRACT_DETAIL_LABELS;
  const searchParams = useSearchParams();
  const { session } = useSession();
  // AuthGuard가 ADMIN을 막으므로 여기에는 고용주·근로자만 온다
  const role = session.member?.role === "EMPLOYER" ? "EMPLOYER" : "WORKER";
  // 화면 확인용: ?state=reviewing | awaiting | returned | recording | ended (기본 done)
  const detail = getDetailFixture(contractId, searchParams?.get("state"), role);
  const [page, setPage] = useState(1);
  const counterparty =
    role === "EMPLOYER" ? detail.worker?.name : detail.employer?.companyName;

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

      <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">
        {detail.contractNo}
        {counterparty && (
          <span lang={role === "EMPLOYER" ? "vi" : undefined}>
            {" "}
            {counterparty}
          </span>
        )}
      </h1>
      {detail.previousContractId != null && (
        <p className="mb-6 text-muted">
          {labels.previousContract}{" "}
          <Link
            href={routes.contract(detail.previousContractId)}
            className="text-primary underline"
          >
            {labels.previousContractLink}
          </Link>
        </p>
      )}

      <StatusCard
        detail={detail}
        contractId={contractId}
        role={role}
        onRevise={() => {}}
        onDownload={() => {}}
        labels={labels}
      />

      <div className="flex flex-wrap items-start gap-10">
        {detail.file?.fileId !== undefined && (
          <ContractFileViewer
            id="contract-detail-original"
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
          <TermsSection terms={detail.terms ?? []} labels={labels} />

          {(detail.timeline?.length ?? 0) > 0 && (
            <section
              aria-labelledby="contract-detail-history"
              className="flex flex-col gap-4 border-t-2 border-foreground pt-6"
            >
              <h2
                id="contract-detail-history"
                className="text-2xl leading-normal font-bold"
              >
                {labels.historyTitle}
              </h2>
              <HistoryList history={detail.timeline ?? []} />
            </section>
          )}

          {(detail.chainRecords?.length ?? 0) > 0 && (
            <section
              aria-labelledby="contract-detail-chain"
              className="flex flex-col gap-4 border-t-2 border-foreground pt-6"
            >
              <h2
                id="contract-detail-chain"
                className="text-2xl leading-normal font-bold"
              >
                {labels.chainTitle}
              </h2>
              <ChainRecordsTable records={detail.chainRecords ?? []} />
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
