"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useContractList, useStartContractRevision } from "@/entities/contract";
import { useSession } from "@/entities/session";
import { getApiErrorCode, getApiErrorFeedback } from "@/shared/api";
import { routes } from "@/shared/config";
import { useCurrentLanguage } from "@/shared/i18n";
import {
  Alert,
  Button,
  EmptyState,
  ErrorState,
  LoadingState,
  Pagination,
  buttonClass,
} from "@/shared/ui";
import { CONTRACT_LIST_LABELS } from "../model/labels";
import { todoTarget } from "../model/todo-target";
import { ContractTable } from "./contract-table";
import { StatusFilter, type StatusFilterValue } from "./status-filter";

const STATUSES = ["IN_PROGRESS", "CONFIRMED", "TERMINATED"] as const;

export function ContractListPage() {
  const language = useCurrentLanguage();
  const labels = CONTRACT_LIST_LABELS[language];
  const router = useRouter();
  const pathname = usePathname() ?? routes.contracts;
  const searchParams = useSearchParams();
  const { session } = useSession();
  // AuthGuard가 ADMIN을 막으므로 여기에는 고용주·근로자만 온다
  const role = session.member?.role === "EMPLOYER" ? "EMPLOYER" : "WORKER";
  const statusParam = searchParams?.get("status");
  const status: StatusFilterValue = STATUSES.find(
    (value) => value === statusParam,
  );
  const [page, setPage] = useState(0);
  const listQuery = useContractList(status, page);
  const revision = useStartContractRevision();
  const revisingId = revision.isPending ? revision.variables : undefined;

  if (listQuery.isPending) {
    return <LoadingState label={labels.loading} />;
  }
  if (listQuery.isError) {
    return (
      <ErrorState
        title={labels.loadError}
        message={
          getApiErrorFeedback(getApiErrorCode(listQuery.error), language)
            .message
        }
        onRetry={() => void listQuery.refetch()}
      />
    );
  }

  const list = listQuery.data;
  const items = list.content ?? [];
  const firstTodo = items.find((item) => item.todo);
  const firstTodoTarget = firstTodo && todoTarget(firstTodo);
  const firstTodoId = firstTodo?.contractId;
  const totalPages = list.page?.totalPages ?? 1;

  function changeStatus(next: StatusFilterValue) {
    setPage(0);
    router.replace(next ? `${pathname}?status=${next}` : pathname);
  }

  function startRevision(contractId: number) {
    if (revision.isPending) return;
    revision.mutate(contractId, {
      onSuccess: () => router.push(routes.contractDraft(contractId)),
    });
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
        <span aria-current="page">{labels.title}</span>
      </nav>

      <div className="mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">
            {labels.title}
          </h1>
          <p className="text-lg text-muted">{labels.description[role]}</p>
        </div>
        {role === "EMPLOYER" && (
          <Link href={routes.contractNew} className={buttonClass()}>
            {labels.newContract}
          </Link>
        )}
      </div>

      {(list.todoCount ?? 0) > 0 && (
        <Alert
          tone="warning"
          accent="top"
          title={labels.todoBanner(list.todoCount ?? 0)}
          className="mb-8"
          action={
            firstTodo &&
            firstTodoTarget &&
            (firstTodoTarget.kind === "link" ? (
              <Link
                href={firstTodoTarget.href}
                className="font-bold text-primary underline"
              >
                {labels.todoBannerAction}
              </Link>
            ) : firstTodoId !== undefined ? (
              <Button
                variant="secondary"
                size="md"
                disabled={revisingId === firstTodoId}
                onClick={() => startRevision(firstTodoId)}
              >
                {labels.todoBannerAction}
              </Button>
            ) : null)
          }
        />
      )}

      <StatusFilter
        counts={list.counts}
        value={status}
        onChange={changeStatus}
        labels={labels}
      />

      {revision.isError && (
        <Alert tone="danger" title={labels.revisionError} className="mb-4">
          {
            getApiErrorFeedback(getApiErrorCode(revision.error), language)
              .message
          }
        </Alert>
      )}

      {listQuery.isPlaceholderData ? (
        <LoadingState label={labels.loading} />
      ) : items.length > 0 ? (
        <>
          <ContractTable
            items={items}
            role={role}
            onRevise={startRevision}
            revisingId={revisingId}
            labels={labels}
          />
          {totalPages > 1 && (
            <Pagination
              page={list.page?.number ?? page}
              totalPages={totalPages}
              disabled={listQuery.isPlaceholderData}
              onChange={setPage}
              labels={labels}
            />
          )}
        </>
      ) : (list.counts?.all ?? 0) === 0 ? (
        <EmptyState
          title={labels.empty[role]}
          description={labels.emptyHint[role]}
          action={
            role === "EMPLOYER" && (
              <Link href={routes.contractNew} className={buttonClass()}>
                {labels.newContract}
              </Link>
            )
          }
        />
      ) : (
        <EmptyState title={labels.emptyFiltered} />
      )}

      {role === "WORKER" && (
        <p className="mt-4 text-sm text-muted">{labels.workerHint}</p>
      )}
    </div>
  );
}
