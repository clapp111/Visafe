package com.visafe.domain.notification.controller;

import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.notification.dto.response.NotificationListResponse;
import com.visafe.domain.notification.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "알림 (선택 API)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @Operation(summary = "알림 목록", description = "내 알림을 최근 순으로 조회한다. unreadOnly=true면 안 읽은 알림만 본다.")
    @GetMapping
    public NotificationListResponse list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MemberDetails member) {
        return notificationQueryService.list(unreadOnly, page, size, member);
    }

    @Operation(summary = "알림 읽음 처리", description = "내 알림 하나를 읽음으로 바꾼다. 이미 읽은 알림이면 그대로 둔다.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long notificationId, @AuthenticationPrincipal MemberDetails member) {
        notificationQueryService.markRead(notificationId, member);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 모두 읽음", description = "내 안 읽은 알림을 모두 읽음으로 바꾼다.")
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal MemberDetails member) {
        notificationQueryService.markAllRead(member);
        return ResponseEntity.noContent().build();
    }
}
