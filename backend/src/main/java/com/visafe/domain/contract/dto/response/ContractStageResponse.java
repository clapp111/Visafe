package com.visafe.domain.contract.dto.response;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractStage;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 오래 걸리는 작업(AI 추출, 번역, 블록체인 기록)을 시작하거나 stage를 바꾸는 API의 공통 응답.
 * (R1, R2, R3, R6, R7, R8, W2, D3, D4)
 *
 * @param contractId 계약 ID
 * @param contractNo 내부 계약 번호
 * @param stage 바뀐 뒤의 stage
 * @param versionNo 현재 제출 회차
 * @param processing 진행 중인 작업, 없으면 null
 */
@Schema(description = "계약 stage 변경 응답")
public record ContractStageResponse(
        @Schema(description = "계약 ID", example = "31") Long contractId,

        @Schema(description = "내부 계약 번호", example = "C-2026-04817")
        String contractNo,

        @Schema(description = "stage", example = "TRANSLATING")
        ContractStage stage,

        @Schema(description = "현재 제출 회차", example = "1") Integer versionNo,

        @Schema(description = "진행 중인 작업, 없으면 null", nullable = true)
        ProcessingResponse processing) {

    public static ContractStageResponse of(Contract contract, ProcessingResponse processing) {
        ContractVersion version = contract.getCurrentVersion();
        return new ContractStageResponse(
                contract.getId(),
                contract.getContractNo(),
                contract.getStage(),
                version == null ? null : version.getVersionNo(),
                processing);
    }
}
