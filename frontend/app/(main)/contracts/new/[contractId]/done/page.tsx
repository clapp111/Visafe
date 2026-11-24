import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ContractSubmittedPage } from "@/pages/contract-submitted";

export const metadata: Metadata = {
  title: "제출 완료 | Visafe",
};

export default async function Page({
  params,
}: {
  params: Promise<{ contractId: string }>;
}) {
  const { contractId } = await params;
  const id = Number(contractId);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return <ContractSubmittedPage contractId={id} />;
}
