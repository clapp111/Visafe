"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { type FormEvent, useState } from "react";
import { routes } from "@/shared/config";
import { Badge, Button, EmptyState, Pagination, TextField } from "@/shared/ui";
import { getAdminListFixture } from "../model/fixtures";
import {
  ADMIN_CONTRACTS_LABELS,
  type AdminContractsLabels,
} from "../model/labels";
import { parseAdminView } from "../model/view";
import { AdminContractTable } from "./admin-contract-table";

export function AdminContractsPage() {
  const labels = ADMIN_CONTRACTS_LABELS;
  const router = useRouter();
  const pathname = usePathname() ?? routes.adminContracts;
  const searchParams = useSearchParams();
  const view = parseAdminView(searchParams?.get("view"));
  const q = searchParams?.get("q") ?? "";
  const [page, setPage] = useState(0);
  // 화면 확인용: ?empty=1 빈 목록 (TERMINATED 보기도 비어 있다)
  const list = getAdminListFixture(
    view,
    q,
    searchParams?.has("empty") ?? false,
  );
  const items = list.content ?? [];
  const totalPages = list.page?.totalPages ?? 1;
  const title = labels.titles[view];

  function search(keyword: string) {
    const params = new URLSearchParams();
    if (view !== "PENDING") params.set("view", view);
    if (keyword) params.set("q", keyword);
    setPage(0);
    const query = params.toString();
    router.replace(query ? `${pathname}?${query}` : pathname);
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
        <span aria-current="page">{title}</span>
      </nav>

      <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">{title}</h1>
      <p className="mb-8 text-lg text-muted">{labels.descriptions[view]}</p>

      {/* 보기나 검색어가 주소에서 바뀌면 입력란도 그 값으로 다시 그린다 */}
      <SearchForm
        key={`${view}:${q}`}
        initial={q}
        onSearch={search}
        labels={labels}
      />

      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <p>
          {labels.total(list.page?.totalElements ?? items.length)}
          {view === "PENDING" && ` · ${labels.sortOrder}`}
        </p>
        <p className="flex flex-wrap gap-4 text-sm text-muted">
          <span>
            <Badge tone="warning">{labels.legendWarning}</Badge>{" "}
            {labels.legendWarningHint}
          </span>
          <span>
            <Badge tone="highlight">{labels.legendEmployer}</Badge>{" "}
            {labels.legendEmployerHint}
          </span>
        </p>
      </div>

      {items.length > 0 ? (
        <>
          <AdminContractTable
            items={items}
            view={view}
            title={title}
            labels={labels}
          />
          {totalPages > 1 && (
            <Pagination
              page={list.page?.number ?? page}
              totalPages={totalPages}
              disabled={false}
              onChange={setPage}
              labels={labels}
            />
          )}
        </>
      ) : q ? (
        <EmptyState
          title={labels.emptySearch}
          description={labels.emptySearchHint}
        />
      ) : (
        <EmptyState title={labels.empty[view]} />
      )}

      <div className="mt-8 flex flex-col gap-1.5 rounded-md bg-surface-muted px-6 py-5 text-sm text-muted">
        <strong className="text-base text-foreground">
          {labels.infoTitle}
        </strong>
        {labels.info.map((line) => (
          <span key={line}>{line}</span>
        ))}
      </div>
    </div>
  );
}

function SearchForm({
  initial,
  onSearch,
  labels,
}: {
  initial: string;
  onSearch: (keyword: string) => void;
  labels: AdminContractsLabels;
}) {
  const [keyword, setKeyword] = useState(initial);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSearch(keyword.trim());
  }

  return (
    <form
      role="search"
      onSubmit={handleSubmit}
      className="mb-6 flex flex-wrap items-end gap-3"
    >
      <div className="min-w-0 flex-[1_1_360px]">
        <TextField
          id="admin-contracts-q"
          type="search"
          label={labels.searchLabel}
          placeholder={labels.searchPlaceholder}
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
        />
      </div>
      <Button type="submit" size="md">
        {labels.search}
      </Button>
    </form>
  );
}
