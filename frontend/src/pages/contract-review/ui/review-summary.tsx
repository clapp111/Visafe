import type { components } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { Badge } from "@/shared/ui";
import type { ContractReviewLabels } from "../model/labels";

type Review = components["schemas"]["ContractReviewResponse"];

/** 고용주·요청 일시·계약 번호·상태 */
export function ReviewSummary({
  review,
  labels,
}: {
  review: Review;
  labels: ContractReviewLabels;
}) {
  const employer = [review.employer?.companyName, review.employer?.memberName]
    .filter(Boolean)
    .join(" · ");
  const rows = [
    { label: labels.summary.employer, value: employer },
    {
      label: labels.summary.requestedAt,
      value: review.requestedAt && formatDateTime(review.requestedAt),
    },
    { label: labels.summary.contractNo, value: review.contractNo },
  ];

  return (
    <dl className="mb-8 flex flex-wrap gap-x-10 gap-y-3 rounded-md border border-border px-6 py-5">
      {rows.map(
        (row) =>
          row.value && (
            <div key={row.label} className="flex gap-3">
              <dt className="text-muted">{row.label}</dt>
              <dd className="font-bold">{row.value}</dd>
            </div>
          ),
      )}
      {review.status && (
        <div className="flex items-center gap-3">
          <dt className="text-muted">{labels.summary.status}</dt>
          <dd>
            <Badge tone="primary">{labels.statuses[review.status]}</Badge>
          </dd>
        </div>
      )}
    </dl>
  );
}
