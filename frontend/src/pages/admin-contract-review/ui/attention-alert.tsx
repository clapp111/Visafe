import type { components } from "@/shared/api";
import { Alert, Badge } from "@/shared/ui";
import type { AdminContractReviewLabels } from "../model/labels";
import { fieldRowId } from "./fields-table";

type Field = components["schemas"]["ContractDraftResponseField"];

function FieldLinks({ fields }: { fields: Field[] }) {
  return fields.map((field, index) => (
    <span key={field.key}>
      {index > 0 && ", "}
      <a href={`#${fieldRowId(field.key)}`} className="text-primary underline">
        {field.label}
      </a>
    </span>
  ));
}

/** 경고가 남은 항목과 고용주가 입력한 항목을 모아 보인다 */
export function AttentionAlert({
  fields,
  labels,
}: {
  fields: Field[];
  labels: AdminContractReviewLabels;
}) {
  const warned = fields.filter((field) => field.warning);
  const byEmployer = fields.filter(
    (field) => field.valueSource && field.valueSource !== "AI",
  );
  const count = warned.length + byEmployer.length;
  if (count === 0) return null;

  return (
    <Alert tone="warning" accent="top" title={labels.attentionTitle(count)}>
      <ul className="mt-1 flex flex-col gap-1.5 text-base">
        {warned.length > 0 && (
          <li className="flex flex-wrap items-center gap-2">
            <Badge tone="warning">{labels.warningTag}</Badge>
            <span>
              <FieldLinks fields={warned} />
              {warned[0]?.warning?.message &&
                warned.length === 1 &&
                ` — ${warned[0].warning.message}`}
            </span>
          </li>
        )}
        {byEmployer.length > 0 && (
          <li className="flex flex-wrap items-center gap-2">
            <Badge tone="highlight">{labels.employerTag}</Badge>
            <span>
              <FieldLinks fields={byEmployer} /> — {labels.employerReason}
            </span>
          </li>
        )}
      </ul>
      <p className="mt-2">{labels.attentionHint}</p>
    </Alert>
  );
}
