package com.visafe.domain.contract.dto.response;

import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.contract.enums.ContractAction;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.contract.enums.TerminationReasonType;
import com.visafe.domain.contract.enums.TerminationStatus;
import com.visafe.domain.term.enums.ValueSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** "내 계약 · 상세" 화면 데이터 (M2 응답). 제출·확인·확정 완료 화면이 진행 상황을 확인할 때도 쓴다. */
@Schema(description = "계약 상세 응답 DTO")
public record ContractDetailResponse(
        @Schema(description = "계약 ID", example = "31") Long contractId,

        @Schema(description = "내부 계약 번호", example = "C-2026-04817")
        String contractNo,

        @Schema(description = "공개 상태", example = "CONFIRMED")
        ContractStatus status,

        @Schema(description = "stage", example = "DONE") ContractStage stage,

        @Schema(description = "진행 중인 작업, 없으면 null", nullable = true)
        ProcessingResponse processing,

        @Schema(description = "공개 확인번호, 확정 전이면 null", example = "VS-7K3F-9QXM", nullable = true)
        String confirmationNo,

        @Schema(description = "확정 일시", nullable = true) Instant confirmedAt,
        @Schema(description = "종료 일시", nullable = true) Instant terminatedAt,
        @Schema(description = "고용주") Employer employer,
        @Schema(description = "근로자") Worker worker,

        @Schema(description = "대체한 이전 계약 ID", nullable = true)
        Long previousContractId,

        @Schema(description = "원본 파일") File file,

        @Schema(description = "현재 회차(확정 후에는 확정 회차)에서 값이 있는 항목")
        List<Term> terms,

        @Schema(description = "진행 이력 (일어난 순서, 이어진 항목 수정은 한 줄로 묶음)")
        List<TimelineEntry> timeline,

        @Schema(description = "블록체인 기록") List<ChainRecordItem> chainRecords,

        @Schema(description = "진행 중이거나 완료된 종료 (REQUESTED·AGREED), 없으면 null", nullable = true)
        Termination termination,

        @Schema(description = "지금 할 수 있는 일") List<ContractAction> availableActions) {

    @Schema(description = "고용주")
    public record Employer(
            @Schema(description = "사업체명", example = "한빛정밀") String companyName,

            @Schema(description = "대표자", example = "김민수") String representativeName) {}

    @Schema(description = "근로자")
    public record Worker(
            @Schema(description = "근로자 회원 ID", example = "12")
            Long memberId,

            @Schema(description = "이름", example = "Nguyễn Văn An")
            String name,

            @Schema(description = "국적 (ISO 3166-1)", example = "VN", nullable = true)
            String nationality) {}

    @Schema(description = "원본 파일")
    public record File(
            @Schema(description = "원본 파일 ID", example = "44")
            Long fileId,

            @Schema(description = "파일 이름", example = "원본_근로계약서.pdf")
            String fileName,

            @Schema(description = "파일 형식", example = "application/pdf")
            String mimeType,

            @Schema(description = "크기(바이트)", example = "1258291")
            long sizeBytes,

            @Schema(description = "쪽 수", example = "2") Integer pageCount) {}

    @Schema(description = "항목")
    public record Term(
            @Schema(description = "항목 키", example = "breakMinutes")
            String key,

            @Schema(description = "표시 이름", example = "휴게시간(분)")
            String label,

            @Schema(description = "값 (날짜 2027-01-02, 시각 09:00, 정수)", example = "60")
            Object value,

            @Schema(description = "값 출처 (형광펜 표시 여부)", example = "EMPLOYER_INPUT")
            ValueSource valueSource,

            @Schema(description = "설명 (근로자: 모국어 쉬운 설명, 고용주·관리자: 한국어 설명), 번역 전이면 null", nullable = true)
            String explanation) {}

    @Schema(description = "진행 이력")
    public record TimelineEntry(
            @Schema(description = "이벤트 종류", example = "SUBMITTED")
            ContractEventType type,

            @Schema(description = "일어난 시각", example = "2026-11-13T04:24:00Z")
            Instant occurredAt,

            @Schema(description = "행동한 쪽 (고용주는 사업체명, 근로자는 이름), 서버가 한 일이면 null", nullable = true)
            String actorLabel,

            @Schema(
                    description = "상세 (WORKER_UNDERSTOOD: language, TERM_EDITED: keys, WORKER_REPORTED: keys·reasonKo, "
                            + "EXTRACTION_FAILED·TRANSLATION_FAILED: error), 없으면 null",
                    nullable = true)
            Map<String, Object> detail) {}

    @Schema(description = "종료")
    public record Termination(
            @Schema(description = "종료 ID", example = "3") Long terminationId,

            @Schema(description = "EXPIRED | MUTUAL | REPLACED", example = "MUTUAL")
            TerminationReasonType reasonType,

            @Schema(description = "사유", example = "귀국 예정", nullable = true)
            String reasonText,

            @Schema(description = "종료일 (마지막 근무일)", example = "2027-06-30")
            LocalDate endDate,

            @Schema(description = "REQUESTED | AGREED", example = "REQUESTED")
            TerminationStatus status,

            @Schema(description = "로그인한 사람이 요청했는지", example = "false")
            boolean requestedByMe) {}

    @Schema(description = "블록체인 기록")
    public record ChainRecordItem(
            @Schema(description = "기록 종류", example = "CONFIRM")
            RecordType recordType,

            @Schema(description = "기록 상태", example = "MINED")
            ChainRecordStatus status,

            @Schema(description = "네트워크", example = "sepolia")
            String network,

            @Schema(description = "트랜잭션 해시, 보내기 전이면 null", nullable = true)
            String txHash,

            @Schema(description = "블록 포함 일시", nullable = true)
            Instant minedAt,

            @Schema(description = "블록 탐색기 링크, 트랜잭션 해시가 없으면 null", nullable = true)
            String explorerUrl) {

        private static final String SEPOLIA = "sepolia";
        private static final String SEPOLIA_EXPLORER_TX_URL = "https://sepolia.etherscan.io/tx/";

        /** 블록체인 기록으로 만든다. 탐색기 링크는 Sepolia 트랜잭션에만 붙는다. (계약 상세·공개 조회가 함께 씀) */
        public static ChainRecordItem of(ChainRecord record) {
            String explorerUrl = record.getTxHash() != null && SEPOLIA.equals(record.getNetwork())
                    ? SEPOLIA_EXPLORER_TX_URL + record.getTxHash()
                    : null;
            return new ChainRecordItem(
                    record.getRecordType(),
                    record.getStatus(),
                    record.getNetwork(),
                    record.getTxHash(),
                    record.getMinedAt(),
                    explorerUrl);
        }
    }
}
