import type { components } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { Alert } from "@/shared/ui";
import { useCurrentLanguage } from "@/shared/i18n";
import type { ContractDraftLabels } from "../model/labels";

type Schemas = components["schemas"];

/** 근로자 신고·기관 반려로 돌아온 계약의 안내 */
export function ReturnedNotice({
  returned,
  fields,
  labels,
}: {
  returned: Schemas["ContractDraftResponseReturned"];
  fields: Schemas["ContractDraftResponseField"][];
  labels: ContractDraftLabels;
}) {
  const language = useCurrentLanguage();
  const by = returned.by ?? "WORKER";
  const reportedLabels = (returned.reportedFields ?? []).map(
    (key) => fields.find((field) => field.key === key)?.label ?? key,
  );
  const showOriginal =
    returned.reason &&
    returned.reasonKo &&
    returned.reason !== returned.reasonKo;

  return (
    <Alert
      tone="warning"
      accent="left"
      title={labels.returnedTitle[by]}
      className="mb-8"
    >
      <div className="flex flex-col gap-3 text-base">
        <dl className="grid grid-cols-[120px_1fr] gap-x-4 gap-y-1">
          {returned.at && (
            <>
              <dt className="text-muted">{labels.returnedAt}</dt>
              <dd>{formatDateTime(returned.at, language)}</dd>
            </>
          )}
          {reportedLabels.length > 0 && (
            <>
              <dt className="text-muted">{labels.reportedFields}</dt>
              <dd className="font-bold">{reportedLabels.join(", ")}</dd>
            </>
          )}
          {returned.reasonKo && (
            <>
              <dt className="text-muted">{labels.returnedReason[by]}</dt>
              <dd>
                {returned.reasonKo}
                {showOriginal && (
                  <span className="block text-sm text-muted">
                    {labels.reasonOriginal}:{" "}
                    <span lang="vi">{returned.reason}</span>
                  </span>
                )}
              </dd>
            </>
          )}
        </dl>
        <p className="text-sm text-muted">{labels.returnedHint}</p>
      </div>
    </Alert>
  );
}
