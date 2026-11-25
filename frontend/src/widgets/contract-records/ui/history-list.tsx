import type { components } from "@/shared/api";
import { useCurrentLanguage } from "@/shared/i18n";
import { formatDateTime } from "@/shared/lib";
import {
  CONTRACT_RECORDS_LABELS,
  type ContractRecordsLabels,
} from "../model/labels";

/** 조회(L1) history와 상세(M2) timeline은 같은 모양이다 */
type HistoryEntry =
  components["schemas"]["ContractDetailResponseTimelineEntry"];

/** 이벤트 아래 줄. 근로자 확인은 번역 언어, 신고는 사유, 확정은 기록 완료, 나머지는 행동한 쪽 */
function entryDetail(entry: HistoryEntry, labels: ContractRecordsLabels) {
  if (entry.type === "WORKER_UNDERSTOOD") {
    const language = entry.detail?.language;
    return typeof language === "string"
      ? labels.translationBasis(labels.languageNames[language] ?? language)
      : (entry.actorLabel ?? undefined);
  }
  if (entry.type === "WORKER_REPORTED") {
    const reason = entry.detail?.reasonKo;
    return typeof reason === "string" && reason.trim()
      ? labels.reportReason(reason)
      : (entry.actorLabel ?? undefined);
  }
  if (entry.type === "CONFIRMED") {
    return labels.confirmedDetail;
  }
  return entry.actorLabel ?? undefined;
}

/** 계약 이벤트를 일어난 순서로 보인다. */
export function HistoryList({ history }: { history: HistoryEntry[] }) {
  const language = useCurrentLanguage();
  const labels = CONTRACT_RECORDS_LABELS[language];
  return (
    <ol>
      {history.map((entry, index) => {
        const title = entry.type && labels.events[entry.type];
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
              {entry.occurredAt && formatDateTime(entry.occurredAt, language)}
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
