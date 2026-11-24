import { Button } from "../button";

export interface PaginationLabels {
  pagination: string;
  previousPage: string;
  nextPage: string;
  pageStatus: (page: number, total: number) => string;
}

/** 목록 쪽 이동. page는 0부터 센다 (page.number) */
export function Pagination({
  page,
  totalPages,
  disabled,
  onChange,
  labels,
}: {
  page: number;
  totalPages: number;
  disabled: boolean;
  onChange: (page: number) => void;
  labels: PaginationLabels;
}) {
  return (
    <nav
      aria-label={labels.pagination}
      className="mt-6 flex items-center justify-center gap-3"
    >
      <Button
        variant="secondary"
        size="md"
        disabled={disabled || page <= 0}
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
        disabled={disabled || page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        {labels.nextPage}
      </Button>
    </nav>
  );
}
