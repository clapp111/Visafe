package com.visafe.domain.review.dto.response;

import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.term.enums.ExtractionStatus;
import com.visafe.domain.term.enums.ValueSource;
import com.visafe.domain.term.enums.WarningCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 근로자 "계약 내용 확인" 화면 데이터 (W1 응답). */
@Schema(description = "확인할 계약 내용 응답 DTO")
public record ContractReviewResponse(
        @Schema(description = "계약 ID", example = "31") Long contractId,

        @Schema(description = "내부 계약 번호", example = "C-2026-04817")
        String contractNo,

        @Schema(description = "공개 상태", example = "IN_PROGRESS")
        ContractStatus status,

        @Schema(description = "stage", example = "AWAITING_WORKER")
        ContractStage stage,

        @Schema(description = "지금 응답할 수 있는지 (AWAITING_WORKER일 때만 true)", example = "true")
        boolean canRespond,

        @Schema(description = "고용주") Employer employer,

        @Schema(description = "확인 요청 시각 (번역 완료 시각)", example = "2026-11-13T04:30:00Z")
        Instant requestedAt,

        @Schema(description = "확인한 번역본 ID. 응답할 때 그대로 보낸다", example = "57")
        Long translationId,

        @Schema(description = "번역 언어", example = "vi") String language,
        @Schema(description = "원본 파일") File file,

        @Schema(description = "'꼭 확인할 3가지' (임금, 근로시간, 계약기간)")
        KeyTerms keyTerms,

        @Schema(description = "경고 (최저임금 미달, 확인 필요)") List<Warning> warnings,
        @Schema(description = "값이 있는 항목 (TermField 순서)") List<Item> items,
        @Schema(description = "원문·번역 조항 (읽는 순서)") List<Clause> clauses) {

    @Schema(description = "고용주")
    public record Employer(
            @Schema(description = "사업체명", example = "한빛정밀") String companyName,

            @Schema(description = "대표자", example = "김민수") String representativeName,

            @Schema(description = "등록한 고용주 이름", example = "김민수")
            String memberName) {}

    @Schema(description = "원본 파일")
    public record File(
            @Schema(description = "원본 파일 ID", example = "44")
            Long fileId,

            @Schema(description = "파일 형식", example = "application/pdf")
            String mimeType,

            @Schema(description = "쪽 수", example = "2") Integer pageCount) {}

    @Schema(description = "꼭 확인할 항목")
    public record KeyTerms(
            @Schema(description = "월 기본급(원)", example = "2000000", nullable = true)
            Integer monthlyBaseWage,

            @Schema(description = "임금 지급일", example = "10", nullable = true)
            Integer payDay,

            @Schema(description = "지급 방법", example = "본인 명의 계좌 입금", nullable = true)
            String payMethod,

            @Schema(description = "시업 시각", example = "09:00", nullable = true)
            String workStartTime,

            @Schema(description = "종업 시각", example = "18:00", nullable = true)
            String workEndTime,

            @Schema(description = "근무일 원문 표현", example = "월~금", nullable = true)
            String workDaysText,

            @Schema(description = "주 소정근로시간", example = "40")
            double weeklyHours,

            @Schema(description = "휴게시간(분)", example = "60", nullable = true)
            Integer breakMinutes,

            @Schema(description = "계약 시작일", example = "2027-01-02", nullable = true)
            LocalDate startDate,

            @Schema(description = "계약 종료일", example = "2029-12-31", nullable = true)
            LocalDate endDate) {}

    @Schema(description = "경고")
    public record Warning(
            @Schema(description = "경고 코드", example = "MIN_WAGE")
            WarningCode code,

            @Schema(description = "항목 키", example = "monthlyBaseWage")
            String key,

            @Schema(description = "근로자 언어 문구") String message,
            @Schema(description = "한국어 문구") String messageKo,

            @Schema(description = "계산 근거 (MIN_WAGE: hourlyWage, minimumWage, year), 없으면 null", nullable = true)
            Map<String, Object> detail) {}

    @Schema(description = "항목")
    public record Item(
            @Schema(description = "항목 키", example = "breakMinutes")
            String key,

            @Schema(description = "표시 이름", example = "휴게시간(분)")
            String label,

            @Schema(
                    description = "한국어 원본 값 (날짜 2027-01-02, 시각 09:00, 정수)",
                    example = "60",
                    types = {"string", "integer", "null"})
            Object value,

            @Schema(description = "번역된 값", example = "60 phút", nullable = true)
            String translatedValue,

            @Schema(description = "모국어 쉬운 설명", nullable = true)
            String easyExplanation,

            @Schema(description = "검토용 한국어 설명", nullable = true)
            String easyExplanationKo,

            @Schema(description = "값 출처", example = "EMPLOYER_INPUT")
            ValueSource valueSource,

            @Schema(description = "고용주가 고치거나 입력한 항목의 AI 값 (valueSource가 AI가 아닐 때만)", nullable = true)
            EmployerInput employerInput,

            @Schema(description = "근거 조항, 없으면 null", nullable = true)
            SourceClause sourceClause) {}

    @Schema(description = "고용주 입력 자세히")
    public record EmployerInput(
            @Schema(description = "AI가 읽은 값", nullable = true)
            String aiValue,

            @Schema(description = "AI 읽음 여부", example = "UNREAD")
            ExtractionStatus extractionStatus) {}

    @Schema(description = "근거 조항")
    public record SourceClause(
            @Schema(description = "조항 ID", example = "211") Long clauseId,

            @Schema(description = "조항 번호", example = "제4조", nullable = true)
            String clauseNo,

            @Schema(description = "쪽", example = "1", nullable = true)
            Integer pageNo) {}

    @Schema(description = "조항")
    public record Clause(
            @Schema(description = "조항 ID", example = "211") Long clauseId,

            @Schema(description = "읽는 순서", example = "4") int seq,

            @Schema(description = "조항 번호", example = "제4조", nullable = true)
            String clauseNo,

            @Schema(description = "쪽", example = "1", nullable = true)
            Integer pageNo,

            @Schema(description = "원문") String textKo,

            @Schema(description = "번역문", nullable = true) String translatedText) {}
}
