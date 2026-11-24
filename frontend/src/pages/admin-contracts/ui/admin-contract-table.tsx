import Link from "next/link";
import type { components } from "@/shared/api";
import { routes } from "@/shared/config";
import { formatDateTime } from "@/shared/lib";
import { Badge, Table, buttonClass } from "@/shared/ui";
import type { AdminContractsLabels, AdminView } from "../model/labels";

type Item = components["schemas"]["AdminContractListResponseItem"];

function CountCell({
  count,
  tone,
  labels,
}: {
  count?: number;
  tone: "warning" | "highlight";
  labels: AdminContractsLabels;
}) {
  return (
    <td className="text-center">
      {count ? (
        <Badge tone={tone}>{labels.count(count)}</Badge>
      ) : (
        <span className="text-border-strong">0</span>
      )}
    </td>
  );
}

/** 기관 목록 표. 대기 목록은 [검토], 나머지는 [보기] */
export function AdminContractTable({
  items,
  view,
  title,
  labels,
}: {
  items: Item[];
  view: AdminView;
  title: string;
  labels: AdminContractsLabels;
}) {
  const { columns } = labels;
  const actionLabel = view === "PENDING" ? labels.review : labels.view;
  return (
    <Table caption={labels.caption(title)}>
      <thead>
        <tr>
          <th scope="col">{columns.contractNo}</th>
          <th scope="col">{columns.employer}</th>
          <th scope="col">{columns.worker}</th>
          <th scope="col">{columns.language}</th>
          <th scope="col">{columns.workerConfirmedAt}</th>
          <th scope="col" className="text-center">
            {columns.warningCount}
          </th>
          <th scope="col" className="text-center">
            {columns.employerInputCount}
          </th>
          <th scope="col" className="text-center">
            <span className="sr-only">{columns.action}</span>
          </th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => {
          const href =
            item.contractId !== undefined
              ? routes.adminContract(item.contractId)
              : undefined;
          return (
            <tr key={item.contractId ?? item.contractNo}>
              <td>
                {href ? (
                  <Link href={href} className="font-bold text-primary">
                    {item.contractNo}
                  </Link>
                ) : (
                  item.contractNo
                )}
              </td>
              <td>{item.companyName}</td>
              <td lang={item.language}>{item.workerName}</td>
              <td>
                {item.language &&
                  (labels.languageNames[item.language] ?? item.language)}
              </td>
              <td className="whitespace-nowrap">
                {item.workerConfirmedAt &&
                  formatDateTime(item.workerConfirmedAt)}
              </td>
              <CountCell
                count={item.warningCount}
                tone="warning"
                labels={labels}
              />
              <CountCell
                count={item.employerInputCount}
                tone="highlight"
                labels={labels}
              />
              <td className="text-center">
                {href && (
                  <Link
                    href={href}
                    aria-label={`${item.contractNo} ${actionLabel}`}
                    className={buttonClass({
                      variant: "secondary",
                      size: "md",
                    })}
                  >
                    {actionLabel}
                  </Link>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </Table>
  );
}
