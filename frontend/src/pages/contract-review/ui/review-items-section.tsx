import { FIELD_GROUPS } from "@/entities/contract";
import type { components } from "@/shared/api";
import type { ContractReviewLabels } from "../model/labels";
import { ReviewItem } from "./review-item";

type Item = components["schemas"]["ContractReviewResponseItem"];
type Warning = components["schemas"]["ContractReviewResponseWarning"];

export interface ReviewItemsSectionProps {
  items: Item[];
  warnings: Warning[];
  language?: string;
  onShowSource: (pageNo: number) => void;
  labels: ContractReviewLabels;
}

/** 네 묶음으로 나눈 항목 설명 (값이 있는 항목만) */
export function ReviewItemsSection({
  items,
  warnings,
  language,
  onShowSource,
  labels,
}: ReviewItemsSectionProps) {
  const byKey = new Map(items.map((item) => [item.key, item]));

  return FIELD_GROUPS.map((group) => {
    const groupItems = group.rows
      .flat()
      .map((key) => byKey.get(key))
      .filter((item) => item !== undefined);
    if (groupItems.length === 0) return null;

    return (
      <section
        key={group.id}
        aria-labelledby={`contract-review-${group.id}`}
        className="flex flex-col border-t-2 border-foreground pt-6"
      >
        <h2
          id={`contract-review-${group.id}`}
          className="text-2xl leading-normal font-bold"
        >
          {labels.groups[group.id]}
        </h2>
        {groupItems.map((item) => (
          <ReviewItem
            key={item.key}
            item={item}
            warnings={warnings.filter((warning) => warning.key === item.key)}
            language={language}
            onShowSource={onShowSource}
            labels={labels}
          />
        ))}
      </section>
    );
  });
}
