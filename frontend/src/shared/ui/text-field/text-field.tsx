import type { ComponentProps, ReactNode } from "react";
import { cn } from "@/shared/lib";
import {
  Field,
  fieldControlClass,
  fieldDescribedBy,
  type FieldStatus,
} from "../field";

// ComponentProps<"input">에는 ref가 들어 있어 react-hook-form의 register를 그대로 펼칠 수 있다
export interface TextFieldProps extends Omit<
  ComponentProps<"input">,
  "id" | "size"
> {
  id: string;
  label: ReactNode;
  hint?: ReactNode;
  message?: ReactNode;
  status?: FieldStatus;
  /** md 48px(폼), lg 56px(확인번호 조회처럼 큰 입력) */
  size?: "md" | "lg";
}

export function TextField({
  id,
  label,
  hint,
  message,
  status,
  required,
  size = "md",
  className,
  type = "text",
  ...props
}: TextFieldProps) {
  return (
    <Field
      id={id}
      label={label}
      hint={hint}
      message={message}
      status={status}
      required={required}
    >
      <input
        id={id}
        type={type}
        required={required}
        aria-invalid={status === "error" || undefined}
        aria-describedby={fieldDescribedBy(id, { hint, message })}
        className={cn(
          fieldControlClass(status),
          size === "lg" ? "h-14 px-4 text-lg" : "h-12",
          className,
        )}
        {...props}
      />
    </Field>
  );
}
