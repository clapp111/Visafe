import type { components } from "@/shared/api";
import { formatDateTime } from "../lib/format";
import type { LookupResultLabels } from "../model/labels";

type HistoryEntry = components["schemas"]["LookupResponseHistoryEntry"];

export interface HistorySectionProps {
  history: HistoryEntry[];
  labels: LookupResultLabels;
}

/** 이벤트 아래 줄에 보일 설명. 근로자 확인은 번역 언어, 확정은 기록 완료, 나머지는 행동한 쪽 */
function entryDetail(entry: HistoryEntry, labels: LookupResultLabels) {
  if (entry.type === "WORKER_UNDERSTOOD") {
    const language = entry.detail?.language;
    return typeof language === "string"
      ? labels.translationBasis(labels.languageNames[language] ?? language)
      : undefined;
  }
  if (entry.type === "CONFIRMED") {
    return labels.confirmedDetail;
  }
  return entry.actorLabel ?? undefined;
}

/** 확정 회차의 공개 이벤트를 일어난 순서로 보인다. */
export function HistorySection({ history, labels }: HistorySectionProps) {
  return (
    <ol>
      {history.map((entry, index) => {
        const title = entry.type && labels.historyEvents[entry.type];
        if (!title) {
          return null;
        }
        const detail = entryDetail(entry, labels);
        return (
          <li
            key={`${entry.type}-${index}`}
            className="flex gap-4 border-b border-divider py-3"
          >
            <span className="flex-[0_0_170px] text-sm text-muted">
              {entry.occurredAt && formatDateTime(entry.occurredAt)}
            </span>
            <span>
              <strong>{title}</strong>
              {detail && (
                <>
                  <br />
                  <span className="text-sm text-muted">{detail}</span>
                </>
              )}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
