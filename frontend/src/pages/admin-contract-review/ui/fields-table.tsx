import { formatTermValue } from "@/entities/contract";
import type { components } from "@/shared/api";
import { Badge, Highlight, Table } from "@/shared/ui";
import type { AdminContractReviewLabels } from "../model/labels";

type Field = components["schemas"]["ContractDraftResponseField"];

/** 검토할 항목 알림에서 이 줄로 바로 간다 */
export function fieldRowId(key?: string) {
  return `admin-field-${key ?? ""}`;
}

/** 19개 확정 항목. 고용주 입력은 AI 값과, 경고는 그 문구와 함께 보인다 */
export function FieldsTable({
  fields,
  onShowSource,
  labels,
}: {
  fields: Field[];
  /** 근거 조항 쪽으로 원본 미리보기를 넘긴다 */
  onShowSource: (pageNo: number) => void;
  labels: AdminContractReviewLabels;
}) {
  return (
    <Table caption={labels.fieldsCaption}>
      <thead>
        <tr>
          <th scope="col">{labels.columns.label}</th>
          <th scope="col">{labels.columns.value}</th>
          <th scope="col">{labels.columns.note}</th>
        </tr>
      </thead>
      <tbody>
        {fields.map((field) => {
          const byEmployer =
            field.valueSource !== undefined && field.valueSource !== "AI";
          const value = formatTermValue(field.key, field.value, labels);
          const aiValue =
            field.extractionStatus === "UNREAD" || field.aiValue == null
              ? labels.aiUnread
              : formatTermValue(field.key, field.aiValue, labels);
          const pageNo = field.sourceClause?.pageNo;
          return (
            <tr
              key={field.key}
              id={fieldRowId(field.key)}
              className="scroll-mt-6"
            >
              <th scope="row" className="whitespace-nowrap">
                {field.label}
              </th>
              <td>
                {byEmployer ? <Highlight>{value}</Highlight> : value}
                {byEmployer && (
                  <span className="block text-sm text-muted">
                    {labels.aiValue(aiValue)}
                  </span>
                )}
                {field.warning?.message && (
                  <span className="block text-sm font-bold text-warning">
                    {field.warning.message}
                  </span>
                )}
              </td>
              <td>
                <span className="flex flex-wrap items-center gap-2">
                  {field.warning && (
                    <Badge tone="warning">{labels.warningTag}</Badge>
                  )}
                  {byEmployer && (
                    <Badge tone="highlight">{labels.employerTag}</Badge>
                  )}
                  {pageNo != null && (
                    <button
                      type="button"
                      onClick={() => onShowSource(pageNo)}
                      className="cursor-pointer text-sm whitespace-nowrap text-primary underline"
                    >
                      {labels.showSource(pageNo)}
                    </button>
                  )}
                </span>
              </td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  );
}
