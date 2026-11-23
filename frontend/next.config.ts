import type { NextConfig } from "next";
import { PHASE_DEVELOPMENT_SERVER } from "next/constants";
import { apiProxyTarget } from "./src/shared/config/env";

export default function nextConfig(phase: string): NextConfig {
  const config: NextConfig = { agentRules: false };

  if (phase === PHASE_DEVELOPMENT_SERVER) {
    config.rewrites = async () => [
      {
        source: "/api/:path*",
        destination: new URL("/api/:path*", apiProxyTarget).href,
      },
    ];
  }

  return config;
}
