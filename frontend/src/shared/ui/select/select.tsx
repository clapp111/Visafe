import type { ReactNode, SelectHTMLAttributes } from "react";
import { cn } from "@/shared/lib";
import {
  Field,
  fieldControlClass,
  fieldDescribedBy,
  type FieldStatus,
} from "../field";

export interface SelectProps extends Omit<
  SelectHTMLAttributes<HTMLSelectElement>,
  "id"
> {
  id: string;
  label: ReactNode;
  hint?: ReactNode;
  message?: ReactNode;
  status?: FieldStatus;
  /** `<option>` 목록 */
  children: ReactNode;
}

export function Select({
  id,
  label,
  hint,
  message,
  status,
  required,
  className,
  children,
  ...props
}: SelectProps) {
  return (
    <Field
      id={id}
      label={label}
      hint={hint}
      message={message}
      status={status}
      required={required}
    >
      <select
        id={id}
        required={required}
        aria-invalid={status === "error" || undefined}
        aria-describedby={fieldDescribedBy(id, { hint, message })}
        className={cn(fieldControlClass(status), "h-12", className)}
        {...props}
      >
        {children}
      </select>
    </Field>
  );
}
