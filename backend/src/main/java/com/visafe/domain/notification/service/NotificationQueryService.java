package com.visafe.domain.notification.service;

import com.visafe.domain.contract.dto.response.ContractListResponse.PageInfo;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.notification.dto.response.NotificationListResponse;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.repository.NotificationRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final NotificationRepository notificationRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 로그인한 회원의 알림을 최근 순으로 조회한다.
     *
     * <p>알림 목록 API(N1)에서 쓴다. 안 읽은 알림 수는 안 읽은 것만 보기와 관계없이 전체 기준이다.</p>
     *
     * @param unreadOnly 안 읽은 알림만 볼지
     * @param page 페이지 번호 (0부터)
     * @param size 페이지 크기 (1~100)
     * @param member 로그인한 회원
     * @return 안 읽은 수, 알림, 페이지 정보
     * @throws RestApiException 페이지 번호가 음수이거나 페이지 크기가 1~100이 아닌 경우
     */
    public NotificationListResponse list(boolean unreadOnly, int page, int size, MemberDetails member) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new RestApiException(CustomErrorCode.COMMON_INVALID_REQUEST, "page는 0 이상, size는 1~100이어야 합니다.");
        }
        PageRequest pageable = PageRequest.of(page, size, LATEST_FIRST);
        Page<Notification> notifications = unreadOnly
                ? notificationRepository.findByMemberIdAndReadAtIsNull(member.getMemberId(), pageable)
                : notificationRepository.findByMemberId(member.getMemberId(), pageable);
        return new NotificationListResponse(
                notificationRepository.countByMemberIdAndReadAtIsNull(member.getMemberId()),
                notifications.map(NotificationListResponse.Item::of).getContent(),
                new PageInfo(page, size, notifications.getTotalElements(), notifications.getTotalPages()));
    }

    /**
     * 알림 하나를 읽음으로 바꾼다.
     *
     * <p>알림 읽음 처리 API(N2)에서 쓴다. 이미 읽은 알림이면 처음 읽은 일시를 그대로 둔다.</p>
     *
     * @param notificationId 알림 ID
     * @param member 로그인한 회원
     * @throws RestApiException 알림이 없거나 로그인한 회원의 알림이 아닌 경우
     */
    @Transactional
    public void markRead(Long notificationId, MemberDetails member) {
        notificationRepository
                .findByIdAndMemberId(notificationId, member.getMemberId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.NOTIFICATION_NOT_FOUND))
                .markRead(Instant.now());
    }

    /**
     * 로그인한 회원의 안 읽은 알림을 모두 읽음으로 바꾼다.
     *
     * <p>알림 모두 읽음 API(N3)에서 쓴다. 한 번의 UPDATE로 바꾼다.</p>
     *
     * @param member 로그인한 회원
     */
    @Transactional
    public void markAllRead(MemberDetails member) {
        notificationRepository.markAllRead(member.getMemberId(), Instant.now());
    }
}
