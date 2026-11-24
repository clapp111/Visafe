"use client";

import { type FormEvent, type ReactNode, useState } from "react";
import { cn } from "@/shared/lib";
import {
  Button,
  Field,
  fieldControlClass,
  fieldDescribedBy,
} from "@/shared/ui";
import { formatConfirmationNo } from "../lib/format-confirmation-no";

export interface LookupFormLabels {
  label: string;
  placeholder?: string;
  submit: string;
  required: string;
}

export interface LookupFormProps {
  id: string;
  /** 처음 채워 둘 확인번호 (결과 화면에서는 조회한 번호) */
  defaultValue?: string;
  hint?: ReactNode;
  /** 조회 실패 안내 (404 LOOKUP_NOT_FOUND·400 LOOKUP_INVALID_FORMAT). 입력란 아래에 보인다 */
  errorMessage?: string;
  /** 하이픈을 맞춘 확인번호로 불린다 */
  onSubmit: (confirmationNo: string) => void;
  labels: LookupFormLabels;
  className?: string;
  /** 입력란 아래, 폼 안에 두는 안내 */
  children?: ReactNode;
}

/** 확인번호 입력란과 조회 버튼. 하이픈은 입력하는 동안 자동으로 들어간다. */
export function LookupForm({
  id,
  defaultValue = "",
  hint,
  errorMessage,
  onSubmit,
  labels,
  className,
  children,
}: LookupFormProps) {
  const [value, setValue] = useState(() => formatConfirmationNo(defaultValue));
  const [isEmpty, setIsEmpty] = useState(false);
  const message = isEmpty ? labels.required : errorMessage;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!value) {
      setIsEmpty(true);
      return;
    }
    setIsEmpty(false);
    onSubmit(value);
  }

  return (
    <form
      role="search"
      noValidate
      onSubmit={handleSubmit}
      className={className}
    >
      <Field
        id={id}
        label={labels.label}
        hint={hint}
        message={message}
        status={message ? "error" : undefined}
      >
        <div className="flex flex-wrap gap-3">
          <input
            id={id}
            type="text"
            value={value}
            onChange={(event) =>
              setValue(formatConfirmationNo(event.target.value))
            }
            placeholder={labels.placeholder}
            autoComplete="off"
            autoCapitalize="characters"
            spellCheck={false}
            aria-invalid={message ? true : undefined}
            aria-describedby={fieldDescribedBy(id, { hint, message })}
            className={cn(
              fieldControlClass(message ? "error" : undefined),
              "h-14 min-w-0 flex-[1_1_360px] px-4 text-lg",
            )}
          />
          <Button type="submit" size="lg" className="px-8 text-lg">
            {labels.submit}
          </Button>
        </div>
      </Field>
      {children}
    </form>
  );
}
