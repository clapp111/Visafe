import type { components } from "@/shared/api";

type Field = components["schemas"]["ContractDraftResponseField"];

/** 값이 비어 있는 필수 항목의 키. 제출 전 확인과 422 TERMS_REQUIRED_MISSING 표시에 함께 쓴다. */
export function missingRequiredKeys(fields: Field[]) {
  return fields
    .filter(
      (field) =>
        field.required &&
        field.key &&
        (field.value === null || field.value === undefined),
    )
    .map((field) => field.key as string);
}
