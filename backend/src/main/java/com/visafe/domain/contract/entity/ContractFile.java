package com.visafe.domain.contract.entity;

import com.visafe.domain.member.entity.Member;
import com.visafe.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * B. 원본 파일 (contract_files). 업로드 후 바뀌지 않는다.
 * 파일은 비공개 저장소(storage_key)에 두고, 해시(file_hash)는 블록체인 기록·파일 대조에 쓴다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
@Entity
@Table(name = "contract_files")
public class ContractFile extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /** 저장소 경로 */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    /** 파일 이름 */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** 파일 형식 */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /** 크기(바이트) */
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** 쪽 수 */
    @Column(name = "page_count")
    private Integer pageCount;

    /** 파일 해시 keccak256 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "file_hash", nullable = false, length = 66)
    private String fileHash;

    /** 올린 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_member_id", nullable = false)
    private Member uploadedBy;

    @Builder
    private ContractFile(
            Contract contract,
            String storageKey,
            String fileName,
            String mimeType,
            long sizeBytes,
            Integer pageCount,
            String fileHash,
            Member uploadedBy) {
        this.contract = contract;
        this.storageKey = storageKey;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.pageCount = pageCount;
        this.fileHash = fileHash;
        this.uploadedBy = uploadedBy;
    }
}
