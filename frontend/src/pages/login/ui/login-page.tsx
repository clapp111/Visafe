"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { getPostLoginPath, useSession } from "@/entities/session";
import { getApiErrorCode } from "@/shared/api";
import { routes } from "@/shared/config";
import { useCurrentLanguage } from "@/shared/i18n";
import { InfoIcon } from "@/shared/ui";
import { LOGIN_LABELS } from "../model/labels";
import type { LoginValues } from "../model/schema";
import { LoginForm } from "./login-form";

export function LoginPage() {
  const labels = LOGIN_LABELS[useCurrentLanguage()];
  const router = useRouter();
  const { session, login, isLoggingIn, loginError } = useSession();
  const isSubmitting = session.status === "restoring" || isLoggingIn;
  const errorMessage = loginError
    ? getApiErrorCode(loginError) === "AUTH_LOGIN_FAILED"
      ? labels.invalidCredentials
      : labels.loginUnavailable
    : undefined;

  async function handleSubmit(values: LoginValues) {
    if (isSubmitting) {
      return;
    }

    const returnTo = new URLSearchParams(window.location.search).get("next");
    try {
      const member = await login(values);
      router.replace(getPostLoginPath(member.role, returnTo));
    } catch {
      // The login mutation supplies the form's error message.
    }
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

      <div className="mx-auto flex max-w-[520px] flex-col gap-6">
        <div className="flex flex-col gap-3">
          <h1 className="text-5xl font-bold tracking-[-0.5px]">
            {labels.title}
          </h1>
          <p className="text-muted">{labels.description}</p>
        </div>

        <LoginForm
          onSubmit={handleSubmit}
          isSubmitting={isSubmitting}
          errorMessage={errorMessage}
          labels={labels}
        />

        <div className="flex items-start gap-3 rounded-md bg-surface-muted px-5 py-4">
          <InfoIcon size={24} className="mt-0.5 shrink-0 text-primary" />
          <p className="text-sm text-muted">
            {labels.guestHint}{" "}
            <Link
              href={routes.home}
              className="font-bold text-primary underline"
            >
              {labels.guestLink}
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
