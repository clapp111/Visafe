package com.visafe.domain.chain.entity;

import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.contract.entity.ContractTermination;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.review.entity.ReviewResponse;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * F. 블록체인 기록 (chain_records).
 * <ul>
 *   <li>CONFIRM: 근로자 확인 응답·번역본·파일/항목/번역 해시 필수, 종료 정보 없음.</li>
 *   <li>TERMINATE: 종료 기록과 이전 확정 기록 필수, 근로자 응답 없음.</li>
 *   <li>구성 요소 해시는 record_hash를 다시 계산해 검증하기 위한 고정 기록이다.</li>
 *   <li>SENT·MINED일 때만 tx_hash·sent_at이, MINED일 때만 block_number·mined_at이 있다.</li>
 * </ul>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "chain_records",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_chain_records__record_hash", columnNames = "record_hash"),
            @UniqueConstraint(name = "uq_chain_records__tx_hash", columnNames = "tx_hash"),
            @UniqueConstraint(
                    name = "uq_chain_records__anchor_key__record_type",
                    columnNames = {"anchor_key", "record_type"})
        })
public class ChainRecord extends BaseTimeEntity {

    public static final int CURRENT_HASH_RULE_VERSION = 1;
    public static final String DEFAULT_NETWORK = "sepolia";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기록한 회차 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_version_id", nullable = false)
    private ContractVersion contractVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 10)
    private RecordType recordType;

    /** 온체인에 올린 묶음 해시 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "record_hash", nullable = false, length = 66)
    private String recordHash;

    /** keccak256(확인번호) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "anchor_key", nullable = false, length = 66)
    private String anchorKey;

    /** 해시 계산 규칙 버전 */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "hash_rule_version", nullable = false)
    private int hashRuleVersion = CURRENT_HASH_RULE_VERSION;

    /** 근로자 식별값용 무작위값 (32바이트) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "nonce", nullable = false, length = 66)
    private String nonce;

    /** 구성 요소: 파일 해시 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "file_hash", length = 66)
    private String fileHash;

    /** 구성 요소: 항목 해시 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "items_hash", length = 66)
    private String itemsHash;

    /** 기록한 번역본 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "translation_id")
    private Translation translation;

    /** 구성 요소: 번역 해시 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "translation_hash", length = 66)
    private String translationHash;

    /** 구성 요소: 이력 해시 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "events_hash", nullable = false, length = 66)
    private String eventsHash;

    /** 확정: 근로자 확인 응답 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_response_id", unique = true)
    private ReviewResponse reviewResponse;

    /** 종료: 종료 기록 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_termination_id", unique = true)
    private ContractTermination contractTermination;

    /** 종료: 이전 확정 기록 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_chain_record_id")
    private ChainRecord previousChainRecord;

    /** 네트워크 */
    @Column(name = "network", nullable = false, length = 20)
    private String network = DEFAULT_NETWORK;

    /** 트랜잭션 해시 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tx_hash", length = 66)
    private String txHash;

    /** 블록 번호 */
    @Column(name = "block_number")
    private Long blockNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ChainRecordStatus status = ChainRecordStatus.PENDING;

    /** 재시도 횟수 */
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    /** 전송 일시 */
    @Column(name = "sent_at")
    private Instant sentAt;

    /** 블록 포함 일시 */
    @Column(name = "mined_at")
    private Instant minedAt;

    private ChainRecord(
            ContractVersion contractVersion,
            RecordType recordType,
            String recordHash,
            String anchorKey,
            String nonce,
            String eventsHash,
            String network) {
        this.contractVersion = Objects.requireNonNull(contractVersion, "contractVersion");
        this.recordType = recordType;
        this.recordHash = Objects.requireNonNull(recordHash, "recordHash");
        this.anchorKey = Objects.requireNonNull(anchorKey, "anchorKey");
        this.nonce = Objects.requireNonNull(nonce, "nonce");
        this.eventsHash = Objects.requireNonNull(eventsHash, "eventsHash");
        if (network != null) {
            this.network = network;
        }
    }

    /** 확정 기록 (근로자 "이해했습니다" 이후). */
    @Builder(builderMethodName = "confirmBuilder", builderClassName = "ConfirmBuilder")
    private static ChainRecord confirm(
            ContractVersion contractVersion,
            String recordHash,
            String anchorKey,
            String nonce,
            String fileHash,
            String itemsHash,
            Translation translation,
            String translationHash,
            String eventsHash,
            ReviewResponse reviewResponse,
            String network) {
        if (reviewResponse == null || !reviewResponse.isUnderstood()) {
            throw new IllegalArgumentException("확정 기록에는 '이해했습니다' 응답이 필요합니다.");
        }
        ChainRecord record =
                new ChainRecord(contractVersion, RecordType.CONFIRM, recordHash, anchorKey, nonce, eventsHash, network);
        record.fileHash = Objects.requireNonNull(fileHash, "fileHash");
        record.itemsHash = Objects.requireNonNull(itemsHash, "itemsHash");
        record.translation = Objects.requireNonNull(translation, "translation");
        record.translationHash = Objects.requireNonNull(translationHash, "translationHash");
        record.reviewResponse = reviewResponse;
        return record;
    }

    /** 종료 기록 (이전 확정 기록과 연결). */
    @Builder(builderMethodName = "terminateBuilder", builderClassName = "TerminateBuilder")
    private static ChainRecord terminate(
            ContractVersion contractVersion,
            String recordHash,
            String anchorKey,
            String nonce,
            String eventsHash,
            ContractTermination contractTermination,
            ChainRecord previousChainRecord,
            String network) {
        if (previousChainRecord == null || previousChainRecord.getRecordType() != RecordType.CONFIRM) {
            throw new IllegalArgumentException("종료 기록은 이전 확정 기록이 필요합니다.");
        }
        ChainRecord record = new ChainRecord(
                contractVersion, RecordType.TERMINATE, recordHash, anchorKey, nonce, eventsHash, network);
        record.contractTermination = Objects.requireNonNull(contractTermination, "contractTermination");
        record.previousChainRecord = previousChainRecord;
        return record;
    }

    /** 트랜잭션을 보냈다. */
    public void markSent(String txHash, Instant sentAt) {
        if (status != ChainRecordStatus.PENDING) {
            throw new IllegalStateException("전송 대기 중인 기록만 보낼 수 있습니다. 현재: " + status);
        }
        this.txHash = Objects.requireNonNull(txHash, "txHash");
        this.sentAt = Objects.requireNonNull(sentAt, "sentAt");
        this.status = ChainRecordStatus.SENT;
    }

    /** 트랜잭션이 블록에 포함되었다. */
    public void markMined(long blockNumber, Instant minedAt) {
        if (status != ChainRecordStatus.SENT) {
            throw new IllegalStateException("전송된 기록만 블록 포함 처리할 수 있습니다. 현재: " + status);
        }
        this.blockNumber = blockNumber;
        this.minedAt = Objects.requireNonNull(minedAt, "minedAt");
        this.status = ChainRecordStatus.MINED;
    }

    /** 전송·채굴에 실패했다. tx 정보는 지우고 재시도 횟수를 늘린다. */
    public void markFailed() {
        if (status == ChainRecordStatus.MINED) {
            throw new IllegalStateException("이미 블록에 포함된 기록입니다.");
        }
        this.txHash = null;
        this.sentAt = null;
        this.retryCount++;
        this.status = ChainRecordStatus.FAILED;
    }

    /** 실패한 기록을 다시 전송 대기로 돌린다. */
    public void retry() {
        if (status != ChainRecordStatus.FAILED) {
            throw new IllegalStateException("실패한 기록만 재시도할 수 있습니다. 현재: " + status);
        }
        this.status = ChainRecordStatus.PENDING;
    }

    public boolean isMined() {
        return status == ChainRecordStatus.MINED;
    }
}
