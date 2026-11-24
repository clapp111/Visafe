import type { Metadata } from "next";
import { Suspense } from "react";
import { AdminContractsPage, adminViewTitle } from "@/pages/admin-contracts";

export async function generateMetadata({
  searchParams,
}: {
  searchParams: Promise<{ view?: string | string[] }>;
}): Promise<Metadata> {
  const { view } = await searchParams;
  return {
    title: `${adminViewTitle(typeof view === "string" ? view : undefined)} | Visafe`,
  };
}

// 보기(?view=)와 검색어(?q=)를 주소에서 읽으므로 Suspense로 감싼다
export default function Page() {
  return (
    <Suspense>
      <AdminContractsPage />
    </Suspense>
  );
}
