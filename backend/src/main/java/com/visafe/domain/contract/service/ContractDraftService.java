package com.visafe.domain.contract.service;

import com.visafe.domain.contract.dto.request.TermsUpdateRequest;
import com.visafe.domain.contract.dto.response.ContractDraftResponse;
import com.visafe.domain.contract.dto.response.ContractDraftResponse.Attention;
import com.visafe.domain.contract.dto.response.ContractDraftResponse.AttentionReason;
import com.visafe.domain.contract.dto.response.ContractDraftResponse.Field;
import com.visafe.domain.contract.dto.response.ContractDraftResponse.SourceClause;
import com.visafe.domain.contract.dto.response.ContractDraftResponse.Warning;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractClause;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.entity.MemberWorkerProfile;
import com.visafe.domain.member.repository.MemberRepository;
import com.visafe.domain.member.repository.MemberWorkerProfileRepository;
import com.visafe.domain.term.TermValueParser;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.entity.ContractTermAnnotation;
import com.visafe.domain.term.entity.ContractTermEditLog;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.enums.WarningCode;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.term.repository.ContractTermAnnotationRepository;
import com.visafe.domain.term.repository.ContractTermEditLogRepository;
import com.visafe.domain.term.service.MinimumWageChecker;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractDraftService {

    /** 검토 화면을 볼 수 있는 stage */
    private static final Set<ContractStage> VIEWABLE_STAGES =
            EnumSet.of(ContractStage.UPLOADED, ContractStage.REVIEWING, ContractStage.RETURNED);

    private static final String NEEDS_CHECK_MESSAGE = "AI가 읽은 값이 흐리거나 모호합니다. 원본과 비교해 확인해 주세요.";

    private final ContractAccessService contractAccessService;
    private final ContractProcessingService contractProcessingService;
    private final ContractEventRecorder contractEventRecorder;
    private final MemberRepository memberRepository;
    private final MemberWorkerProfileRepository memberWorkerProfileRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractTermAnnotationRepository contractTermAnnotationRepository;
    private final ContractTermEditLogRepository contractTermEditLogRepository;
    private final MinimumWageChecker minimumWageChecker;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 2단계 검토 화면에 보여 줄 항목을 조회한다.
     *
     * <p>검토할 항목 조회 API(R4)에서 쓴다. 현재 회차의 19개 항목을 값, AI가 읽은 값, 값 출처, 경고, 근거 조항과 함께 돌려준다.
     * 추출이 끝나기 전(UPLOADED)에는 항목이 비어 있다. 최저임금 경고는 저장하지 않고 조회할 때 계산한다.</p>
     *
     * @param contractId 계약 ID
     * @param member 로그인한 고용주
     * @return 검토 화면 데이터
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 또는 stage가 UPLOADED·REVIEWING·RETURNED가 아닌 경우
     */
    public ContractDraftResponse getDraft(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        if (!VIEWABLE_STAGES.contains(contract.getStage())) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        return toResponse(contract);
    }

    /**
     * 고용주가 바꾼 항목을 저장한다.
     *
     * <p>항목 수정 API(R5)에서 쓴다. 모든 값을 먼저 검사한 뒤 한꺼번에 바꾸며, 값이 실제로 바뀐 항목만 수정 이력에 남긴다.
     * 값 출처는 서버가 정한다: AI가 못 읽은 항목이면 EMPLOYER_INPUT, AI 값과 다르면 EMPLOYER_EDIT, AI 값으로 되돌리면 AI.
     * 바뀐 항목이 있으면 TERM_EDITED 이벤트를 하나 남긴다.</p>
     *
     * @param contractId 계약 ID
     * @param request 바꾼 항목 (키 → 값, 비우려면 null)
     * @param member 로그인한 고용주
     * @return 다시 계산한 검토 화면 데이터
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, stage가 REVIEWING이 아니거나 회차가 DRAFT가 아닌 경우,
     *                          없는 항목 키인 경우, 또는 값의 형식·범위가 틀린 경우
     */
    @Transactional
    public ContractDraftResponse updateTerms(Long contractId, TermsUpdateRequest request, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractVersion version = contract.getCurrentVersion();
        if (contract.getStage() != ContractStage.REVIEWING || !version.isDraft()) {
            throw new RestApiException(CustomErrorCode.CONTRACT_STAGE_CONFLICT);
        }
        Map<TermField, Object> values = parseValues(request.values());
        ContractLaborTerms terms =
                contractLaborTermsRepository.findById(version.getId()).orElseThrow();
        checkPeriod(terms, values);

        Map<TermField, ContractTermAnnotation> annotations = annotationsOf(version);
        Member editor = memberRepository.getReferenceById(member.getMemberId());
        List<String> changedKeys = new ArrayList<>();
        for (Map.Entry<TermField, Object> entry : values.entrySet()) {
            TermField field = entry.getKey();
            Object before = terms.getValue(field);
            Object after = entry.getValue();
            if (Objects.equals(before, after)) {
                continue;
            }
            terms.changeValue(field, after);
            contractTermEditLogRepository.save(
                    ContractTermEditLog.of(version, field, text(before), text(after), editor));
            ContractTermAnnotation annotation = annotations.get(field);
            annotation.markEditedByEmployer(sameAsAiValue(field, annotation, after));
            changedKeys.add(field.getKey());
        }
        if (!changedKeys.isEmpty()) {
            contractEventRecorder.record(
                    contract, ContractEventType.TERM_EDITED, member.getMemberId(), Map.of("keys", changedKeys));
        }
        return toResponse(contract);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 검토 화면 응답을 만든다.
     *
     * @param contract 계약
     * @return 현재 회차 기준 검토 화면 데이터
     */
    private ContractDraftResponse toResponse(Contract contract) {
        ContractVersion version = contract.getCurrentVersion();
        ContractFile file = version.getContractFile();
        Member worker = contract.getWorker();
        String nationality = memberWorkerProfileRepository
                .findById(worker.getId())
                .map(MemberWorkerProfile::getNationality)
                .orElse(null);

        List<Field> fields = new ArrayList<>();
        List<Attention> attention = new ArrayList<>();
        Optional<ContractLaborTerms> terms = contractLaborTermsRepository.findById(version.getId());
        if (terms.isPresent()) {
            Map<TermField, ContractTermAnnotation> annotations = annotationsOf(version);
            Optional<MinimumWageChecker.Shortfall> shortfall = minimumWageChecker.check(terms.get());
            for (TermField field : TermField.values()) {
                Object value = terms.get().getValue(field);
                ContractTermAnnotation annotation = annotations.get(field);
                Warning warning = null;
                if (field == TermField.MONTHLY_BASE_WAGE && shortfall.isPresent()) {
                    warning = new Warning(
                            WarningCode.MIN_WAGE,
                            shortfall.get().message(),
                            shortfall.get().detail());
                    attention.add(new Attention(field.getKey(), AttentionReason.MIN_WAGE));
                } else if (annotation.getWarningCode() == WarningCode.NEEDS_CHECK) {
                    warning = new Warning(WarningCode.NEEDS_CHECK, NEEDS_CHECK_MESSAGE, null);
                }
                if (annotation.getWarningCode() == WarningCode.NEEDS_CHECK) {
                    attention.add(new Attention(field.getKey(), AttentionReason.NEEDS_CHECK));
                }
                if (annotation.isUnread() && value == null) {
                    attention.add(new Attention(field.getKey(), AttentionReason.UNREAD));
                }
                fields.add(new Field(
                        field.getKey(),
                        field.getLabel(),
                        field.getType(),
                        field.isRequired(),
                        // 시각은 명세대로 "09:00"으로 보낸다. (Jackson 기본값은 "09:00:00")
                        value instanceof LocalTime ? value.toString() : value,
                        annotation.getAiValue(),
                        annotation.getValueSource(),
                        annotation.getExtractionStatus(),
                        warning,
                        sourceClause(annotation.getContractClause())));
            }
        }

        return new ContractDraftResponse(
                contract.getId(),
                contract.getContractNo(),
                contract.getStage(),
                version.getVersionNo(),
                contractProcessingService.resolve(contract),
                new ContractDraftResponse.Worker(worker.getId(), worker.getName(), nationality),
                new ContractDraftResponse.File(
                        file.getId(), file.getFileName(), file.getMimeType(), file.getSizeBytes(), file.getPageCount()),
                fields,
                attention,
                null);
    }

    /**
     * 회차의 항목 부가 정보를 항목별로 모은다.
     *
     * @param version 회차
     * @return 항목 → 부가 정보 (근거 조항 포함)
     */
    private Map<TermField, ContractTermAnnotation> annotationsOf(ContractVersion version) {
        return contractTermAnnotationRepository.findWithClauseByContractVersionId(version.getId()).stream()
                .collect(Collectors.toMap(ContractTermAnnotation::getFieldName, Function.identity()));
    }

    /**
     * 요청 값을 항목과 항목 타입으로 바꾼다. 하나라도 틀리면 아무것도 바꾸지 않는다.
     *
     * @param raw 요청의 항목 키 → JSON 값
     * @return 항목 → 바꿀 값 (요청 순서 유지, 비우는 항목은 null)
     * @throws RestApiException 없는 항목 키인 경우, 또는 값의 형식·범위가 틀린 경우
     */
    private static Map<TermField, Object> parseValues(Map<String, Object> raw) {
        Map<TermField, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            TermField field = TermField.fromKey(entry.getKey())
                    .orElseThrow(() ->
                            new RestApiException(CustomErrorCode.TERM_KEY_UNKNOWN, "알 수 없는 항목입니다: " + entry.getKey()));
            Object value = entry.getValue();
            if (value == null) {
                values.put(field, null);
                continue;
            }
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
                throw new RestApiException(CustomErrorCode.TERM_VALUE_INVALID, field.getKey() + ": 값 형식이 올바르지 않습니다.");
            }
            try {
                values.put(field, TermValueParser.parse(field, String.valueOf(value)));
            } catch (IllegalArgumentException e) {
                throw new RestApiException(CustomErrorCode.TERM_VALUE_INVALID, field.getKey() + ": " + e.getMessage());
            }
        }
        return values;
    }

    /**
     * 바꾼 뒤의 계약 종료일이 시작일보다 앞서지 않는지 검사한다. (ck_contract_labor_terms__period)
     *
     * @param terms 지금 항목 값
     * @param values 바꿀 값
     * @throws RestApiException 종료일이 시작일보다 앞서는 경우
     */
    private static void checkPeriod(ContractLaborTerms terms, Map<TermField, Object> values) {
        Object start = values.containsKey(TermField.START_DATE)
                ? values.get(TermField.START_DATE)
                : terms.getValue(TermField.START_DATE);
        Object end = values.containsKey(TermField.END_DATE)
                ? values.get(TermField.END_DATE)
                : terms.getValue(TermField.END_DATE);
        if (start instanceof LocalDate startDate && end instanceof LocalDate endDate && endDate.isBefore(startDate)) {
            throw new RestApiException(CustomErrorCode.TERM_VALUE_INVALID, "endDate: 계약 종료일은 시작일보다 앞설 수 없습니다.");
        }
    }

    /**
     * 바꾼 값이 AI가 읽은 값과 같은지 확인한다. AI가 못 읽은 항목은 비운 상태가 AI 값이다.
     *
     * @param field 항목
     * @param annotation 항목 부가 정보
     * @param value 바꾼 값
     * @return AI가 읽은 값과 같으면 {@code true}, 그렇지 않으면 {@code false}
     */
    private static boolean sameAsAiValue(TermField field, ContractTermAnnotation annotation, Object value) {
        if (annotation.isUnread()) {
            return value == null;
        }
        try {
            return Objects.equals(TermValueParser.parse(field, annotation.getAiValue()), value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 수정 이력에 남길 문자열 표기를 만든다. (날짜 2027-01-02, 시각 09:00)
     *
     * @param value 항목 값
     * @return 문자열 표기, 값이 없으면 {@code null}
     */
    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * 근거 조항 응답을 만든다.
     *
     * @param clause 근거 조항
     * @return 근거 조항 응답, 없으면 {@code null}
     */
    private static SourceClause sourceClause(ContractClause clause) {
        return clause == null ? null : new SourceClause(clause.getId(), clause.getClauseNo(), clause.getPageNo());
    }
}
