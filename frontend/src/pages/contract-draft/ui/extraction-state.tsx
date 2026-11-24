import { Button, ErrorIcon } from "@/shared/ui";
import type { ContractDraftLabels } from "../model/labels";

const BOX =
  "flex flex-col items-center gap-3 rounded-lg border border-border px-6 py-16 text-center";

/** AI 추출 중·실패 안내. 실패하면 다시 시도하거나 원본을 다시 올릴 수 있다. */
export function ExtractionState({
  failed,
  onRetry,
  isRetrying,
  onReupload,
  labels,
}: {
  failed: boolean;
  /** R3 추출 다시 시도 */
  onRetry: () => void;
  isRetrying?: boolean;
  onReupload: () => void;
  labels: ContractDraftLabels;
}) {
  if (!failed) {
    return (
      <div role="status" className={BOX}>
        <span
          aria-hidden="true"
          className="size-10 animate-spin rounded-full border-4 border-divider border-t-primary"
        />
        <strong className="text-lg">{labels.extracting}</strong>
        <p className="text-muted">{labels.extractingHint}</p>
      </div>
    );
  }

  return (
    <div role="alert" className={BOX}>
      <ErrorIcon size={40} className="text-danger" />
      <strong className="text-lg">{labels.extractionFailed}</strong>
      <p className="text-muted">{labels.extractionFailedHint}</p>
      <div className="mt-2 flex flex-wrap justify-center gap-3">
        <Button variant="secondary" size="md" onClick={onReupload}>
          {labels.reupload}
        </Button>
        <Button size="md" disabled={isRetrying} onClick={onRetry}>
          {labels.retryExtraction}
        </Button>
      </div>
    </div>
  );
}
