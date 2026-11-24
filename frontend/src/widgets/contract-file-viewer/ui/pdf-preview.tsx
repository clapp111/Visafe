"use client";

import { useEffect, useRef, useState } from "react";
import { Document, Page, pdfjs } from "react-pdf";
import { LoadingState } from "@/shared/ui";

pdfjs.GlobalWorkerOptions.workerSrc = new URL(
  "pdfjs-dist/build/pdf.worker.min.mjs",
  import.meta.url,
).toString();

const options = { disableRange: true, disableStream: true };
const loading = <LoadingState label="원본 계약서를 불러오는 중입니다" />;

export function PdfPreview({
  url,
  page,
  onReady,
  onError,
}: {
  url: string;
  page: number;
  onReady: (pageCount: number) => void;
  onError: () => void;
}) {
  const frame = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState({ width: 0, height: 0 });
  const [pageRatio, setPageRatio] = useState<{ page: number; ratio: number }>();

  useEffect(() => {
    const element = frame.current;
    if (!element) {
      return;
    }
    const observer = new ResizeObserver(([entry]) => {
      const { width, height } = entry.contentRect;
      setSize({ width, height });
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  const width =
    pageRatio?.page === page
      ? Math.min(size.width, size.height * pageRatio.ratio)
      : size.width;

  return (
    <div ref={frame} className="flex size-full items-center justify-center">
      <Document
        file={url}
        options={options}
        suspense={false}
        loading={loading}
        onLoadSuccess={({ numPages }) => onReady(numPages)}
        onLoadError={onError}
        onSourceError={onError}
      >
        {width > 0 && (
          <Page
            pageNumber={page}
            width={Math.max(1, Math.floor(width))}
            suspense={false}
            renderTextLayer={false}
            renderAnnotationLayer={false}
            loading={loading}
            onLoadSuccess={(pdfPage) => {
              const viewport = pdfPage.getViewport({ scale: 1 });
              const ratio = viewport.width / viewport.height;
              setPageRatio((current) =>
                current?.page === page && current.ratio === ratio
                  ? current
                  : { page, ratio },
              );
            }}
            onLoadError={onError}
            onRenderError={onError}
          />
        )}
      </Document>
    </div>
  );
}
