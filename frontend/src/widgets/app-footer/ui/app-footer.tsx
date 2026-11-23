export function AppFooter({
  notice = "Visafe는 블록체인 프로젝트 수업의 프로토타입이며, 정부·공공기관의 공식 서비스가 아닙니다.",
}: {
  notice?: string;
}) {
  return (
    <footer className="border-t border-border bg-surface-muted">
      <div className="mx-auto max-w-[1200px] px-6 pt-8 pb-10 text-sm text-muted">
        <p>{notice}</p>
      </div>
    </footer>
  );
}
