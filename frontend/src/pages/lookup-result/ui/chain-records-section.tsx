import type { components } from "@/shared/api";
import { Table } from "@/shared/ui";
import { formatDateTime, shortenHash } from "../lib/format";
import type { LookupResultLabels } from "../model/labels";

type ChainRecord =
  components["schemas"]["ContractDetailResponseChainRecordItem"];

export interface ChainRecordsSectionProps {
  records: ChainRecord[];
  labels: LookupResultLabels;
}

/** 확정·종료 트랜잭션 표 */
export function ChainRecordsSection({
  records,
  labels,
}: ChainRecordsSectionProps) {
  return (
    <>
      <Table caption={labels.chainCaption}>
        <thead>
          <tr>
            <th scope="col">{labels.chainRecord}</th>
            <th scope="col">{labels.chainTime}</th>
            <th scope="col">{labels.chainTxHash}</th>
            <th scope="col">
              <span className="sr-only">{labels.chainLink}</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {records.map((record, index) => (
            <tr key={record.txHash ?? index}>
              <td>
                {record.recordType && labels.recordTypes[record.recordType]}
              </td>
              <td className="whitespace-nowrap">
                {record.minedAt
                  ? formatDateTime(record.minedAt)
                  : labels.chainPending}
              </td>
              <td
                title={record.txHash ?? undefined}
                className="font-mono text-sm break-all"
              >
                {record.txHash && shortenHash(record.txHash)}
              </td>
              <td>
                {record.explorerUrl && (
                  <a
                    href={record.explorerUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary underline"
                  >
                    {labels.chainExplorer}
                  </a>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
      <p className="text-sm text-muted">{labels.chainHint}</p>
    </>
  );
}
