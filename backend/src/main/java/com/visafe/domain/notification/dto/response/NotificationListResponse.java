package com.visafe.domain.notification.dto.response;

import com.visafe.domain.contract.dto.response.ContractListResponse.PageInfo;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** 알림 목록 (N1 응답). */
@Schema(description = "알림 목록 응답 DTO")
public record NotificationListResponse(
        @Schema(description = "안 읽은 알림 수 (unreadOnly와 관계없이 전체 기준)", example = "2")
        long unreadCount,

        @Schema(description = "알림 (최근 순)") List<Item> content,
        @Schema(description = "페이지 정보") PageInfo page) {

    @Schema(description = "알림")
    public record Item(
            @Schema(description = "알림 ID", example = "101") Long notificationId,

            @Schema(description = "알림 종류", example = "CONTRACT_RETURNED")
            NotificationType type,

            @Schema(description = "제목", example = "계약이 돌아왔습니다")
            String title,

            @Schema(description = "내용") String message,

            @Schema(description = "누르면 이동할 경로", example = "/contracts/31", nullable = true)
            String linkPath,

            @Schema(description = "관련 계약 ID", example = "31", nullable = true)
            Long contractId,

            @Schema(description = "읽은 일시, null이면 안 읽음", nullable = true)
            Instant readAt,

            @Schema(description = "알림 일시", example = "2026-11-13T05:20:00Z")
            Instant createdAt) {

        public static Item of(Notification notification) {
            return new Item(
                    notification.getId(),
                    notification.getType(),
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getLinkPath(),
                    notification.getContract() == null
                            ? null
                            : notification.getContract().getId(),
                    notification.getReadAt(),
                    notification.getCreatedAt());
        }
    }
}
