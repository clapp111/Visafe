package com.visafe.domain.member.entity;

import com.visafe.domain.member.enums.MemberRole;
import com.visafe.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A. 근로자 정보 (member_worker_profiles). members와 1:1이며 근로자 역할만 가진다.
 * <p>
 * 외국인등록번호는 암호화한 원문(alien_reg_no_enc)과 검색용 해시(alien_reg_no_hash),
 * 표시용 뒷자리 4개(alien_reg_no_last4)로 나눠 저장한다. 검색은 해시로만 한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "member_worker_profiles",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_member_worker_profiles__alien_reg_no_hash", columnNames = "alien_reg_no_hash")
        })
public class MemberWorkerProfile extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    /** 국적 (ISO 3166-1, 예: VN) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "nationality", nullable = false, length = 2)
    private String nationality;

    /** 생년월일 (동명이인 구분) */
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /** 모국어 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "preferred_language", nullable = false, length = 2)
    private String preferredLanguage = "vi";

    /** 외국인등록번호 (암호화) */
    @Column(name = "alien_reg_no_enc", nullable = false)
    private byte[] alienRegNoEnc;

    /** 외국인등록번호 검색용 해시 */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "alien_reg_no_hash", nullable = false, length = 66)
    private String alienRegNoHash;

    /** 외국인등록번호 뒷자리 4개 (표시용) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "alien_reg_no_last4", nullable = false, length = 4)
    private String alienRegNoLast4;

    @Builder
    private MemberWorkerProfile(
            Member member,
            String nationality,
            LocalDate birthDate,
            String preferredLanguage,
            byte[] alienRegNoEnc,
            String alienRegNoHash,
            String alienRegNoLast4) {
        if (!member.hasRole(MemberRole.WORKER)) {
            throw new IllegalArgumentException("근로자 정보는 WORKER 역할 회원만 가질 수 있습니다.");
        }
        this.member = member;
        this.nationality = nationality;
        this.birthDate = birthDate;
        if (preferredLanguage != null) {
            this.preferredLanguage = preferredLanguage;
        }
        this.alienRegNoEnc = alienRegNoEnc;
        this.alienRegNoHash = alienRegNoHash;
        this.alienRegNoLast4 = alienRegNoLast4;
    }

    public void changePreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
}
