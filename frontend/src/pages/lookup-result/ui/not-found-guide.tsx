import type { LookupResultLabels } from "../model/labels";

/** 번호 없음(404)·형식 오류(400)일 때 확인번호를 다시 확인하도록 돕는 안내 */
export function NotFoundGuide({ labels }: { labels: LookupResultLabels }) {
  const { format, confirmed, renewed } = labels.notFoundTips;
  return (
    <section
      aria-labelledby="lookup-not-found-title"
      className="flex flex-col gap-4 rounded-md border border-border px-8 py-7"
    >
      <h2 id="lookup-not-found-title" className="text-2xl font-bold">
        {labels.notFoundTitle}
      </h2>
      <ul className="flex list-disc flex-col gap-2 pl-5">
        <li>
          {format[0]}
          <strong>{format[1]}</strong>
          {format[2]}
        </li>
        <li>
          {confirmed[0]}
          <strong>{confirmed[1]}</strong>
          {confirmed[2]}
        </li>
        <li>{renewed}</li>
      </ul>
      <p className="text-sm text-muted">{labels.notFoundHint}</p>
    </section>
  );
}
