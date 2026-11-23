import type { ReactNode } from "react";
import { Button } from "../button";
import { ErrorIcon, InfoIcon } from "../icons";

const BOX = "flex flex-col items-center gap-3 px-6 py-16 text-center";

export function LoadingState({
  label = "불러오는 중입니다",
}: {
  label?: string;
}) {
  return (
    <div role="status" className={BOX}>
      <span
        aria-hidden="true"
        className="size-10 animate-spin rounded-full border-4 border-divider border-t-primary"
      />
      <span className="text-muted">{label}</span>
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
  retryLabel = "다시 시도",
}: {
  title: ReactNode;
  message?: ReactNode;
  onRetry?: () => void;
  retryLabel?: string;
}) {
  return (
    <div role="alert" className={BOX}>
      <ErrorIcon size={40} className="text-danger" />
      <strong className="text-lg">{title}</strong>
      {message && <p className="text-muted">{message}</p>}
      {onRetry && (
        <Button variant="secondary" size="md" onClick={onRetry}>
          {retryLabel}
        </Button>
      )}
    </div>
  );
}
