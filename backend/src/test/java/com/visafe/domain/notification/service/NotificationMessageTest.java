package com.visafe.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.visafe.domain.notification.enums.NotificationType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NotificationMessageTest {

    private static final String CONTRACT_NO = "C-2026-04817";

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void everyTypeHasKoreanAndVietnameseWithinColumnLimits(NotificationType type) {
        for (String language : new String[] {"ko", "vi"}) {
            NotificationMessage message = NotificationMessage.of(type, language, CONTRACT_NO);

            assertThat(message.title()).isNotBlank().hasSizeLessThanOrEqualTo(100);
            assertThat(message.message()).contains(CONTRACT_NO).hasSizeLessThanOrEqualTo(500);
        }
    }

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void unknownLanguageFallsBackToKorean(NotificationType type) {
        assertThat(NotificationMessage.of(type, "en", CONTRACT_NO))
                .isEqualTo(NotificationMessage.of(type, "ko", CONTRACT_NO));
    }
}
