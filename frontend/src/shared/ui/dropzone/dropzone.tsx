"use client";

import { useState, type DragEvent } from "react";
import { cn } from "@/shared/lib";
import { buttonClass } from "../button";
import { UploadIcon } from "../icons";

export interface DropzoneProps {
  id: string;
  /** 예: "application/pdf,image/jpeg,image/png" */
  accept?: string;
  multiple?: boolean;
  disabled?: boolean;
  /** 고른 파일을 그대로 넘긴다. 형식·크기 확인은 쓰는 쪽에서 한다 */
  onFiles: (files: File[]) => void;
  /** 파일 영역을 설명하는 라벨·도움말 id */
  labelledBy?: string;
  describedBy?: string;
  dropLabel?: string;
  selectLabel?: string;
}

export function Dropzone({
  id,
  accept,
  multiple,
  disabled,
  onFiles,
  labelledBy,
  describedBy,
  dropLabel = "파일을 이곳에 끌어다 놓거나",
  selectLabel = "파일 선택",
}: DropzoneProps) {
  const [dragging, setDragging] = useState(false);

  function handleDragOver(event: DragEvent<HTMLDivElement>) {
    if (disabled) return;
    event.preventDefault();
    setDragging(true);
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    if (disabled) return;
    event.preventDefault();
    setDragging(false);
    const files = Array.from(event.dataTransfer.files);
    if (files.length > 0) onFiles(multiple ? files : files.slice(0, 1));
  }

  return (
    <div
      onDragOver={handleDragOver}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
      className={cn(
        "flex flex-col items-center gap-3 rounded-lg border-2 border-dashed px-6 py-10 text-center",
        dragging
          ? "border-primary bg-primary-subtle"
          : "border-border-strong bg-surface-muted",
        disabled && "opacity-50",
      )}
    >
      <UploadIcon size={40} className="text-muted" />
      <span>{dropLabel}</span>
      <input
        id={id}
        type="file"
        accept={accept}
        multiple={multiple}
        disabled={disabled}
        aria-labelledby={labelledBy}
        aria-describedby={describedBy}
        className="peer sr-only"
        onChange={(event) => {
          const files = Array.from(event.target.files ?? []);
          if (files.length > 0) onFiles(files);
          // 같은 파일을 다시 골라도 onChange가 불리도록 비운다
          event.target.value = "";
        }}
      />
      <label
        htmlFor={id}
        className={cn(
          buttonClass({ variant: "outline", size: "md" }),
          "peer-focus-visible:outline-3 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-primary",
          disabled && "pointer-events-none",
        )}
      >
        {selectLabel}
      </label>
    </div>
  );
}
