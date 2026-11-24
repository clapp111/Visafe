import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ContractDraftPage } from "@/pages/contract-draft";

export const metadata: Metadata = {
  title: "계약 내용 검토 · 수정 | Visafe",
};

export default async function Page({
  params,
}: {
  params: Promise<{ contractId: string }>;
}) {
  const { contractId } = await params;
  const id = Number(contractId);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return <ContractDraftPage contractId={id} />;
}
