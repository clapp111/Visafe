import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { AdminContractReviewPage } from "@/pages/admin-contract-review";

export const metadata: Metadata = {
  title: "계약 검토 | Visafe",
};

export default async function Page({
  params,
}: {
  params: Promise<{ contractId: string }>;
}) {
  const { contractId } = await params;
  const id = Number(contractId);
  if (!Number.isSafeInteger(id) || id <= 0) notFound();
  return <AdminContractReviewPage contractId={id} />;
}
