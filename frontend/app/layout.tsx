import type { Metadata } from "next";
import { Noto_Sans_KR } from "next/font/google";
import type { ReactNode } from "react";
import { QueryProvider } from "@/app/entrypoint";
import "@/app/styles/globals.css";

// 근로자 화면은 베트남어라 vietnamese 부분집합도 미리 받는다. 한글은 unicode-range로 필요할 때 받는다
const notoSansKr = Noto_Sans_KR({
  subsets: ["latin", "vietnamese"],
  display: "swap",
  variable: "--font-noto-sans-kr",
});

export const metadata: Metadata = {
  title: "Visafe",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko" className={notoSansKr.variable}>
      <body>
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
