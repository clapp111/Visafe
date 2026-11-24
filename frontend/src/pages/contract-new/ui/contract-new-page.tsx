"use client";

import Link from "next/link";
import { useState } from "react";
import type { components } from "@/shared/api";
import { routes } from "@/shared/config";
import { searchWorkerFixtures } from "../model/fixtures";
import { CONTRACT_NEW_LABELS } from "../model/labels";
import { RegisterForm } from "./register-form";
import { RegisterSteps } from "./register-steps";

export function ContractNewPage() {
  const labels = CONTRACT_NEW_LABELS;
  // A6 근로자 검색·R1 계약 생성을 연결하기 전까지 예시값으로 화면을 보인다
  const [workers, setWorkers] =
    useState<components["schemas"]["WorkerSearchResponse"][]>();

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

      <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">
        {labels.title}
      </h1>
      <p className="mb-8 max-w-[820px] text-lg text-muted">
        {labels.description[0]}
        <br />
        {labels.description[1]}
      </p>

      <RegisterSteps current={1} labels={labels.steps} />

      <RegisterForm
        workers={workers}
        onSearchWorkers={(name) => setWorkers(searchWorkerFixtures(name))}
        onSubmit={() => {}}
        labels={labels}
      />
    </div>
  );
}
