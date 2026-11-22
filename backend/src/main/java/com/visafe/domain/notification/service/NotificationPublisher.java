package com.visafe.domain.notification.service;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationPublisher {

    private final NotificationRepository notificationRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 계약과 관련된 알림을 저장한다.
     *
     * <p>실제로 보내지 않고 DB에만 저장하며, 알림 화면이 생기면 목록 API로 보여 준다.
     * 제목과 내용은 받는 사람의 화면 언어로 만들고, 누르면 계약 상세({@code /contracts/{id}})로 이동한다.</p>
     *
     * @param recipient 알림을 받을 회원
     * @param contract 관련 계약
     * @param type 알림 종류
     * @return 저장한 알림
     */
    @Transactional
    public Notification publish(Member recipient, Contract contract, NotificationType type) {
        NotificationMessage message = NotificationMessage.of(type, recipient.getUiLanguage(), contract.getContractNo());
        return notificationRepository.save(Notification.builder()
                .member(recipient)
                .contract(contract)
                .type(type)
                .title(message.title())
                .message(message.message())
                .linkPath("/contracts/" + contract.getId())
                .build());
    }
}
