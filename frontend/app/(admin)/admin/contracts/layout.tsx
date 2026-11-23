import type { ReactNode } from "react";
import { AuthGuard } from "@/app/entrypoint";

export default function Layout({ children }: { children: ReactNode }) {
  return <AuthGuard>{children}</AuthGuard>;
}
