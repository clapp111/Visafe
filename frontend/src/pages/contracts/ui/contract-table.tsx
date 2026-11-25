import Link from "next/link";
import type { components } from "@/shared/api";
import { useCurrentLanguage } from "@/shared/i18n";
import { cn, formatPeriod } from "@/shared/lib";
import {
  Badge,
  Button,
  CheckIcon,
  MinusCircleIcon,
  Table,
  buttonClass,
} from "@/shared/ui";
import type { ContractListLabels } from "../model/labels";
import { todoTarget } from "../model/todo-target";

type Item = components["schemas"]["ContractListResponseItem"];
type Role = "EMPLOYER" | "WORKER";

const STATUS_TONE = {
  IN_PROGRESS: "primary",
  CONFIRMED: "success",
  TERMINATED: "neutral",
} as const;

export interface ContractTableProps {
  items: Item[];
  role: Role;
  /** 수정 필요(FIX_REQUIRED) 계약의 [수정하기] (R8 새 회차 만들기 → 2단계) */
  onRevise: (contractId: number) => void;
  /** R8 요청 중인 계약 */
  revisingId?: number;
  labels: ContractListLabels;
}

/** 내 계약 표. 할 일이 있으면 그 일을 하러 가는 버튼, 없으면 [상세]를 둔다. */
export function ContractTable({
  items,
  role,
  onRevise,
  revisingId,
  labels,
}: ContractTableProps) {
  const language = useCurrentLanguage();
  return (
    <Table caption={labels.caption[role]}>
      <thead>
        <tr>
          <th scope="col">{labels.columns.contractNo}</th>
          <th scope="col">{labels.columns.counterparty[role]}</th>
          <th scope="col">{labels.columns.period}</th>
          <th scope="col">{labels.columns.status}</th>
          <th scope="col">{labels.columns.todo}</th>
          <th scope="col">
            <span className="sr-only">{labels.columns.action}</span>
          </th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => {
          const target = todoTarget(item);
          const todo = item.todo ? labels.todos[item.todo] : undefined;
          const period =
            item.startDate &&
            formatPeriod(item.startDate, item.endDate, language);
          const id = item.contractId ?? 0;
          const status = item.status ?? "IN_PROGRESS";
          const actionLabel = todo?.action ?? labels.detail;
          const actionClass = buttonClass({
            variant: todo ? "primary" : "secondary",
            size: "md",
          });

          return (
            <tr key={id}>
              <td>
                {target.kind === "link" ? (
                  <Link
                    href={target.href}
                    className="font-bold text-primary underline"
                  >
                    {item.contractNo}
                  </Link>
                ) : (
                  <button
                    type="button"
                    disabled={revisingId === id}
                    onClick={() => onRevise(id)}
                    className="cursor-pointer font-bold text-primary underline disabled:cursor-not-allowed"
                  >
                    {item.contractNo}
                  </button>
                )}
              </td>
              <td lang={role === "EMPLOYER" ? "vi" : undefined}>
                {item.counterpartyName}
              </td>
              <td className="whitespace-nowrap">
                {period ?? (
                  <span className="text-muted">{labels.noPeriod}</span>
                )}
              </td>
              <td>
                <Badge
                  tone={STATUS_TONE[status]}
                  size="md"
                  icon={
                    status === "CONFIRMED" ? (
                      <CheckIcon size={16} />
                    ) : status === "TERMINATED" ? (
                      <MinusCircleIcon size={16} />
                    ) : undefined
                  }
                >
                  {labels.statuses[status]}
                </Badge>
              </td>
              <td>
                {todo ? (
                  <Badge tone="warning">{todo.tag}</Badge>
                ) : (
                  <span className="text-border-strong">
                    <span aria-hidden="true">—</span>
                    <span className="sr-only">{labels.noTodo}</span>
                  </span>
                )}
              </td>
              <td>
                {target.kind === "link" ? (
                  <Link
                    href={target.href}
                    className={cn(actionClass, "whitespace-nowrap")}
                  >
                    {actionLabel}
                  </Link>
                ) : (
                  <Button
                    size="md"
                    disabled={revisingId === id}
                    onClick={() => onRevise(id)}
                    className="whitespace-nowrap"
                  >
                    {actionLabel}
                  </Button>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  );
}
