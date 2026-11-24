import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";
import { ContractReviewPage } from "@/pages/contract-review";

export const metadata: Metadata = {
  title: "근로계약 내용 확인 | Visafe",
};

export default async function Page({
  params,
}: {
  params: Promise<{ contractId: string }>;
}) {
  const { contractId } = await params;
  const id = Number(contractId);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return (
    <Suspense>
      <ContractReviewPage contractId={id} />
    </Suspense>
  );
}
