"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useCreateContract, useWorkerSearch } from "@/entities/contract";
import { getApiErrorCode, getApiErrorFeedback } from "@/shared/api";
import { routes } from "@/shared/config";
import { RegisterSteps } from "@/widgets/register-steps";
import { CONTRACT_NEW_LABELS } from "../model/labels";
import {
  RegisterForm,
  type RegisterIntent,
  type RegisterValues,
} from "./register-form";

export function ContractNewPage() {
  const labels = CONTRACT_NEW_LABELS;
  const router = useRouter();
  const [workerName, setWorkerName] = useState("");
  const workerSearch = useWorkerSearch(workerName);
  const createContract = useCreateContract();
  const submitErrorCode = getApiErrorCode(createContract.error);
  const submitErrorMessage = createContract.isError
    ? getApiErrorFeedback(submitErrorCode, "ko").message
    : undefined;
  const fileErrorMessage =
    submitErrorCode === "FILE_TOO_LARGE" ||
    submitErrorCode === "FILE_UNSUPPORTED_TYPE"
      ? submitErrorMessage
      : undefined;
  const workerErrorMessage =
    submitErrorCode === "WORKER_NOT_FOUND" ? submitErrorMessage : undefined;
  const formErrorMessage =
    submitErrorMessage && !fileErrorMessage && !workerErrorMessage
      ? submitErrorMessage
      : workerSearch.isError
        ? getApiErrorFeedback(getApiErrorCode(workerSearch.error), "ko").message
        : undefined;

  function handleSearch(name: string) {
    if (name === workerName) {
      if (!workerSearch.isFetching) void workerSearch.refetch();
      return;
    }
    setWorkerName(name);
  }

  function handleSubmit(values: RegisterValues, intent: RegisterIntent) {
    if (createContract.isPending || createContract.isSuccess) return;

    createContract.mutate(values, {
      onSuccess: ({ contractId }) => {
        router.push(
          intent === "next"
            ? routes.contractDraft(contractId)
            : routes.contracts,
        );
      },
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

      <h1 className="mb-3 text-5xl font-bold tracking-[-0.5px]">
        {labels.title}
      </h1>
      <p className="mb-8 max-w-[820px] text-lg text-muted">
        {labels.description[0]}
        <br />
        {labels.description[1]}
      </p>

      <RegisterSteps current={1} />

      <RegisterForm
        workers={workerSearch.data}
        isSearchingWorkers={workerSearch.isFetching}
        onSearchWorkers={handleSearch}
        onSubmit={handleSubmit}
        isSubmitting={createContract.isPending}
        errorMessage={formErrorMessage}
        fileErrorMessage={fileErrorMessage}
        workerErrorMessage={workerErrorMessage}
        labels={labels}
      />
    </div>
  );
}
