import type { Metadata } from "next";
import { Suspense } from "react";
import { ContractListPage } from "@/pages/contracts";

export const metadata: Metadata = {
  title: "내 계약 | Visafe",
};

// 상태 필터를 주소(?status=)에서 읽으므로 Suspense로 감싼다
export default function Page() {
  return (
    <Suspense>
      <ContractListPage />
    </Suspense>
  );
}
