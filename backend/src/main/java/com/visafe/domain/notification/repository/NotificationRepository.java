package com.visafe.domain.notification.repository;

import com.visafe.domain.notification.entity.Notification;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 회원의 알림 (정렬은 Pageable로). */
    Page<Notification> findByMemberId(Long memberId, Pageable pageable);

    /** 회원의 안 읽은 알림 (정렬은 Pageable로). */
    Page<Notification> findByMemberIdAndReadAtIsNull(Long memberId, Pageable pageable);

    long countByMemberIdAndReadAtIsNull(Long memberId);

    Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

    /** 회원의 안 읽은 알림을 모두 읽음으로 바꾼다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Notification n set n.readAt = :readAt where n.member.id = :memberId and n.readAt is null")
    int markAllRead(@Param("memberId") Long memberId, @Param("readAt") Instant readAt);
}
