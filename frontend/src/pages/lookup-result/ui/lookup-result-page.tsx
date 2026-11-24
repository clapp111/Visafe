"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useFileCheck, useLookup } from "@/entities/contract";
import { LookupForm } from "@/features/lookup-contract";
import { getApiErrorCode, getApiErrorFeedback } from "@/shared/api";
import { routes } from "@/shared/config";
import { formatDate } from "@/shared/lib";
import { ErrorState, LoadingState } from "@/shared/ui";
import { hashFile } from "../lib/hash-file";
import { LOOKUP_RESULT_LABELS } from "../model/labels";
import { ChainRecordsSection } from "./chain-records-section";
import { FileCheckSection } from "./file-check-section";
import { HistorySection } from "./history-section";
import { KeyTermsSection } from "./key-terms-section";
import { NotFoundGuide } from "./not-found-guide";
import { ResultSection } from "./result-section";
import { StatusCard } from "./status-card";

function ContractFileCheck({ confirmationNo }: { confirmationNo: string }) {
  const labels = LOOKUP_RESULT_LABELS;
  const mutation = useFileCheck(confirmationNo);
  const [hashStatus, setHashStatus] = useState<"idle" | "hashing" | "error">(
    "idle",
  );
  const hashController = useRef<AbortController | null>(null);

  useEffect(() => () => hashController.current?.abort(), []);

  async function handleCheck(file: File) {
    hashController.current?.abort();
    const controller = new AbortController();
    hashController.current = controller;
    mutation.reset();
    setHashStatus("hashing");

    try {
      const fileHash = await hashFile(file);
      if (!controller.signal.aborted) {
        mutation.mutate({ fileHash });
        setHashStatus("idle");
      }
    } catch {
      if (!controller.signal.aborted) {
        setHashStatus("error");
      }
    }
  }

  const errorMessage =
    hashStatus === "error"
      ? labels.fileReadError
      : mutation.isError
        ? getApiErrorFeedback(getApiErrorCode(mutation.error), "ko").message
        : undefined;

  return (
    <FileCheckSection
      result={mutation.data}
      isChecking={hashStatus === "hashing" || mutation.isPending}
      errorMessage={errorMessage}
      onCheck={handleCheck}
      labels={labels}
    />
  );
}

export function LookupResultPage({
  confirmationNo,
}: {
  confirmationNo: string;
}) {
  const labels = LOOKUP_RESULT_LABELS;
  const router = useRouter();
  const {
    data: result,
    error,
    isError,
    isFetching,
    refetch,
  } = useLookup(confirmationNo);
  const errorCode = isFetching ? undefined : getApiErrorCode(error);
  const errorMessage =
    errorCode === "LOOKUP_NOT_FOUND"
      ? labels.notFound
      : errorCode === "LOOKUP_INVALID_FORMAT"
        ? labels.invalidFormat
        : undefined;
  const termination = result?.termination;

  function handleSubmit(next: string) {
    if (next === confirmationNo) {
      void refetch();
      return;
    }
    router.push(routes.lookup(next));
  }

  return (
    <div className="mx-auto max-w-[1200px] px-6 pt-6 pb-20">
      <nav aria-label={labels.breadcrumb} className="mb-8 text-sm text-muted">
        <Link href={routes.home} className="text-muted underline">
          {labels.home}
        </Link>
        <span aria-hidden="true" className="mx-2">
          &gt;
        </span>
        <Link href={routes.home} className="text-muted underline">
          {labels.lookup}
        </Link>
        <span aria-hidden="true" className="mx-2">
          &gt;
        </span>
        <span aria-current="page">{labels.title}</span>
      </nav>

      <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">
        {labels.title}
      </h1>
      <LookupForm
        key={confirmationNo}
        id="lookup-confirmation-no"
        defaultValue={confirmationNo}
        errorMessage={errorMessage}
        onSubmit={handleSubmit}
        labels={labels.lookupForm}
        className="mb-10 rounded-lg bg-surface-muted p-6"
      />

      {isFetching ? (
        <LoadingState />
      ) : errorMessage ? (
        <NotFoundGuide labels={labels} />
      ) : isError ? (
        <ErrorState
          title={getApiErrorFeedback(undefined, "ko").message}
          onRetry={refetch}
        />
      ) : result ? (
        <div className="flex flex-col gap-12">
          <StatusCard result={result} labels={labels} />

          {termination?.reasonType && (
            <div className="flex flex-col gap-1.5 rounded-md bg-surface-muted px-6 py-5">
              <strong>
                {labels.terminationReason}:{" "}
                {labels.terminationReasons[termination.reasonType]}
              </strong>
              {termination.endDate && (
                <span className="text-sm text-muted">
                  {labels.terminationEndDate(formatDate(termination.endDate))}
                </span>
              )}
              <span className="text-sm text-muted">
                {labels.terminationNewNumber}
              </span>
            </div>
          )}

          {/* 와이어프레임에서 종료된 계약은 핵심 근로조건을 보이지 않는다 */}
          {result.status === "CONFIRMED" &&
            result.keyTerms &&
            result.startDate && (
              <ResultSection id="lookup-key-terms" title={labels.keyTermsTitle}>
                <KeyTermsSection
                  keyTerms={result.keyTerms}
                  startDate={result.startDate}
                  endDate={result.endDate}
                  labels={labels}
                />
              </ResultSection>
            )}

          {/* 메모: 종료된 계약도 파일 대조 가능 */}
          <ResultSection id="lookup-file-check" title={labels.fileCheckTitle}>
            <ContractFileCheck
              key={confirmationNo}
              confirmationNo={confirmationNo}
            />
          </ResultSection>

          {result.chainRecords && result.chainRecords.length > 0 && (
            <ResultSection id="lookup-chain" title={labels.chainTitle}>
              <ChainRecordsSection
                records={result.chainRecords}
                labels={labels}
              />
            </ResultSection>
          )}

          {result.history && result.history.length > 0 && (
            <ResultSection id="lookup-history" title={labels.historyTitle}>
              <HistorySection history={result.history} labels={labels} />
            </ResultSection>
          )}
        </div>
      ) : null}
    </div>
  );
}
