import type { ReactNode, TextareaHTMLAttributes } from "react";
import { cn } from "@/shared/lib";
import {
  Field,
  fieldControlClass,
  fieldDescribedBy,
  type FieldStatus,
} from "../field";

export interface TextareaProps extends Omit<
  TextareaHTMLAttributes<HTMLTextAreaElement>,
  "id"
> {
  id: string;
  label: ReactNode;
  hint?: ReactNode;
  message?: ReactNode;
  status?: FieldStatus;
}

export function Textarea({
  id,
  label,
  hint,
  message,
  status,
  required,
  className,
  ...props
}: TextareaProps) {
  return (
    <Field
      id={id}
      label={label}
      hint={hint}
      message={message}
      status={status}
      required={required}
    >
      <textarea
        id={id}
        required={required}
        aria-invalid={status === "error" || undefined}
        aria-describedby={fieldDescribedBy(id, { hint, message })}
        className={cn(
          fieldControlClass(status),
          "min-h-36 resize-y py-3",
          className,
        )}
        {...props}
      />
    </Field>
  );
}
