package com.visafe.domain.contract.extraction;

import com.visafe.domain.contract.extraction.ExtractionResult.Clause;
import com.visafe.domain.contract.extraction.ExtractionResult.Term;
import com.visafe.domain.term.enums.TermField;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 실제 LLM 연동 전까지 쓰는 추출기. 원본과 관계없이 표준근로계약서 예시 결과를 돌려준다.
 * 검토 화면을 확인할 수 있도록 휴게시간은 읽지 못한 것으로, 주휴일은 확인이 필요한 것으로 둔다.
 */
@Component
public class StubContractExtractor implements ContractExtractor {

    private static final List<Clause> CLAUSES = List.of(
            new Clause(null, 1, "사업주 한빛정밀(대표 김민수, 경기도 안산시 단원구 데모로 1)과 근로자 Nguyễn Văn An(국적 베트남)은 다음과 같이 근로계약을 체결한다."),
            new Clause("제1조", 1, "근로계약기간: 2027년 1월 2일부터 2029년 12월 31일까지"),
            new Clause("제2조", 1, "근무 장소: 경기도 안산시 단원구 데모로 1 한빛정밀 제2공장"),
            new Clause("제3조", 1, "업무 내용: 금속 부품 가공 및 포장"),
            new Clause("제4조", 1, "근로시간: 09시 00분부터 18시 00분까지 (휴게시간:    시    분 ~    시    분)"),
            new Clause("제5조", 1, "근무일 및 휴일: 매주 5일(월~금) 근무, 주휴일 매주 일요일"),
            new Clause("제6조", 1, "임금: 월 기본급 2,000,000원"),
            new Clause("제7조", 2, "임금 지급일: 매월 10일, 지급 방법: 근로자 본인 명의 예금통장에 입금"),
            new Clause("제8조", 2, "숙식 제공: 숙소 제공(기숙사), 식사 중식 제공"),
            new Clause("제9조", 2, "연장·야간·휴일근로 시에는 통상임금의 50%를 가산하여 지급한다."));

    private static final Map<TermField, Term> TERMS = Map.ofEntries(
            Map.entry(TermField.EMPLOYER_NAME, new Term("한빛정밀", false, 0)),
            Map.entry(TermField.REPRESENTATIVE_NAME, new Term("김민수", false, 0)),
            Map.entry(TermField.EMPLOYER_ADDRESS, new Term("경기도 안산시 단원구 데모로 1", false, 0)),
            Map.entry(TermField.WORKER_NAME, new Term("Nguyễn Văn An", false, 0)),
            Map.entry(TermField.WORKER_NATIONALITY, new Term("VN", false, 0)),
            Map.entry(TermField.START_DATE, new Term("2027-01-02", false, 1)),
            Map.entry(TermField.END_DATE, new Term("2029-12-31", false, 1)),
            Map.entry(TermField.WORK_PLACE, new Term("경기도 안산시 단원구 데모로 1 한빛정밀 제2공장", false, 2)),
            Map.entry(TermField.JOB_DESCRIPTION, new Term("금속 부품 가공 및 포장", false, 3)),
            Map.entry(TermField.WORK_START_TIME, new Term("09:00", false, 4)),
            Map.entry(TermField.WORK_END_TIME, new Term("18:00", false, 4)),
            Map.entry(TermField.WORK_DAYS_PER_WEEK, new Term("5", false, 5)),
            Map.entry(TermField.WORK_DAYS_TEXT, new Term("월~금", false, 5)),
            Map.entry(TermField.WEEKLY_HOLIDAY, new Term("일요일", true, 5)),
            Map.entry(TermField.MONTHLY_BASE_WAGE, new Term("2000000", false, 6)),
            Map.entry(TermField.PAY_DAY, new Term("10", false, 7)),
            Map.entry(TermField.PAY_METHOD, new Term("근로자 본인 명의 예금통장에 입금", false, 7)),
            Map.entry(TermField.ACCOMMODATION, new Term("숙소 제공(기숙사), 중식 제공", false, 8)));

    @Override
    public ExtractionResult extract(byte[] content, String mimeType) {
        return new ExtractionResult(CLAUSES, TERMS);
    }
}
