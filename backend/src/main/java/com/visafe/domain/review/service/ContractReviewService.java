package com.visafe.domain.review.service;

import com.visafe.domain.chain.service.ContractConfirmationService;
import com.visafe.domain.company.entity.Company;
import com.visafe.domain.contract.dto.response.ContractStageResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractClause;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.repository.ContractClauseRepository;
import com.visafe.domain.contract.service.ContractAccessService;
import com.visafe.domain.contract.service.ContractEventRecorder;
import com.visafe.domain.contract.service.ContractProcessingService;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.service.NotificationPublisher;
import com.visafe.domain.review.dto.request.ReviewResponseCreateRequest;
import com.visafe.domain.review.dto.response.ContractReviewResponse;
import com.visafe.domain.review.dto.response.ContractReviewResponse.Clause;
import com.visafe.domain.review.dto.response.ContractReviewResponse.EmployerInput;
import com.visafe.domain.review.dto.response.ContractReviewResponse.Item;
import com.visafe.domain.review.dto.response.ContractReviewResponse.KeyTerms;
import com.visafe.domain.review.dto.response.ContractReviewResponse.SourceClause;
import com.visafe.domain.review.dto.response.ContractReviewResponse.Warning;
import com.visafe.domain.review.entity.ReviewReportItem;
import com.visafe.domain.review.entity.ReviewResponse;
import com.visafe.domain.review.enums.ReviewAction;
import com.visafe.domain.review.repository.ReviewReportItemRepository;
import com.visafe.domain.review.repository.ReviewResponseRepository;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.entity.ContractTermAnnotation;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.enums.ValueSource;
import com.visafe.domain.term.enums.WarningCode;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.term.repository.ContractTermAnnotationRepository;
import com.visafe.domain.term.service.MinimumWageChecker;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.entity.TranslationClause;
import com.visafe.domain.translation.entity.TranslationItem;
import com.visafe.domain.translation.enums.TranslationStatus;
import com.visafe.domain.translation.repository.TranslationClauseRepository;
import com.visafe.domain.translation.repository.TranslationItemRepository;
import com.visafe.domain.translation.repository.TranslationRepository;
import com.visafe.domain.translation.translator.ContractTranslator;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractReviewService {

    private static final String VIETNAMESE = "vi";
    private static final String NEEDS_CHECK_MESSAGE_KO = "AI가 읽은 값이 흐리거나 모호할 수 있습니다. 원본과 비교해 확인해 주세요.";
    private static final String NEEDS_CHECK_MESSAGE_VI =
            "Giá trị này có thể chưa rõ ràng. Hãy so sánh với bản hợp đồng gốc.";

    private final ContractAccessService contractAccessService;
    private final ContractProcessingService contractProcessingService;
    private final ContractEventRecorder contractEventRecorder;
    private final ContractConfirmationService contractConfirmationService;
    private final NotificationPublisher notificationPublisher;
    private final MemberRepository memberRepository;
    private final ContractClauseRepository contractClauseRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractTermAnnotationRepository contractTermAnnotationRepository;
    private final TranslationRepository translationRepository;
    private final TranslationItemRepository translationItemRepository;
    private final TranslationClauseRepository translationClauseRepository;
    private final ReviewResponseRepository reviewResponseRepository;
    private final ReviewReportItemRepository reviewReportItemRepository;
    private final MinimumWageChecker minimumWageChecker;
    private final ContractTranslator contractTranslator;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 근로자가 확인할 계약 내용을 조회한다.
     *
     * <p>확인할 계약 내용 API(W1)에서 쓴다. 현재 회차의 마지막 완료 번역본을 기준으로 항목 번역·쉬운 설명, 원문·번역 조항,
     * 꼭 확인할 항목, 경고를 돌려준다. 값이 빈 항목은 빼고, 고용주가 고치거나 입력한 항목에는 AI가 읽은 값을 함께 준다.
     * stage가 AWAITING_WORKER가 아니어도 읽을 수 있지만 그때는 응답할 수 없다.</p>
     *
     * @param contractId 계약 ID
     * @param member 로그인한 근로자
     * @return 계약 내용 확인 화면 데이터
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 또는 현재 회차에 완료된 번역본이 없는 경우
     */
    public ContractReviewResponse getReview(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractVersion version = contract.getCurrentVersion();
        Translation translation = latestDoneTranslation(version)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TRANSLATION_NOT_READY));
        ContractLaborTerms terms =
                contractLaborTermsRepository.findById(version.getId()).orElseThrow();
        Map<TermField, ContractTermAnnotation> annotations =
                contractTermAnnotationRepository.findWithClauseByContractVersionId(version.getId()).stream()
                        .collect(Collectors.toMap(ContractTermAnnotation::getFieldName, Function.identity()));
        Map<TermField, TranslationItem> translatedItems =
                translationItemRepository.findByTranslationId(translation.getId()).stream()
                        .collect(Collectors.toMap(TranslationItem::getFieldName, Function.identity()));

        Company company = contract.getCompany();
        ContractFile file = version.getContractFile();
        return new ContractReviewResponse(
                contract.getId(),
                contract.getContractNo(),
                contract.getStatus(),
                contract.getStage(),
                contract.getStage() == ContractStage.AWAITING_WORKER,
                new ContractReviewResponse.Employer(
                        company.getName(),
                        company.getRepresentativeName(),
                        contract.getEmployer().getName()),
                translation.getCompletedAt(),
                translation.getId(),
                translation.getLanguage(),
                new ContractReviewResponse.File(file.getId(), file.getMimeType(), file.getPageCount()),
                keyTerms(terms),
                warnings(terms, annotations, translation.getLanguage()),
                items(terms, annotations, translatedItems),
                clauses(file, translation));
    }

    /**
     * 근로자의 "이해했습니다" 또는 "원본과 다릅니다" 응답을 저장한다.
     *
     * <p>근로자 응답 API(W2)에서 쓴다. W1에서 받은 번역본이 지금도 마지막 완료 번역본이어야 한다.
     * 이해했으면 WORKER_UNDERSTOOD를 남기고, 기관 검토가 필요한 계약은 AUTHORITY_REVIEW로 넘기며 그렇지 않으면 블록체인 기록을 시작한다.
     * 원본과 다르면 신고 항목을 저장하고 회차와 stage를 RETURNED로 바꾼 뒤 WORKER_REPORTED를 남기고 고용주에게 알린다.
     * 신고 사유는 한국어로 번역해 이벤트 payload의 reasonKo에 담으며, 번역에 실패해도 신고는 받는다.</p>
     *
     * @param contractId 계약 ID
     * @param request 확인한 번역본, 응답 종류, 모두 읽음 체크, 신고 항목과 사유
     * @param member 로그인한 근로자
     * @return 바뀐 stage와 진행 상태 (이해함: RECORDING 또는 AUTHORITY_REVIEW, 신고: RETURNED)
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, stage가 AWAITING_WORKER가 아닌 경우,
     *                          번역본이 마지막 완료 번역본이 아닌 경우, 이해함인데 모두 읽음 체크가 없는 경우,
     *                          신고인데 신고 항목이 없는 경우, 또는 없는 항목 키인 경우
     */
    @Transactional
    public ContractStageResponse respond(Long contractId, ReviewResponseCreateRequest request, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        if (contract.getStage() != ContractStage.AWAITING_WORKER) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        Translation translation = latestDoneTranslation(contract.getCurrentVersion())
                .filter(latest -> latest.getId().equals(request.translationId()))
                .orElseThrow(() -> new RestApiException(CustomErrorCode.TRANSLATION_OUTDATED));
        Member worker = memberRepository.getReferenceById(member.getMemberId());

        if (request.action() == ReviewAction.UNDERSTOOD) {
            understand(contract, translation, worker, request);
        } else {
            report(contract, translation, worker, request);
        }
        return ContractStageResponse.of(contract, contractProcessingService.resolve(contract));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * "이해했습니다" 응답을 저장하고 다음 단계로 넘긴다.
     *
     * @param contract 계약
     * @param translation 확인한 번역본
     * @param worker 응답한 근로자
     * @param request 응답 요청
     * @throws RestApiException 모두 읽음 체크가 없는 경우
     */
    private void understand(
            Contract contract, Translation translation, Member worker, ReviewResponseCreateRequest request) {
        if (!Boolean.TRUE.equals(request.readAllChecked())) {
            throw new RestApiException(CustomErrorCode.READ_ALL_REQUIRED);
        }
        ReviewResponse response = reviewResponseRepository.save(ReviewResponse.understood(translation, worker));
        contractEventRecorder.record(
                contract,
                ContractEventType.WORKER_UNDERSTOOD,
                worker.getId(),
                Map.of("reviewResponseId", response.getId(), "translationId", translation.getId()));
        if (contract.isRequiresAuthorityReview()) {
            contract.changeStage(ContractStage.AUTHORITY_REVIEW);
            contractEventRecorder.recordBySystem(contract, ContractEventType.AUTHORITY_REVIEW_REQUESTED, null);
            return;
        }
        contractConfirmationService.startRecording(contract, response);
    }

    /**
     * "원본과 다릅니다" 신고를 저장하고 계약을 고용주에게 돌려보낸다.
     *
     * @param contract 계약
     * @param translation 확인한 번역본
     * @param worker 신고한 근로자
     * @param request 응답 요청
     * @throws RestApiException 신고 항목이 없는 경우, 또는 없는 항목 키인 경우
     */
    private void report(
            Contract contract, Translation translation, Member worker, ReviewResponseCreateRequest request) {
        if (request.reportItems() == null || request.reportItems().isEmpty()) {
            throw new RestApiException(CustomErrorCode.REPORT_ITEMS_REQUIRED);
        }
        Set<TermField> fields = new LinkedHashSet<>();
        for (ReviewResponseCreateRequest.ReportItem item : request.reportItems()) {
            fields.add(TermField.fromKey(item.key())
                    .orElseThrow(() ->
                            new RestApiException(CustomErrorCode.TERM_KEY_UNKNOWN, "알 수 없는 항목입니다: " + item.key())));
        }

        ReviewResponse response = reviewResponseRepository.save(ReviewResponse.reported(
                translation, worker, Boolean.TRUE.equals(request.readAllChecked()), request.reason()));
        reviewReportItemRepository.saveAll(fields.stream()
                .map(field -> ReviewReportItem.itemMismatch(response, field))
                .toList());
        contract.getCurrentVersion().markReturned();
        contract.changeStage(ContractStage.RETURNED);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewResponseId", response.getId());
        payload.put("keys", fields.stream().map(TermField::getKey).toList());
        payload.put("reasonKo", reasonInKorean(request.reason(), translation.getLanguage()));
        contractEventRecorder.record(contract, ContractEventType.WORKER_REPORTED, worker.getId(), payload);
        notificationPublisher.publish(contract.getEmployer(), contract, NotificationType.CONTRACT_RETURNED);
    }

    /**
     * 신고 사유를 한국어로 번역한다.
     *
     * @param reason 신고 사유
     * @param language 사유의 언어
     * @return 한국어 번역, 사유가 없거나 번역에 실패하면 {@code null}
     */
    private String reasonInKorean(String reason, String language) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        try {
            return contractTranslator.translateToKorean(reason, language);
        } catch (RuntimeException e) {
            log.warn("[신고 사유 번역] 실패 - language={}", language, e);
            return null;
        }
    }

    /**
     * 회차의 마지막 완료 번역본을 찾는다.
     *
     * @param version 회차
     * @return 마지막 완료 번역본, 없으면 빈 {@link Optional}
     */
    private Optional<Translation> latestDoneTranslation(ContractVersion version) {
        return translationRepository.findFirstByContractVersionIdAndStatusOrderByTranslationNoDesc(
                version.getId(), TranslationStatus.DONE);
    }

    /**
     * '꼭 확인할 3가지' 카드 값을 만든다.
     *
     * @param terms 항목 값
     * @return 임금·근로시간·계약기간 항목
     */
    private static KeyTerms keyTerms(ContractLaborTerms terms) {
        return new KeyTerms(
                (Integer) terms.getValue(TermField.MONTHLY_BASE_WAGE),
                (Integer) terms.getValue(TermField.PAY_DAY),
                (String) terms.getValue(TermField.PAY_METHOD),
                text(terms.getValue(TermField.WORK_START_TIME)),
                text(terms.getValue(TermField.WORK_END_TIME)),
                (String) terms.getValue(TermField.WORK_DAYS_TEXT),
                MinimumWageChecker.weeklyHours(terms),
                (Integer) terms.getValue(TermField.BREAK_MINUTES),
                (LocalDate) terms.getValue(TermField.START_DATE),
                (LocalDate) terms.getValue(TermField.END_DATE));
    }

    /**
     * 최저임금 미달과 확인이 필요한 항목의 경고를 만든다.
     *
     * @param terms 항목 값
     * @param annotations 항목 부가 정보
     * @param language 근로자 언어
     * @return 경고 목록 (최저임금 미달이 먼저)
     */
    private List<Warning> warnings(
            ContractLaborTerms terms, Map<TermField, ContractTermAnnotation> annotations, String language) {
        List<Warning> warnings = new ArrayList<>();
        minimumWageChecker
                .check(terms)
                .ifPresent(shortfall -> warnings.add(new Warning(
                        WarningCode.MIN_WAGE,
                        TermField.MONTHLY_BASE_WAGE.getKey(),
                        VIETNAMESE.equals(language) ? minWageMessageVi(shortfall) : shortfall.message(),
                        shortfall.message(),
                        shortfall.detail())));
        for (TermField field : TermField.values()) {
            ContractTermAnnotation annotation = annotations.get(field);
            if (terms.getValue(field) != null && annotation.getWarningCode() == WarningCode.NEEDS_CHECK) {
                warnings.add(new Warning(
                        WarningCode.NEEDS_CHECK,
                        field.getKey(),
                        VIETNAMESE.equals(language) ? NEEDS_CHECK_MESSAGE_VI : NEEDS_CHECK_MESSAGE_KO,
                        NEEDS_CHECK_MESSAGE_KO,
                        null));
            }
        }
        return warnings;
    }

    /**
     * 값이 있는 항목을 번역·쉬운 설명과 함께 만든다.
     *
     * @param terms 항목 값
     * @param annotations 항목 부가 정보
     * @param translatedItems 항목 번역
     * @return 값이 있는 항목 (TermField 순서)
     */
    private static List<Item> items(
            ContractLaborTerms terms,
            Map<TermField, ContractTermAnnotation> annotations,
            Map<TermField, TranslationItem> translatedItems) {
        List<Item> items = new ArrayList<>();
        for (TermField field : TermField.values()) {
            Object value = terms.getValue(field);
            if (value == null) {
                continue;
            }
            ContractTermAnnotation annotation = annotations.get(field);
            Optional<TranslationItem> translated = Optional.ofNullable(translatedItems.get(field));
            ContractClause clause = annotation.getContractClause();
            items.add(new Item(
                    field.getKey(),
                    field.getLabel(),
                    // 시각은 명세대로 "09:00"으로 보낸다. (Jackson 기본값은 "09:00:00")
                    value instanceof LocalTime ? value.toString() : value,
                    translated.map(TranslationItem::getTranslatedValue).orElse(null),
                    translated.map(TranslationItem::getEasyExplanation).orElse(null),
                    translated.map(TranslationItem::getEasyExplanationKo).orElse(null),
                    annotation.getValueSource(),
                    annotation.getValueSource() == ValueSource.AI
                            ? null
                            : new EmployerInput(annotation.getAiValue(), annotation.getExtractionStatus()),
                    clause == null
                            ? null
                            : new SourceClause(clause.getId(), clause.getClauseNo(), clause.getPageNo())));
        }
        return items;
    }

    /**
     * 원문 조항과 번역문을 짝지어 만든다.
     *
     * @param file 원본 파일
     * @param translation 번역본
     * @return 원문·번역 조항 (읽는 순서)
     */
    private List<Clause> clauses(ContractFile file, Translation translation) {
        Map<Long, String> translatedTexts =
                translationClauseRepository.findByTranslationId(translation.getId()).stream()
                        .collect(Collectors.toMap(
                                clause -> clause.getContractClause().getId(), TranslationClause::getTranslatedText));
        return contractClauseRepository.findByContractFileIdOrderBySeqAsc(file.getId()).stream()
                .map(clause -> new Clause(
                        clause.getId(),
                        clause.getSeq(),
                        clause.getClauseNo(),
                        clause.getPageNo(),
                        clause.getTextKo(),
                        translatedTexts.get(clause.getId())))
                .toList();
    }

    /**
     * 최저임금 미달 경고의 베트남어 문구를 만든다.
     *
     * @param shortfall 최저임금 미달 계산 결과
     * @return 베트남어 문구
     */
    private static String minWageMessageVi(MinimumWageChecker.Shortfall shortfall) {
        DecimalFormat won = new DecimalFormat("#,###");
        DecimalFormat hours = new DecimalFormat("0.#");
        return "Tính theo %s giờ/tuần (%d giờ/tháng), lương theo giờ khoảng %s won, thấp hơn lương tối thiểu năm %d là %s won/giờ."
                .formatted(
                        hours.format(shortfall.weeklyHours()),
                        shortfall.monthlyHours(),
                        won.format(shortfall.hourlyWage()),
                        shortfall.year(),
                        won.format(shortfall.minimumWage()));
    }

    /**
     * 시각 값을 "09:00" 형식 문자열로 바꾼다.
     *
     * @param value 항목 값
     * @return 문자열, 값이 없으면 {@code null}
     */
    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
