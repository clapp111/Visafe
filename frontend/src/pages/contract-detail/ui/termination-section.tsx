import { Button } from "@/shared/ui";
import type { ContractDetailLabels } from "../model/labels";

/** 확정 계약의 '계약을 끝내야 하나요?' (REQUEST_TERMINATION일 때) */
export function TerminationSection({
  onRequest,
  labels,
}: {
  /** [종료 요청] — 요청 창을 연다 */
  onRequest: () => void;
  labels: ContractDetailLabels;
}) {
  const t = labels.termination;
  return (
    <section
      aria-labelledby="contract-detail-termination"
      className="flex flex-col items-start gap-3 rounded-lg border border-border p-6"
    >
      <h2 id="contract-detail-termination" className="text-xl font-bold">
        {t.sectionTitle}
      </h2>
      <p className="text-muted">{t.sectionDescription}</p>
      <Button variant="secondary" size="md" onClick={onRequest}>
        {t.request}
      </Button>
    </section>
  );
}
