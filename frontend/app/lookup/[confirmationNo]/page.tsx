import type { Metadata } from "next";
import { LookupResultPage } from "@/pages/lookup-result";

export const metadata: Metadata = {
  title: "조회 결과 | Visafe",
};

export default async function Page({
  params,
}: {
  params: Promise<{ confirmationNo: string }>;
}) {
  const { confirmationNo } = await params;
  return <LookupResultPage confirmationNo={confirmationNo} />;
}
