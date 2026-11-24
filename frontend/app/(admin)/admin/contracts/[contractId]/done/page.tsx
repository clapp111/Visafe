import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { AdminContractDonePage } from "@/pages/admin-contract-done";

export const metadata: Metadata = {
  title: "확정 완료 | Visafe",
};

export default async function Page({
  params,
}: {
  params: Promise<{ contractId: string }>;
}) {
  const { contractId } = await params;
  const id = Number(contractId);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  // 화면 확인용 ?state=를 읽으므로 Suspense로 감싼다 (API 연동 단계에서 지운다)
  return (
    <Suspense>
      <AdminContractDonePage contractId={id} />
    </Suspense>
  );
}
