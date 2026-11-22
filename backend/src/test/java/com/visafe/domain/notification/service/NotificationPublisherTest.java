package com.visafe.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.visafe.domain.contract.ContractFixtures;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.notification.entity.Notification;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotificationPublisherTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationPublisher publisher = new NotificationPublisher(repository);

    @Test
    void workerGetsVietnameseMessageLinkingToContract() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Contract contract = ContractFixtures.uploaded();
        ReflectionTestUtils.setField(contract, "id", 31L);
        Member worker = contract.getWorker();
        worker.changeUiLanguage("vi");

        Notification notification = publisher.publish(worker, contract, NotificationType.REVIEW_REQUESTED);

        assertThat(notification.getMember()).isSameAs(worker);
        assertThat(notification.getType()).isEqualTo(NotificationType.REVIEW_REQUESTED);
        assertThat(notification.getTitle()).isEqualTo("Có hợp đồng cần bạn xác nhận");
        assertThat(notification.getMessage()).contains(ContractFixtures.CONTRACT_NO);
        assertThat(notification.getLinkPath()).isEqualTo("/contracts/31");
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void employerGetsKoreanMessage() {
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Contract contract = ContractFixtures.uploaded();

        Notification notification =
                publisher.publish(contract.getEmployer(), contract, NotificationType.CONTRACT_RETURNED);

        assertThat(notification.getTitle()).isEqualTo("계약이 돌아왔습니다");
    }
}
