package com.visafe.global.jwt;

import com.visafe.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 리프레시 토큰 (refresh_tokens, V2 마이그레이션). 회원당 1개이며 재발급할 때마다 새 토큰으로 바꾼다(rotation).
 * 토큰 원문은 저장하지 않고 SHA-256 해시만 저장한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
            @UniqueConstraint(name = "uq_refresh_tokens__member_id", columnNames = "member_id"),
            @UniqueConstraint(name = "uq_refresh_tokens__token_hash", columnNames = "token_hash")
        })
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 회원 */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 토큰 해시 (0x + SHA-256) */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 66)
    private String tokenHash;

    /** 만료 일시 */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private RefreshToken(Long memberId, String refreshToken, Instant expiresAt) {
        this.memberId = memberId;
        this.tokenHash = hash(refreshToken);
        this.expiresAt = expiresAt;
    }

    public static RefreshToken issue(Long memberId, String refreshToken, Instant expiresAt) {
        return new RefreshToken(memberId, refreshToken, expiresAt);
    }

    /** 재발급·재로그인 시 새 토큰으로 바꾼다. 이전 토큰은 더 이상 쓸 수 없다. */
    public void rotate(String refreshToken, Instant expiresAt) {
        this.tokenHash = hash(refreshToken);
        this.expiresAt = expiresAt;
    }

    /** 저장된 토큰과 같은지 비교한다. (시간 차이로 값을 추측할 수 없도록 상수 시간 비교) */
    public boolean matches(String refreshToken) {
        return MessageDigest.isEqual(
                tokenHash.getBytes(StandardCharsets.US_ASCII),
                hash(refreshToken).getBytes(StandardCharsets.US_ASCII));
    }

    static String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return "0x" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
