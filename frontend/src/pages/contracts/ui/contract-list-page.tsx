"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useSession } from "@/entities/session";
import { routes } from "@/shared/config";
import { Alert, Button, EmptyState, buttonClass } from "@/shared/ui";
import { getListFixture } from "../model/fixtures";
import { CONTRACT_LIST_LABELS } from "../model/labels";
import { todoTarget } from "../model/todo-target";
import { ContractTable } from "./contract-table";
import { ListPagination } from "./list-pagination";
import { StatusFilter, type StatusFilterValue } from "./status-filter";

const STATUSES = ["IN_PROGRESS", "CONFIRMED", "TERMINATED"] as const;

export function ContractListPage() {
  const labels = CONTRACT_LIST_LABELS;
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
  // M1 내 계약 목록·R8 수정 시작을 연결하기 전까지 예시값으로 화면을 보인다 (?empty=1: 빈 목록)
  const list = getListFixture(
    role,
    status,
    searchParams?.has("empty") ?? false,
  );
  const items = list.content ?? [];
  const firstTodo = items.find((item) => item.todo);
  const firstTodoTarget = firstTodo && todoTarget(firstTodo);
  const totalPages = list.page?.totalPages ?? 1;

  function changeStatus(next: StatusFilterValue) {
    setPage(0);
    router.replace(next ? `${pathname}?status=${next}` : pathname);
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
            ) : (
              <Button variant="secondary" size="md" onClick={() => {}}>
                {labels.todoBannerAction}
              </Button>
            ))
          }
        />
      )}

      <StatusFilter
        counts={list.counts}
        value={status}
        onChange={changeStatus}
        labels={labels}
      />

      {items.length > 0 ? (
        <>
          <ContractTable
            items={items}
            role={role}
            onRevise={() => {}}
            labels={labels}
          />
          {totalPages > 1 && (
            <ListPagination
              page={page}
              totalPages={totalPages}
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
