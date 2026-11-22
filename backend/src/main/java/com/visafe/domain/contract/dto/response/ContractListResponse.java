package com.visafe.domain.contract.dto.response;

import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.contract.enums.ContractTodo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** "내 계약" 목록 화면 데이터 (M1 응답). */
@Schema(description = "내 계약 목록 응답 DTO")
public record ContractListResponse(
        @Schema(description = "상태 필터 칩 숫자 (필터와 관계없이 전체 기준)") Counts counts,

        @Schema(description = "할 일이 있는 계약 수 (배너, 필터와 관계없이 전체 기준)", example = "1")
        int todoCount,

        @Schema(description = "계약 목록 (할 일이 있는 계약 먼저, 그다음 최근 수정 순)")
        List<Item> content,

        @Schema(description = "페이지 정보") PageInfo page) {

    @Schema(description = "상태별 계약 수")
    public record Counts(
            @Schema(description = "전체", example = "5") long all,
            @Schema(description = "진행 중", example = "2") long inProgress,
            @Schema(description = "확정", example = "2") long confirmed,
            @Schema(description = "종료", example = "1") long terminated) {}

    @Schema(description = "계약")
    public record Item(
            @Schema(description = "계약 ID", example = "35") Long contractId,

            @Schema(description = "내부 계약 번호", example = "C-2026-04833")
            String contractNo,

            @Schema(description = "상대방 (고용주에게는 근로자 이름, 근로자에게는 사업체명)", example = "Lê Thị Hoa")
            String counterpartyName,

            @Schema(description = "계약 시작일, 추출 전이면 null", example = "2027-01-02", nullable = true)
            LocalDate startDate,

            @Schema(description = "계약 종료일", example = "2028-12-31", nullable = true)
            LocalDate endDate,

            @Schema(description = "공개 상태", example = "IN_PROGRESS")
            ContractStatus status,

            @Schema(description = "stage", example = "RETURNED")
            ContractStage stage,

            @Schema(description = "할 일, 없으면 null", example = "FIX_REQUIRED", nullable = true)
            ContractTodo todo,

            @Schema(description = "최근 수정 시각", example = "2026-11-13T05:20:00Z")
            Instant updatedAt) {}

    @Schema(description = "페이지 정보")
    public record PageInfo(
            @Schema(description = "페이지 번호 (0부터)", example = "0")
            int number,

            @Schema(description = "페이지 크기", example = "20") int size,
            @Schema(description = "전체 건수", example = "5") long totalElements,
            @Schema(description = "전체 페이지 수", example = "1") int totalPages) {}
}
