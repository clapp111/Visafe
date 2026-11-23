import type { ReactNode } from "react";
import { cn } from "@/shared/lib";
import { ErrorIcon, WarningIcon } from "../icons";

/** error: 잘못된 값(빨강), warning: 확인이 필요한 값(주황) */
export type FieldStatus = "error" | "warning";

export interface FieldProps {
  id: string;
  label: ReactNode;
  hint?: ReactNode;
  message?: ReactNode;
  status?: FieldStatus;
  required?: boolean;
  requiredLabel?: string;
  children: ReactNode;
}

/** 입력란의 hint·message id를 aria-describedby 값으로 만든다. */
export function fieldDescribedBy(
  id: string,
  { hint, message }: { hint?: ReactNode; message?: ReactNode },
) {
  const ids = [hint && `${id}-hint`, message && `${id}-message`].filter(
    Boolean,
  );
  return ids.length > 0 ? ids.join(" ") : undefined;
}

/** 입력란 테두리. 상태가 있으면 2px로 강조한다. */
export function fieldControlClass(status?: FieldStatus) {
  return cn(
    "w-full rounded-md border bg-surface px-3.5 text-base text-foreground disabled:bg-surface-muted",
    status === "error" && "border-2 border-danger",
    status === "warning" && "border-2 border-warning-accent",
    !status && "border-border-strong",
  );
}

/** 라벨·도움말·메시지로 입력란을 감싼다. 입력란에는 같은 id를 준다. */
export function Field({
  id,
  label,
  hint,
  message,
  status,
  required,
  requiredLabel = "필수",
  children,
}: FieldProps) {
  return (
    <div className="flex min-w-0 flex-col gap-2">
      <label htmlFor={id} className="font-bold">
        {label}
        {required && (
          <>
            <span aria-hidden="true" className="ml-0.5 text-danger">
              *
            </span>
            <span className="sr-only">{requiredLabel}</span>
          </>
        )}
      </label>
      {hint && (
        <p id={`${id}-hint`} className="text-sm text-muted">
          {hint}
        </p>
      )}
      {children}
      {message && (
        <FieldMessage id={`${id}-message`} status={status}>
          {message}
        </FieldMessage>
      )}
    </div>
  );
}

export function FieldMessage({
  id,
  status = "error",
  children,
}: {
  id?: string;
  status?: FieldStatus;
  children: ReactNode;
}) {
  const Icon = status === "error" ? ErrorIcon : WarningIcon;
  return (
    <p
      id={id}
      role={status === "error" ? "alert" : "status"}
      className={cn(
        "flex items-center gap-1.5 text-sm font-bold",
        status === "error" ? "text-danger" : "text-warning",
      )}
    >
      <Icon size={18} className="shrink-0" />
      {children}
    </p>
  );
}
