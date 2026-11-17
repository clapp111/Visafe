package com.visafe.domain.translation.entity;

import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.translation.enums.TranslationStatus;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * D. 번역본 (translations). 회차·언어마다 번역본 번호(v1, v2…)가 붙는다.
 * <p>
 * 시도는 최대 3회이며, DONE일 때만 완료 일시와 번역 해시가 있다. (ck_translations__done_fields)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "translations",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_translations__contract_version_id__language__translation_no",
                    columnNames = {"contract_version_id", "language", "translation_no"})
        })
public class Translation extends BaseTimeEntity {

    public static final int MAX_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_version_id", nullable = false)
    private ContractVersion contractVersion;

    /** 언어 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "language", nullable = false, length = 2)
    private String language;

    /** 번역본 번호 (1부터) */
    @Column(name = "translation_no", nullable = false)
    private int translationNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private TranslationStatus status = TranslationStatus.PENDING;

    /** 시도 횟수 (최대 3) */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /** 번역 해시 (전문 + 근로자에게 보인 항목 설명) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "translation_hash", length = 66)
    private String translationHash;

    /** 마지막 오류 */
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    /** 완료 일시 */
    @Column(name = "completed_at")
    private Instant completedAt;

    private Translation(ContractVersion contractVersion, String language, int translationNo) {
        this.contractVersion = contractVersion;
        this.language = language;
        this.translationNo = translationNo;
    }

    public static Translation create(ContractVersion contractVersion, String language, int translationNo) {
        if (translationNo < 1) {
            throw new IllegalArgumentException("번역본 번호는 1부터 시작합니다.");
        }
        return new Translation(contractVersion, language, translationNo);
    }

    /** 번역 시도를 시작한다. (PENDING 또는 재시도 가능한 FAILED에서) */
    public void start() {
        if (status != TranslationStatus.PENDING && !(status == TranslationStatus.FAILED && canRetry())) {
            throw new IllegalStateException("번역을 시작할 수 없습니다. 상태: " + status + ", 시도: " + attemptCount);
        }
        this.attemptCount++;
        this.status = TranslationStatus.RUNNING;
    }

    public void complete(String translationHash, Instant completedAt) {
        requireRunning();
        this.translationHash = Objects.requireNonNull(translationHash, "translationHash");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
        this.lastError = null;
        this.status = TranslationStatus.DONE;
    }

    public void fail(String error) {
        requireRunning();
        this.lastError = error;
        this.status = TranslationStatus.FAILED;
    }

    public boolean canRetry() {
        return attemptCount < MAX_ATTEMPTS;
    }

    public boolean isDone() {
        return status == TranslationStatus.DONE;
    }

    private void requireRunning() {
        if (status != TranslationStatus.RUNNING) {
            throw new IllegalStateException("진행 중인 번역이 아닙니다. 상태: " + status);
        }
    }
}
