"use client";

import { useState } from "react";
import { Button } from "@/shared/ui";

/** 글자를 클립보드에 복사하고 잠시 '복사했습니다'로 바꿔 보인다 */
export function CopyButton({
  text,
  label,
  copiedLabel,
}: {
  text: string;
  label: string;
  copiedLabel: string;
}) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  }

  return (
    <Button variant="secondary" size="md" onClick={() => void copy()}>
      <span aria-live="polite">{copied ? copiedLabel : label}</span>
    </Button>
  );
}
