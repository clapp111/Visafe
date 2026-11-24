import type { components } from "@/shared/api";
import { Alert } from "@/shared/ui";
import type { ContractDraftLabels } from "../model/labels";
import { fieldInputId } from "./term-field-input";

type Schemas = components["schemas"];

/** 화면 위 '확인이 필요한 항목' 목록. 항목 이름을 누르면 그 입력란으로 이동한다. */
export function AttentionSummary({
  attention,
  fields,
  labels,
}: {
  attention: Schemas["ContractDraftResponseAttention"][];
  fields: Schemas["ContractDraftResponseField"][];
  labels: ContractDraftLabels;
}) {
  const labelOf = (key?: string) =>
    fields.find((field) => field.key === key)?.label ?? key;

  return (
    <Alert
      tone="warning"
      accent="top"
      title={labels.attentionTitle(attention.length)}
      className="mb-8"
    >
      <ul className="flex list-disc flex-col gap-1 pl-5 text-base">
        {attention.map((item) => (
          <li key={`${item.key}-${item.reason}`}>
            <a
              href={`#${fieldInputId(item.key ?? "")}`}
              className="font-bold text-primary underline"
            >
              {labelOf(item.key)}
            </a>
            : {item.reason && labels.attentionReasons[item.reason]}
          </li>
        ))}
      </ul>
    </Alert>
  );
}
