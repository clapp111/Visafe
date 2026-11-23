import type { ReactNode } from "react";
import { FileIcon } from "../icons";

/** 올린 파일 목록. 안에 `FileItem`을 둔다. */
export function FileList({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <ul aria-label={label} className="border-t border-border">
      {children}
    </ul>
  );
}

export interface FileItemProps {
  name: string;
  /** 예: "2쪽 · 1.2MB · 업로드 완료" */
  meta?: ReactNode;
  /** 삭제 버튼 등 */
  action?: ReactNode;
}

export function FileItem({ name, meta, action }: FileItemProps) {
  return (
    <li className="flex flex-wrap items-center gap-3 border-b border-border px-1 py-4">
      <FileIcon size={24} className="shrink-0 text-primary" />
      <span className="min-w-0 flex-[1_1_240px]">
        <strong className="break-all">{name}</strong>
        {meta && <span className="block text-sm text-muted">{meta}</span>}
      </span>
      {action}
    </li>
  );
}
