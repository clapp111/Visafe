import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ContractDetailPage } from "@/pages/contract-detail";

export const metadata: Metadata = {
  title: "계약 상세 | Visafe",
};

export default async function Page({
  params,
}: {
  params: Promise<{ contractId: string }>;
}) {
  const { contractId } = await params;
  const id = Number(contractId);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return <ContractDetailPage contractId={id} />;
}
