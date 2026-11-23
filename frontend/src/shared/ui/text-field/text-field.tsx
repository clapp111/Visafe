import type { InputHTMLAttributes, ReactNode } from "react";
import { cn } from "@/shared/lib";
import {
  Field,
  fieldControlClass,
  fieldDescribedBy,
  type FieldStatus,
} from "../field";

export interface TextFieldProps extends Omit<
  InputHTMLAttributes<HTMLInputElement>,
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
