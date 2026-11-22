package com.visafe.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.visafe.domain.member.dto.response.WorkerSearchResponse;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberWorkerProfile;
import com.visafe.domain.member.repository.MemberWorkerProfileRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class WorkerServiceTest {

    private final MemberWorkerProfileRepository repository = mock(MemberWorkerProfileRepository.class);
    private final WorkerService service = new WorkerService(repository);

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "N", "  N  ", "ễ"})
    void queryShorterThanTwoCharactersIsRejected(String name) {
        assertThatThrownBy(() -> service.searchByName(name))
                .isInstanceOfSatisfying(
                        RestApiException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CustomErrorCode.COMMON_INVALID_PARAMETER));
        verifyNoInteractions(repository);
    }

    @Test
    void queryIsTrimmedAndLimitedToTwenty() {
        when(repository.searchWorkerIdsByNamePrefix(anyString(), anyInt())).thenReturn(List.of());

        assertThat(service.searchByName("  Nguyen  ")).isEmpty();

        verify(repository).searchWorkerIdsByNamePrefix("Nguyen", 20);
        verify(repository, never()).findAllWithMemberByMemberIdIn(any());
    }

    @Test
    void likeWildcardsAreSearchedAsPlainCharacters() {
        assertThat(WorkerService.escapeLike("50%_a\\b")).isEqualTo("50\\%\\_a\\\\b");
    }

    @Test
    void resultsKeepSearchOrder() {
        when(repository.searchWorkerIdsByNamePrefix(anyString(), anyInt())).thenReturn(List.of(2L, 1L));
        // DB가 IN 조회 결과를 다른 순서로 주어도 검색 순서(이름 → 생년월일 → ID)를 지킨다.
        when(repository.findAllWithMemberByMemberIdIn(List.of(2L, 1L)))
                .thenReturn(List.of(profile(1L, "1998-04-17", "4821"), profile(2L, "2001-09-03", "1357")));

        List<WorkerSearchResponse> results = service.searchByName("Nguyen");

        assertThat(results).extracting(WorkerSearchResponse::memberId).containsExactly(2L, 1L);
        assertThat(results.get(0))
                .isEqualTo(new WorkerSearchResponse(2L, "Nguyễn Văn An", "VN", LocalDate.parse("2001-09-03"), "1357"));
    }

    private static MemberWorkerProfile profile(Long memberId, String birthDate, String last4) {
        Member member = Member.worker("worker" + memberId + "@demo.visafe.test", "hash", "Nguyễn Văn An");
        ReflectionTestUtils.setField(member, "id", memberId);
        MemberWorkerProfile profile = MemberWorkerProfile.builder()
                .member(member)
                .nationality("VN")
                .birthDate(LocalDate.parse(birthDate))
                .alienRegNoEnc(new byte[] {1})
                .alienRegNoHash("0x" + "0".repeat(64))
                .alienRegNoLast4(last4)
                .build();
        ReflectionTestUtils.setField(profile, "memberId", memberId);
        return profile;
    }
}
