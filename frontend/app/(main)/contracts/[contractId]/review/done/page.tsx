import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ContractReviewDonePage } from "@/pages/contract-review-done";

export const metadata: Metadata = {
  title: "확인 완료 | Visafe",
};

export default async function Page({
  params,
}: {
  params: Promise<{ contractId: string }>;
}) {
  const { contractId } = await params;
  const id = Number(contractId);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return <ContractReviewDonePage contractId={id} />;
}
