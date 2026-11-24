import { formatTermValue } from "@/entities/contract";
import type { components } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import type { AdminContractReviewLabels } from "../model/labels";

type EditLog = components["schemas"]["AdminContractDetailResponseEditLog"];

/** 현재 회차에서 고용주가 고친 항목 (고친 순서) */
export function EditLogs({
  logs,
  fieldLabels,
  labels,
}: {
  logs: EditLog[];
  /** 항목 키 → 표시 이름 */
  fieldLabels: Map<string | undefined, string | undefined>;
  labels: AdminContractReviewLabels;
}) {
  if (logs.length === 0) {
    return <p className="text-muted">{labels.editLogsEmpty}</p>;
  }
  return (
    <ol>
      {logs.map((log, index) => (
        <li
          key={`${log.key}-${index}`}
          className="flex gap-4 border-b border-divider py-3"
        >
          <span className="flex-[0_0_170px] text-sm text-muted">
            {log.editedAt && formatDateTime(log.editedAt)}
          </span>
          <span>
            <strong>{fieldLabels.get(log.key) ?? log.key}</strong>
            <br />
            <span className="text-sm text-muted">
              {labels.editLog(
                formatTermValue(log.key, log.before, labels),
                formatTermValue(log.key, log.after, labels),
              )}
            </span>
          </span>
        </li>
      ))}
    </ol>
  );
}
