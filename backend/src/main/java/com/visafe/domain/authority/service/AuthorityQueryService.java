package com.visafe.domain.authority.service;

import com.visafe.domain.authority.dto.request.AdminContractView;
import com.visafe.domain.authority.dto.response.AdminContractDetailResponse;
import com.visafe.domain.authority.dto.response.AdminContractDetailResponse.EditLog;
import com.visafe.domain.authority.dto.response.AdminContractListResponse;
import com.visafe.domain.authority.dto.response.AdminContractListResponse.Item;
import com.visafe.domain.authority.entity.AuthorityReview;
import com.visafe.domain.authority.enums.ReviewDecision;
import com.visafe.domain.authority.repository.AuthorityReviewRepository;
import com.visafe.domain.company.entity.Company;
import com.visafe.domain.contract.dto.response.ContractDetailResponse;
import com.visafe.domain.contract.dto.response.ContractListResponse.PageInfo;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.service.ContractAccessService;
import com.visafe.domain.contract.service.ContractDraftService;
import com.visafe.domain.contract.service.ContractQueryService;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.entity.MemberWorkerProfile;
import com.visafe.domain.member.repository.MemberWorkerProfileRepository;
import com.visafe.domain.term.entity.ContractTermAnnotation;
import com.visafe.domain.term.enums.ValueSource;
import com.visafe.domain.term.enums.WarningCode;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.term.repository.ContractTermAnnotationRepository;
import com.visafe.domain.term.repository.ContractTermEditLogRepository;
import com.visafe.domain.term.service.MinimumWageChecker;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorityQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ContractRepository contractRepository;
    private final ContractAccessService contractAccessService;
    private final ContractEventRepository contractEventRepository;
    private final ContractDraftService contractDraftService;
    private final ContractQueryService contractQueryService;
    private final AuthorityReviewRepository authorityReviewRepository;
    private final MemberWorkerProfileRepository memberWorkerProfileRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractTermAnnotationRepository contractTermAnnotationRepository;
    private final ContractTermEditLogRepository contractTermEditLogRepository;
    private final MinimumWageChecker minimumWageChecker;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 감독 기관의 확인 대기 목록을 조회한다.
     *
     * <p>확인 대기 목록 API(D1)에서 쓴다. PENDING은 기관 검토 중인 계약을 근로자 확인이 오래된 순으로,
     * APPROVED·REJECTED는 그 결정의 검토를 최근 순으로(검토 한 건이 한 줄), TERMINATED는 승인한 뒤 종료된 계약을 최근 종료 순으로 보여 준다.
     * 검색어는 계약 번호·사업체명·근로자 이름에서 대소문자와 베트남어 성조를 무시하고 찾는다.
     * 정렬 기준과 건수가 여러 테이블을 계산해야 나오므로 대상을 모두 읽어 메모리에서 정렬하고 페이지를 자른다.</p>
     *
     * @param view 보기
     * @param query 검색어, 없으면 전체
     * @param page 페이지 번호 (0부터)
     * @param size 페이지 크기 (1~100)
     * @return 계약 목록과 페이지 정보
     * @throws RestApiException 페이지 번호가 음수이거나 페이지 크기가 1~100이 아닌 경우
     */
    public AdminContractListResponse list(AdminContractView view, String query, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new RestApiException(CustomErrorCode.COMMON_INVALID_REQUEST, "page는 0 이상, size는 1~100이어야 합니다.");
        }
        String keyword = query == null || query.isBlank() ? null : searchable(query);
        List<Row> rows = rows(view).stream()
                .filter(row -> keyword == null || matches(row.contract(), keyword))
                .sorted(
                        view == AdminContractView.PENDING
                                ? Comparator.comparing(Row::sortTime, Comparator.nullsLast(Comparator.naturalOrder()))
                                : Comparator.comparing(Row::sortTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int from = (int) Math.min((long) page * size, rows.size());
        int to = Math.min(from + size, rows.size());
        return new AdminContractListResponse(
                rows.subList(from, to).stream().map(this::toItem).toList(),
                new PageInfo(page, size, rows.size(), (rows.size() + size - 1) / size));
    }

    /**
     * 감독 기관이 검토할 계약 내용을 조회한다.
     *
     * <p>계약 검토 API(D2)에서 쓴다. 검토 화면(R4)과 같은 19개 항목에 계약 상세(M2)와 같은 당사자·원본·진행 이력,
     * 현재 회차의 항목 수정 이력을 더한다. 기관은 번역을 검토하지 않으므로 번역은 넣지 않는다.</p>
     *
     * @param contractId 계약 ID
     * @param member 로그인한 관리자
     * @return 계약 검토 화면 데이터
     * @throws RestApiException 계약이 없는 경우
     */
    public AdminContractDetailResponse detail(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractVersion version = contract.getCurrentVersion();
        Company company = contract.getCompany();
        Member worker = contract.getWorker();
        ContractFile file = version.getContractFile();

        return new AdminContractDetailResponse(
                contract.getId(),
                contract.getContractNo(),
                contract.getStage(),
                version.getVersionNo(),
                new ContractDetailResponse.Employer(company.getName(), company.getRepresentativeName()),
                new ContractDetailResponse.Worker(
                        worker.getId(),
                        worker.getName(),
                        memberWorkerProfileRepository
                                .findById(worker.getId())
                                .map(MemberWorkerProfile::getNationality)
                                .orElse(null)),
                new ContractDetailResponse.File(
                        file.getId(), file.getFileName(), file.getMimeType(), file.getSizeBytes(), file.getPageCount()),
                contractDraftService.fieldsOf(version),
                contractTermEditLogRepository.findByContractVersionIdOrderByIdAsc(version.getId()).stream()
                        .map(log -> new EditLog(
                                log.getFieldName().getKey(),
                                log.getBeforeValue(),
                                log.getAfterValue(),
                                log.getCreatedAt()))
                        .toList(),
                contractQueryService.timeline(contract));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 보기에 맞는 목록 대상을 정렬 기준과 함께 모은다.
     *
     * @param view 보기
     * @return 계약, 기준 회차, 정렬 시각
     */
    private List<Row> rows(AdminContractView view) {
        return switch (view) {
            case PENDING ->
                contractRepository.findAllWithPartiesByStage(ContractStage.AUTHORITY_REVIEW).stream()
                        .map(contract -> new Row(
                                contract,
                                contract.getCurrentVersion(),
                                workerConfirmedAt(contract.getCurrentVersion())))
                        .toList();
            case APPROVED -> reviewRows(ReviewDecision.APPROVED).toList();
            case REJECTED -> reviewRows(ReviewDecision.REJECTED).toList();
            case TERMINATED ->
                authorityReviewRepository.findAllWithContractByDecision(ReviewDecision.APPROVED).stream()
                        .map(review -> review.getContractVersion().getContract())
                        .filter(contract -> contract.getStage() == ContractStage.ENDED)
                        .distinct()
                        .map(contract -> new Row(contract, contract.getCurrentVersion(), contract.getTerminatedAt()))
                        .toList();
        };
    }

    /**
     * 주어진 결정의 검토를 한 줄씩 만든다.
     *
     * @param decision 검토 결정
     * @return 검토한 계약과 회차, 검토 시각
     */
    private Stream<Row> reviewRows(ReviewDecision decision) {
        return authorityReviewRepository.findAllWithContractByDecision(decision).stream()
                .map((AuthorityReview review) -> new Row(
                        review.getContractVersion().getContract(), review.getContractVersion(), review.getCreatedAt()));
    }

    /**
     * 목록의 계약 한 줄을 만든다.
     *
     * @param row 목록 대상
     * @return 목록의 계약
     */
    private Item toItem(Row row) {
        Contract contract = row.contract();
        ContractVersion version = row.version();
        List<ContractTermAnnotation> annotations =
                contractTermAnnotationRepository.findWithClauseByContractVersionId(version.getId());
        long needsCheck = annotations.stream()
                .filter(annotation -> annotation.getWarningCode() == WarningCode.NEEDS_CHECK)
                .count();
        long minWage = contractLaborTermsRepository
                        .findById(version.getId())
                        .flatMap(minimumWageChecker::check)
                        .isPresent()
                ? 1
                : 0;
        long employerInput = annotations.stream()
                .filter(annotation -> annotation.getValueSource() != ValueSource.AI)
                .count();
        return new Item(
                contract.getId(),
                contract.getContractNo(),
                contract.getCompany().getName(),
                contract.getWorker().getName(),
                contract.getTargetLanguage(),
                workerConfirmedAt(version),
                (int) (needsCheck + minWage),
                (int) employerInput);
    }

    /**
     * 회차에서 근로자가 "이해했습니다"로 응답한 시각을 찾는다.
     *
     * @param version 회차
     * @return 마지막 WORKER_UNDERSTOOD 시각, 없으면 {@code null}
     */
    private Instant workerConfirmedAt(ContractVersion version) {
        return contractEventRepository
                .findFirstByContractVersionIdAndEventTypeOrderByIdDesc(
                        version.getId(), ContractEventType.WORKER_UNDERSTOOD)
                .map(ContractEvent::getCreatedAt)
                .orElse(null);
    }

    /**
     * 계약 번호·사업체명·근로자 이름 중 하나에 검색어가 들어 있는지 확인한다.
     *
     * @param contract 계약
     * @param keyword 정규화한 검색어
     * @return 들어 있으면 {@code true}, 그렇지 않으면 {@code false}
     */
    private static boolean matches(Contract contract, String keyword) {
        return Stream.of(
                        contract.getContractNo(),
                        contract.getCompany().getName(),
                        contract.getWorker().getName())
                .anyMatch(text -> searchable(text).contains(keyword));
    }

    /**
     * 대소문자와 베트남어 성조를 무시하고 비교하도록 글자를 정규화한다. (Nguyễn → nguyen, Đ → d)
     *
     * @param text 원래 글자
     * @return 결합 문자를 지우고 소문자로 바꾼 글자
     */
    private static String searchable(String text) {
        return Normalizer.normalize(text.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private record Row(Contract contract, ContractVersion version, Instant sortTime) {}
}
