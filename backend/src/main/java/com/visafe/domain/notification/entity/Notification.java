package com.visafe.domain.notification.entity;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.global.common.BaseCreatedTimeEntity;
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
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * G. 알림 (notifications). 실제로 보내지 않고 DB에만 저장해 화면 알림 목록에서 보여 준다.
 * read_at이 null이면 안 읽은 알림이다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications")
public class Notification extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 받는 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** 관련 계약 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    /** 제목 */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** 내용 */
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    /** 누르면 이동할 경로 ('/'로 시작) */
    @Column(name = "link_path", length = 255)
    private String linkPath;

    /** 읽은 일시 (null이면 안 읽음) */
    @Column(name = "read_at")
    private Instant readAt;

    @Builder
    private Notification(
            Member member, Contract contract, NotificationType type, String title, String message, String linkPath) {
        if (linkPath != null && !linkPath.startsWith("/")) {
            throw new IllegalArgumentException("linkPath는 '/'로 시작해야 합니다.");
        }
        this.member = Objects.requireNonNull(member, "member");
        this.contract = contract;
        this.type = Objects.requireNonNull(type, "type");
        this.title = title;
        this.message = message;
        this.linkPath = linkPath;
    }

    public void markRead(Instant readAt) {
        if (this.readAt == null) {
            this.readAt = Objects.requireNonNull(readAt, "readAt");
        }
    }

    public boolean isRead() {
        return readAt != null;
    }
}
