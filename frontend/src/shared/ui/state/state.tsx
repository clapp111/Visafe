"use client";

import type { ReactNode } from "react";
import { messages, useCurrentLanguage } from "@/shared/i18n";
import { Button } from "../button";
import { ErrorIcon, InfoIcon } from "../icons";

const BOX = "flex flex-col items-center gap-3 px-6 py-16 text-center";

export function LoadingState({ label }: { label?: string }) {
  const text = messages[useCurrentLanguage()];
  return (
    <div role="status" className={BOX}>
      <span
        aria-hidden="true"
        className="size-10 animate-spin rounded-full border-4 border-divider border-t-primary"
      />
      <span className="text-muted">{label ?? text.loading}</span>
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: ReactNode;
  description?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className={BOX}>
      <InfoIcon size={40} className="text-muted" />
      <strong className="text-lg">{title}</strong>
      {description && <p className="text-muted">{description}</p>}
      {action}
    </div>
  );
}

export function ErrorState({
  title,
  message,
  onRetry,
  retryLabel,
}: {
  title: ReactNode;
  message?: ReactNode;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  const text = messages[useCurrentLanguage()];
  return (
    <div role="alert" className={BOX}>
      <ErrorIcon size={40} className="text-danger" />
      <strong className="text-lg">{title}</strong>
      {message && <p className="text-muted">{message}</p>}
      {onRetry && (
        <Button variant="secondary" size="md" onClick={onRetry}>
          {retryLabel ?? text.retry}
        </Button>
      )}
    </div>
  );
}
