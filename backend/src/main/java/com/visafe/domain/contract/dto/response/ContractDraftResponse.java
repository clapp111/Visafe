package com.visafe.domain.contract.dto.response;

import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.term.enums.ExtractionStatus;
import com.visafe.domain.term.enums.TermValueType;
import com.visafe.domain.term.enums.ValueSource;
import com.visafe.domain.term.enums.WarningCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 2단계 "계약 내용 검토·수정" 화면 데이터 (R4, R5 응답). */
@Schema(description = "검토할 항목 응답 DTO")
public record ContractDraftResponse(
        @Schema(description = "계약 ID", example = "31") Long contractId,

        @Schema(description = "내부 계약 번호", example = "C-2026-04817")
        String contractNo,

        @Schema(description = "stage", example = "REVIEWING")
        ContractStage stage,

        @Schema(description = "현재 제출 회차", example = "1") Integer versionNo,

        @Schema(description = "진행 중인 작업 (추출 중이면 EXTRACTION), 없으면 null", nullable = true)
        ProcessingResponse processing,

        @Schema(description = "근로자") Worker worker,
        @Schema(description = "원본 파일") File file,

        @Schema(description = "19개 항목 (TermField 순서). 추출이 끝나기 전에는 빈 배열")
        List<Field> fields,

        @Schema(description = "화면 상단 '확인이 필요한 항목'") List<Attention> attention,

        @Schema(description = "돌아온 계약의 신고·반려 내용. 돌아온 계약(또는 그 계약을 고치는 새 회차)일 때만, 아니면 null", nullable = true)
        Returned returned) {

    @Schema(description = "근로자")
    public record Worker(
            @Schema(description = "근로자 회원 ID", example = "12")
            Long memberId,

            @Schema(description = "이름", example = "Nguyễn Văn An")
            String name,

            @Schema(description = "국적 (ISO 3166-1)", example = "VN")
            String nationality) {}

    @Schema(description = "원본 파일")
    public record File(
            @Schema(description = "원본 파일 ID", example = "44")
            Long fileId,

            @Schema(description = "파일 이름", example = "표준근로계약서_한빛정밀.pdf")
            String fileName,

            @Schema(description = "파일 형식", example = "application/pdf")
            String mimeType,

            @Schema(description = "크기(바이트)", example = "1258291")
            long sizeBytes,

            @Schema(description = "쪽 수", example = "2") Integer pageCount) {}

    @Schema(description = "항목")
    public record Field(
            @Schema(description = "항목 키", example = "monthlyBaseWage")
            String key,

            @Schema(description = "표시 이름", example = "월 기본급(원)")
            String label,

            @Schema(description = "값 타입", example = "INTEGER")
            TermValueType type,

            @Schema(description = "제출 시 필수 여부", example = "true")
            boolean required,

            @Schema(description = "현재 값 (날짜 2027-01-02, 시각 09:00, 정수)", example = "2000000", nullable = true)
            Object value,

            @Schema(description = "AI가 읽은 값", example = "2000000", nullable = true)
            String aiValue,

            @Schema(description = "값 출처", example = "AI") ValueSource valueSource,

            @Schema(description = "AI 읽음 여부", example = "READ")
            ExtractionStatus extractionStatus,

            @Schema(description = "경고, 없으면 null", nullable = true)
            Warning warning,

            @Schema(description = "근거 조항, 없으면 null", nullable = true)
            SourceClause sourceClause) {}

    @Schema(description = "항목 경고")
    public record Warning(
            @Schema(description = "경고 코드", example = "MIN_WAGE")
            WarningCode code,

            @Schema(description = "화면 문구") String message,

            @Schema(description = "계산 근거 (MIN_WAGE: hourlyWage, minimumWage, year), 없으면 null", nullable = true)
            Map<String, Object> detail) {}

    @Schema(description = "근거 조항")
    public record SourceClause(
            @Schema(description = "조항 ID", example = "208") Long clauseId,

            @Schema(description = "조항 번호", example = "제6조", nullable = true)
            String clauseNo,

            @Schema(description = "쪽", example = "1", nullable = true)
            Integer pageNo) {}

    @Schema(description = "확인이 필요한 항목")
    public record Attention(
            @Schema(description = "항목 키", example = "breakMinutes")
            String key,

            @Schema(description = "이유", example = "UNREAD") AttentionReason reason) {}

    @Schema(description = "돌아온 계약의 신고·반려 내용")
    public record Returned(
            @Schema(description = "돌려보낸 쪽 (WORKER: 근로자 신고, AUTHORITY: 기관 반려)", example = "WORKER")
            ReturnedBy by,

            @Schema(description = "돌려보낸 시각", example = "2026-11-13T05:20:00Z")
            Instant at,

            @Schema(description = "원본과 다르다고 신고한 항목 키 (기관 반려면 빈 목록)", example = "[\"breakMinutes\"]")
            List<String> reportedFields,

            @Schema(description = "신고 사유 원문 (기관 반려면 반려 사유)", nullable = true)
            String reason,

            @Schema(description = "신고 사유 한국어 번역 (기관 반려면 반려 사유 그대로)", nullable = true)
            String reasonKo) {}

    /** 계약을 돌려보낸 쪽. */
    public enum ReturnedBy {
        /** 근로자가 "원본과 다릅니다"로 신고함 */
        WORKER,
        /** 감독 기관이 반려함 */
        AUTHORITY
    }

    /** 확인이 필요한 이유. */
    public enum AttentionReason {
        /** AI가 읽지 못했고 아직 비어 있음 */
        UNREAD,
        /** 최저임금 미달 */
        MIN_WAGE,
        /** AI가 읽었지만 확인이 필요함 */
        NEEDS_CHECK,
        /** 근로자가 원본과 다르다고 신고함 */
        REPORTED
    }
}
