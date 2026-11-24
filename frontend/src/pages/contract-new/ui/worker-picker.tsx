"use client";

import { type KeyboardEvent, useState } from "react";
import type { components } from "@/shared/api";
import { cn } from "@/shared/lib";
import {
  Button,
  Field,
  FieldMessage,
  fieldControlClass,
  fieldDescribedBy,
} from "@/shared/ui";
import type { ContractNewLabels } from "../model/labels";

type WorkerSearchResponse = components["schemas"]["WorkerSearchResponse"];

const INPUT_ID = "contract-new-worker";
const REGION_NAMES = new Intl.DisplayNames(["ko"], { type: "region" });

/** 국적 코드(VN)를 이름(베트남)으로 바꾼다. 모르는 코드는 그대로 둔다. */
function nationalityName(code?: string) {
  if (!code) return undefined;
  try {
    return REGION_NAMES.of(code) ?? code;
  } catch {
    return code;
  }
}

export interface WorkerPickerProps {
  /** 검색 결과 (A6). 아직 검색하지 않았으면 undefined */
  workers?: WorkerSearchResponse[];
  isSearching?: boolean;
  /** 2글자 이상인 검색어로 불린다 */
  onSearch: (name: string) => void;
  selectedId?: number;
  onSelect: (memberId: number) => void;
  /** 근로자를 고르지 않았거나 404 WORKER_NOT_FOUND일 때의 안내 */
  errorMessage?: string;
  labels: ContractNewLabels;
}

/** 근로자를 이름으로 찾아 고른다. 동명이인은 국적·생년월일·외국인등록번호 끝 4자리로 구분한다. */
export function WorkerPicker({
  workers,
  isSearching,
  onSearch,
  selectedId,
  onSelect,
  errorMessage,
  labels,
}: WorkerPickerProps) {
  const [name, setName] = useState("");
  const [tooShort, setTooShort] = useState(false);
  const searchMessage = tooShort ? labels.searchTooShort : undefined;

  function search() {
    const query = name.trim();
    if (query.length < 2) {
      setTooShort(true);
      return;
    }
    setTooShort(false);
    onSearch(query);
  }

  // 등록 폼 안에 있어 Enter가 폼 제출이 되지 않도록 검색으로 바꾼다
  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      search();
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <Field
        id={INPUT_ID}
        label={labels.workerLabel}
        hint={labels.workerHint}
        message={searchMessage}
        status={searchMessage ? "error" : undefined}
        required
        requiredLabel={labels.required}
      >
        <div className="flex flex-wrap gap-3">
          <input
            id={INPUT_ID}
            type="search"
            value={name}
            onChange={(event) => setName(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={labels.workerPlaceholder}
            autoComplete="off"
            aria-invalid={searchMessage ? true : undefined}
            aria-describedby={fieldDescribedBy(INPUT_ID, {
              hint: labels.workerHint,
              message: searchMessage,
            })}
            className={cn(
              fieldControlClass(searchMessage ? "error" : undefined),
              "h-12 min-w-0 flex-[1_1_320px]",
            )}
          />
          <Button
            variant="secondary"
            size="md"
            disabled={isSearching}
            onClick={search}
          >
            {isSearching ? labels.searching : labels.search}
          </Button>
        </div>
      </Field>

      {workers && (
        <fieldset
          aria-describedby={
            workers.length > 0 ? "contract-new-worker-results-hint" : undefined
          }
          className="flex flex-col gap-3"
        >
          <legend className="mb-1 font-bold">{labels.workerResults}</legend>
          {workers.length === 0 ? (
            <p className="text-muted">{labels.noWorkers}</p>
          ) : (
            <>
              <p
                id="contract-new-worker-results-hint"
                className="text-sm text-muted"
              >
                {labels.workerResultsHint}
              </p>
              <ul className="flex flex-col gap-2">
                {workers.map((worker) => {
                  const checked = worker.memberId === selectedId;
                  return (
                    <li key={worker.memberId}>
                      <label
                        className={cn(
                          "flex min-h-11 cursor-pointer items-start gap-3 rounded-md border px-4 py-3",
                          checked
                            ? "border-2 border-primary bg-primary-subtle"
                            : "border-border",
                        )}
                      >
                        <input
                          type="radio"
                          name="workerMemberId"
                          checked={checked}
                          onChange={() =>
                            worker.memberId !== undefined &&
                            onSelect(worker.memberId)
                          }
                          className="mt-[3px] size-5 shrink-0 accent-primary"
                        />
                        <span className="flex min-w-0 flex-col gap-0.5">
                          <strong>{worker.name}</strong>
                          <span className="flex flex-wrap gap-x-4 text-sm text-muted">
                            <span>
                              {labels.nationality}{" "}
                              {nationalityName(worker.nationality)}
                            </span>
                            <span>
                              {labels.birthDate}{" "}
                              {worker.birthDate &&
                                `${worker.birthDate.replaceAll("-", ".")}.`}
                            </span>
                            <span>
                              {labels.alienRegNoLast4} {worker.alienRegNoLast4}
                            </span>
                          </span>
                        </span>
                      </label>
                    </li>
                  );
                })}
              </ul>
            </>
          )}
        </fieldset>
      )}

      {errorMessage && <FieldMessage>{errorMessage}</FieldMessage>}
    </div>
  );
}
