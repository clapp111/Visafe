package com.visafe.domain.member.entity;

import com.visafe.domain.company.entity.Company;
import com.visafe.domain.member.enums.MemberRole;
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
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A. 회원 (members).
 * <ul>
 *   <li>고용주만 사업주(company)에 소속된다. (ck_members__company_by_role)</li>
 *   <li>login_email은 대소문자를 무시하고 고유하다. (uq_members_login_email_lower, 저장 전 소문자로 정규화)</li>
 * </ul>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "members")
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 이메일 */
    @Column(name = "login_email", nullable = false, length = 255)
    private String loginEmail;

    /** 비밀번호 해시 */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** 이름 */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role;

    /** 소속 사업주 (고용주만) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    /** 화면 언어 (ko, vi) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ui_language", nullable = false, length = 2)
    private String uiLanguage = "ko";

    private Member(String loginEmail, String passwordHash, String name, MemberRole role, Company company) {
        this.loginEmail = normalizeEmail(loginEmail);
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.company = company;
    }

    public static Member employer(String loginEmail, String passwordHash, String name, Company company) {
        Objects.requireNonNull(company, "고용주는 사업주 소속이 필요합니다.");
        return new Member(loginEmail, passwordHash, name, MemberRole.EMPLOYER, company);
    }

    public static Member worker(String loginEmail, String passwordHash, String name) {
        return new Member(loginEmail, passwordHash, name, MemberRole.WORKER, null);
    }

    public static Member admin(String loginEmail, String passwordHash, String name) {
        return new Member(loginEmail, passwordHash, name, MemberRole.ADMIN, null);
    }

    public boolean hasRole(MemberRole role) {
        return this.role == role;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeUiLanguage(String uiLanguage) {
        this.uiLanguage = uiLanguage;
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
