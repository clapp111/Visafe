import type { components } from "@/shared/api";

type WorkerSearchResponse = components["schemas"]["WorkerSearchResponse"];

// 화면 확인용 예시값 (동명이인 포함). A6 근로자 검색을 연결하면 지운다.
const WORKERS: WorkerSearchResponse[] = [
  {
    memberId: 12,
    name: "Nguyễn Văn An",
    nationality: "VN",
    birthDate: "1998-04-17",
    alienRegNoLast4: "4821",
  },
  {
    memberId: 27,
    name: "Nguyễn Văn An",
    nationality: "VN",
    birthDate: "2001-09-03",
    alienRegNoLast4: "1507",
  },
  {
    memberId: 31,
    name: "Nguyễn Thị Lan",
    nationality: "VN",
    birthDate: "1996-12-21",
    alienRegNoLast4: "3360",
  },
];

/** 이름 앞부분으로 예시 근로자를 찾는다. 서버처럼 대소문자와 베트남어 성조 부호를 무시한다. */
export function searchWorkerFixtures(name: string) {
  const fold = (text: string) =>
    text
      .normalize("NFD")
      .replace(/\p{M}/gu, "")
      .replace(/đ/gi, "d")
      .toLowerCase();
  const query = fold(name.trim());
  return WORKERS.filter((worker) => fold(worker.name ?? "").startsWith(query));
}
