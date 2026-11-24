import type { Metadata } from "next";
import { notFound } from "next/navigation";
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
  return <AdminContractDonePage contractId={id} />;
}
