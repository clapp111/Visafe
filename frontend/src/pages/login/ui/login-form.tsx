"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { cn } from "@/shared/lib";
import {
  Alert,
  Button,
  EyeIcon,
  Field,
  TextField,
  fieldControlClass,
  fieldDescribedBy,
} from "@/shared/ui";
import type { LoginLabels } from "../model/labels";
import { createLoginSchema, type LoginValues } from "../model/schema";

const EMAIL_ID = "login-email";
const PASSWORD_ID = "login-password";

export interface LoginFormProps {
  /** 입력 검증을 통과한 값으로 불린다 (A1 로그인 요청) */
  onSubmit: (values: LoginValues) => void;
  isSubmitting?: boolean;
  /** 서버가 돌려준 실패 안내 (예: 401 AUTH_LOGIN_FAILED의 message). 폼 위에 보인다 */
  errorMessage?: string;
  labels: LoginLabels;
}

export function LoginForm({
  onSubmit,
  isSubmitting,
  errorMessage,
  labels,
}: LoginFormProps) {
  const [showPassword, setShowPassword] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginValues>({
    resolver: zodResolver(createLoginSchema(labels)),
    defaultValues: { email: "", password: "" },
  });

  const passwordMessage = errors.password?.message;

  return (
    <>
      {errorMessage && (
        <Alert tone="danger" accent="left" title={errorMessage}>
          {labels.failedHint}
        </Alert>
      )}
      <form
        noValidate
        onSubmit={handleSubmit(onSubmit)}
        className="flex flex-col gap-6 rounded-lg border border-border bg-surface p-8"
      >
        <TextField
          id={EMAIL_ID}
          type="email"
          autoComplete="username"
          label={labels.email}
          placeholder={labels.emailPlaceholder}
          status={errors.email ? "error" : undefined}
          message={errors.email?.message}
          {...register("email")}
        />
        <Field
          id={PASSWORD_ID}
          label={labels.password}
          status={passwordMessage ? "error" : undefined}
          message={passwordMessage}
        >
          <div className="flex gap-2">
            <input
              id={PASSWORD_ID}
              type={showPassword ? "text" : "password"}
              autoComplete="current-password"
              aria-invalid={passwordMessage ? true : undefined}
              aria-describedby={fieldDescribedBy(PASSWORD_ID, {
                message: passwordMessage,
              })}
              className={cn(
                fieldControlClass(passwordMessage ? "error" : undefined),
                "h-12 min-w-0 flex-1",
              )}
              {...register("password")}
            />
            <Button
              variant="secondary"
              size="md"
              aria-pressed={showPassword}
              aria-controls={PASSWORD_ID}
              onClick={() => setShowPassword((value) => !value)}
              className="shrink-0"
            >
              <EyeIcon />
              {showPassword ? labels.hidePassword : labels.showPassword}
            </Button>
          </div>
        </Field>
        <Button type="submit" disabled={isSubmitting} className="w-full">
          {isSubmitting ? labels.submitting : labels.submit}
        </Button>
      </form>
    </>
  );
}
