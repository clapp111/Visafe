import { Button } from "@/shared/ui";
import type { ContractListLabels } from "../model/labels";

/** 목록 쪽 이동. page는 0부터 센다 (M1 page.number) */
export function ListPagination({
  page,
  totalPages,
  onChange,
  labels,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
  labels: ContractListLabels;
}) {
  return (
    <nav
      aria-label={labels.pagination}
      className="mt-6 flex items-center justify-center gap-3"
    >
      <Button
        variant="secondary"
        size="md"
        disabled={page <= 0}
        onClick={() => onChange(page - 1)}
      >
        {labels.previousPage}
      </Button>
      <span aria-live="polite" className="text-sm">
        {labels.pageStatus(page + 1, totalPages)}
      </span>
      <Button
        variant="secondary"
        size="md"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        {labels.nextPage}
      </Button>
    </nav>
  );
}
