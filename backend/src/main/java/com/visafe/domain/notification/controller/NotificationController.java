package com.visafe.domain.notification.controller;

import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.notification.dto.response.NotificationListResponse;
import com.visafe.domain.notification.service.NotificationQueryService;
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

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocument {

    private final NotificationQueryService notificationQueryService;

    @Override
    @GetMapping
    public ResponseEntity<NotificationListResponse> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal MemberDetails member) {
        return ResponseEntity.ok(notificationQueryService.list(unreadOnly, page, size, member));
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long notificationId, @AuthenticationPrincipal MemberDetails member) {
        notificationQueryService.markRead(notificationId, member);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal MemberDetails member) {
        notificationQueryService.markAllRead(member);
        return ResponseEntity.noContent().build();
    }
}
