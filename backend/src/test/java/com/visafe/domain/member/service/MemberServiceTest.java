package com.visafe.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.visafe.domain.member.dto.response.MemberResponse;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MemberServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberService service = new MemberService(memberRepository);

    @Test
    void changeUiLanguageUpdatesMemberAndReturnsSameShapeAsGetMe() {
        Member worker = Member.worker("worker1@demo.visafe.test", "hash", "Nguyễn Văn An");
        when(memberRepository.findById(12L)).thenReturn(Optional.of(worker));

        MemberResponse response = service.changeUiLanguage(12L, "vi");

        assertThat(worker.getUiLanguage()).isEqualTo("vi");
        assertThat(response.uiLanguage()).isEqualTo("vi");
        assertThat(response).isEqualTo(service.getMember(12L));
    }

    @Test
    void changeUiLanguageOfMissingMemberFails() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeUiLanguage(99L, "vi"))
                .isInstanceOfSatisfying(
                        RestApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CustomErrorCode.MEMBER_NOT_FOUND));
    }
}
