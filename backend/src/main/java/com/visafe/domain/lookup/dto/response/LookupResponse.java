package com.visafe.domain.lookup.dto.response;

import com.visafe.domain.contract.dto.response.ContractDetailResponse.ChainRecordItem;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.contract.enums.TerminationReasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 확인번호 조회 결과 (L1 응답). 임금은 공개하지 않는다. */
@Schema(description = "확인번호 조회 응답 DTO")
public record LookupResponse(
        @Schema(description = "확인번호", example = "VS-7K3F-9QXM")
        String confirmationNo,

        @Schema(description = "CONFIRMED | TERMINATED", example = "CONFIRMED")
        ContractStatus status,

        @Schema(description = "사업체명", example = "한빛정밀") String companyName,

        @Schema(description = "근로자 이름 (마지막 단어 외 가림)", example = "N***** V** An")
        String workerNameMasked,

        @Schema(description = "계약 시작일", example = "2027-01-02")
        LocalDate startDate,

        @Schema(description = "계약 종료일", example = "2029-12-31", nullable = true)
        LocalDate endDate,

        @Schema(description = "확정 일시", example = "2026-11-13T05:06:00Z")
        Instant confirmedAt,

        @Schema(description = "공개하는 근로시간 조건 (임금 제외)") KeyTerms keyTerms,
        @Schema(description = "확정 기록의 무결성") Integrity integrity,

        @Schema(description = "종료 정보, 종료되지 않았으면 null", nullable = true)
        Termination termination,

        @Schema(description = "블록체인 기록") List<ChainRecordItem> chainRecords,

        @Schema(description = "확정 회차의 공개 이벤트 (SUBMITTED, WORKER_UNDERSTOOD, AUTHORITY_APPROVED, CONFIRMED, TERMINATED)")
        List<HistoryEntry> history) {

    @Schema(description = "공개하는 근로시간 조건")
    public record KeyTerms(
            @Schema(description = "시업 시각", example = "09:00")
            String workStartTime,

            @Schema(description = "종업 시각", example = "18:00")
            String workEndTime,

            @Schema(description = "휴게시간(분)", example = "60") Integer breakMinutes,
            @Schema(description = "주 근무일 수", example = "5") Integer workDaysPerWeek,

            @Schema(description = "근무일 원문 표현", example = "월~금", nullable = true)
            String workDaysText,

            @Schema(description = "주 근로시간 ((종업 − 시업 − 휴게) × 주 근무일 수)", example = "40")
            double weeklyHours) {}

    @Schema(description = "무결성")
    public record Integrity(
            @Schema(description = "VERIFIED | MISMATCH | PENDING | UNAVAILABLE", example = "VERIFIED")
            IntegrityStatus status,

            @Schema(description = "온체인 대조 시각", example = "2026-11-18T10:12:00Z")
            Instant checkedAt) {}

    /** 확정 기록의 무결성 상태. */
    public enum IntegrityStatus {
        /** DB 원본으로 다시 계산한 묶음 해시가 온체인 값과 같음 */
        VERIFIED,
        /** 다름 (온체인에 기록이 없는 경우 포함) */
        MISMATCH,
        /** 아직 블록에 포함되지 않음 */
        PENDING,
        /** 체인을 읽지 못함 */
        UNAVAILABLE
    }

    @Schema(description = "종료 정보")
    public record Termination(
            @Schema(description = "EXPIRED | MUTUAL | REPLACED", example = "MUTUAL")
            TerminationReasonType reasonType,

            @Schema(description = "종료일", example = "2027-06-30")
            LocalDate endDate,

            @Schema(description = "종료 일시") Instant terminatedAt) {}

    @Schema(description = "공개 이벤트")
    public record HistoryEntry(
            @Schema(description = "이벤트 종류", example = "SUBMITTED")
            ContractEventType type,

            @Schema(description = "일어난 시각", example = "2026-11-13T04:24:00Z")
            Instant occurredAt,

            @Schema(description = "행동한 쪽 (고용주가 한 일만 사업체명), 없으면 null", example = "한빛정밀", nullable = true)
            String actorLabel,

            @Schema(description = "상세 (WORKER_UNDERSTOOD: language), 없으면 null", nullable = true)
            Map<String, Object> detail) {}
}
