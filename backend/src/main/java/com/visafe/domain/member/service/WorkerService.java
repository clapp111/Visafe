package com.visafe.domain.member.service;

import com.visafe.domain.member.dto.response.WorkerSearchResponse;
import com.visafe.domain.member.entity.MemberWorkerProfile;
import com.visafe.domain.member.repository.MemberWorkerProfileRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkerService {

    /** 검색어 최소 글자 수 */
    static final int MIN_QUERY_LENGTH = 2;

    /** 검색 결과 최대 건수 */
    static final int MAX_RESULTS = 20;

    private final MemberWorkerProfileRepository memberWorkerProfileRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 이름 앞부분으로 근로자를 검색한다.
     *
     * <p>계약 등록 1단계에서 확인 요청을 보낼 근로자를 고를 때 쓴다. 대소문자와 베트남어 성조 부호를 무시하므로
     * {@code Nguyen}으로 검색해도 {@code Nguyễn}이 나온다. WORKER 역할만, 이름순으로 최대 20건을 돌려준다.
     * 검색어의 {@code %}, {@code _}는 와일드카드가 아닌 글자로 취급한다.</p>
     *
     * @param name 검색어 (앞뒤 공백은 무시)
     * @return 이름, 생년월일, 회원 ID 순으로 정렬한 근로자 목록
     * @throws RestApiException 검색어가 2글자 미만인 경우
     */
    public List<WorkerSearchResponse> searchByName(String name) {
        String query = name == null ? "" : name.strip();
        if (query.codePointCount(0, query.length()) < MIN_QUERY_LENGTH) {
            throw new RestApiException(CustomErrorCode.COMMON_INVALID_PARAMETER, "검색어는 2글자 이상 입력해 주세요.");
        }
        List<Long> memberIds =
                memberWorkerProfileRepository.searchWorkerIdsByNamePrefix(escapeLike(query), MAX_RESULTS);
        if (memberIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MemberWorkerProfile> profiles =
                memberWorkerProfileRepository.findAllWithMemberByMemberIdIn(memberIds).stream()
                        .collect(Collectors.toMap(MemberWorkerProfile::getMemberId, Function.identity()));
        return memberIds.stream()
                .map(profiles::get)
                .map(WorkerSearchResponse::from)
                .toList();
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * LIKE 검색어의 특수 문자를 이스케이프한다.
     *
     * @param query 사용자가 입력한 검색어
     * @return {@code \}, {@code %}, {@code _} 앞에 {@code \}를 붙인 검색어
     */
    static String escapeLike(String query) {
        return query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
