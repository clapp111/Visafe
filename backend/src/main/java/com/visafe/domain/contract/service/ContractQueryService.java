package com.visafe.domain.contract.service;

import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.domain.company.entity.Company;
import com.visafe.domain.contract.dto.response.ContractDetailResponse;
import com.visafe.domain.contract.dto.response.ContractDetailResponse.ChainRecordItem;
import com.visafe.domain.contract.dto.response.ContractDetailResponse.Term;
import com.visafe.domain.contract.dto.response.ContractDetailResponse.TimelineEntry;
import com.visafe.domain.contract.dto.response.ContractListResponse;
import com.visafe.domain.contract.dto.response.ContractListResponse.Counts;
import com.visafe.domain.contract.dto.response.ContractListResponse.Item;
import com.visafe.domain.contract.dto.response.ContractListResponse.PageInfo;
import com.visafe.domain.contract.dto.response.ProcessingResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractAction;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.contract.enums.ContractTodo;
import com.visafe.domain.contract.enums.ProcessingStatus;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.domain.member.entity.MemberWorkerProfile;
import com.visafe.domain.member.enums.MemberRole;
import com.visafe.domain.member.repository.MemberWorkerProfileRepository;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.entity.ContractTermAnnotation;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.term.repository.ContractTermAnnotationRepository;
import com.visafe.domain.translation.entity.TranslationItem;
import com.visafe.domain.translation.enums.TranslationStatus;
import com.visafe.domain.translation.repository.TranslationItemRepository;
import com.visafe.domain.translation.repository.TranslationRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
public class ContractQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ContractRepository contractRepository;
    private final ContractAccessService contractAccessService;
    private final ContractProcessingService contractProcessingService;
    private final ContractEventRepository contractEventRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractTermAnnotationRepository contractTermAnnotationRepository;
    private final MemberWorkerProfileRepository memberWorkerProfileRepository;
    private final TranslationRepository translationRepository;
    private final TranslationItemRepository translationItemRepository;
    private final ChainRecordRepository chainRecordRepository;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 로그인한 사람의 계약 목록을 조회한다.
     *
     * <p>내 계약 목록 API(M1)에서 쓴다. 고용주는 같은 사업주의 모든 계약, 근로자는 본인 계약을 본다.
     * 할 일은 추출·번역 실패처럼 DB 컬럼에 없는 값으로 정해지므로 볼 수 있는 계약을 모두 읽어 계산한 뒤,
     * 할 일이 있는 계약을 먼저, 그다음 최근 수정 순으로 정렬하고 페이지를 자른다. 상태별 수와 할 일 수는 필터와 관계없이 센다.</p>
     *
     * @param status 상태 필터, 없으면 전체
     * @param page 페이지 번호 (0부터)
     * @param size 페이지 크기 (1~100)
     * @param member 로그인한 고용주 또는 근로자
     * @return 상태별 수, 할 일 수, 계약 목록, 페이지 정보
     * @throws RestApiException 페이지 번호가 음수이거나 페이지 크기가 1~100이 아닌 경우
     */
    public ContractListResponse list(ContractStatus status, int page, int size, MemberDetails member) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new RestApiException(CustomErrorCode.COMMON_INVALID_REQUEST, "page는 0 이상, size는 1~100이어야 합니다.");
        }
        boolean employer = member.getRole() == MemberRole.EMPLOYER;
        List<Contract> contracts = employer
                ? contractRepository.findAllForEmployer(member.getMemberId())
                : contractRepository.findAllForWorker(member.getMemberId());
        Map<Long, ContractLaborTerms> termsByVersionId = contractLaborTermsRepository
                .findAllById(contracts.stream()
                        .map(Contract::getCurrentVersion)
                        .filter(Objects::nonNull)
                        .map(ContractVersion::getId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(terms -> terms.getContractVersion().getId(), Function.identity()));

        List<Item> items = contracts.stream()
                .map(contract -> toItem(contract, employer, termsByVersionId))
                .sorted(Comparator.comparing((Item item) -> item.todo() == null)
                        .thenComparing(Item::updatedAt, Comparator.reverseOrder())
                        .thenComparing(Item::contractId, Comparator.reverseOrder()))
                .toList();
        List<Item> filtered = status == null
                ? items
                : items.stream().filter(item -> item.status() == status).toList();

        int from = (int) Math.min((long) page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return new ContractListResponse(
                new Counts(
                        items.size(),
                        count(items, ContractStatus.IN_PROGRESS),
                        count(items, ContractStatus.CONFIRMED),
                        count(items, ContractStatus.TERMINATED)),
                (int) items.stream().filter(item -> item.todo() != null).count(),
                filtered.subList(from, to),
                new PageInfo(page, size, filtered.size(), (filtered.size() + size - 1) / size));
    }

    /**
     * 계약 상세를 조회한다.
     *
     * <p>계약 상세 API(M2)에서 쓴다. 항목 설명은 현재 회차의 마지막 완료 번역본에서 가져오며,
     * 근로자에게는 모국어 쉬운 설명, 고용주·관리자에게는 한국어 설명을 준다. 진행 이력은 이어진 항목 수정을 한 줄로 묶고,
     * 할 수 있는 일은 역할과 stage, 진행 상태로 정한다.</p>
     *
     * @param contractId 계약 ID
     * @param member 로그인한 당사자 또는 관리자
     * @return 계약 상세 화면 데이터
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우
     */
    public ContractDetailResponse detail(Long contractId, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractVersion version = contract.getCurrentVersion();
        ContractFile file = version.getContractFile();
        Company company = contract.getCompany();
        Member worker = contract.getWorker();
        ProcessingResponse processing = contractProcessingService.resolve(contract);

        return new ContractDetailResponse(
                contract.getId(),
                contract.getContractNo(),
                contract.getStatus(),
                contract.getStage(),
                processing,
                contract.getConfirmationNo(),
                contract.getConfirmedAt(),
                contract.getTerminatedAt(),
                new ContractDetailResponse.Employer(company.getName(), company.getRepresentativeName()),
                new ContractDetailResponse.Worker(
                        worker.getId(),
                        worker.getName(),
                        memberWorkerProfileRepository
                                .findById(worker.getId())
                                .map(MemberWorkerProfile::getNationality)
                                .orElse(null)),
                contract.getPreviousContract() == null
                        ? null
                        : contract.getPreviousContract().getId(),
                new ContractDetailResponse.File(
                        file.getId(), file.getFileName(), file.getMimeType(), file.getSizeBytes(), file.getPageCount()),
                terms(version, member.getRole() == MemberRole.WORKER),
                timeline(contract),
                chainRecords(contract),
                null,
                availableActions(contract, member.getRole(), processing));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 목록의 계약 한 줄을 만든다.
     *
     * @param contract 계약
     * @param employer 로그인한 사람이 고용주인지
     * @param termsByVersionId 회차 ID → 항목 값
     * @return 목록의 계약
     */
    private Item toItem(Contract contract, boolean employer, Map<Long, ContractLaborTerms> termsByVersionId) {
        ContractVersion version = contract.getCurrentVersion();
        Optional<ContractLaborTerms> terms =
                Optional.ofNullable(version == null ? null : termsByVersionId.get(version.getId()));
        return new Item(
                contract.getId(),
                contract.getContractNo(),
                employer
                        ? contract.getWorker().getName()
                        : contract.getCompany().getName(),
                terms.map(t -> (LocalDate) t.getValue(TermField.START_DATE)).orElse(null),
                terms.map(t -> (LocalDate) t.getValue(TermField.END_DATE)).orElse(null),
                contract.getStatus(),
                contract.getStage(),
                employer ? employerTodo(contract) : workerTodo(contract),
                contract.getUpdatedAt());
    }

    /**
     * 고용주가 해야 할 일을 정한다.
     *
     * @param contract 계약
     * @return 할 일, 없으면 {@code null}
     */
    private ContractTodo employerTodo(Contract contract) {
        return switch (contract.getStage()) {
            case RETURNED -> ContractTodo.FIX_REQUIRED;
            case REVIEWING -> ContractTodo.REVIEW_INCOMPLETE;
            case UPLOADED -> failed(contract) ? ContractTodo.EXTRACTION_FAILED : null;
            case TRANSLATING -> failed(contract) ? ContractTodo.TRANSLATION_FAILED : null;
            default -> null;
        };
    }

    /**
     * 근로자가 해야 할 일을 정한다.
     *
     * @param contract 계약
     * @return 할 일, 없으면 {@code null}
     */
    private static ContractTodo workerTodo(Contract contract) {
        return contract.getStage() == ContractStage.AWAITING_WORKER ? ContractTodo.CONFIRM_REQUIRED : null;
    }

    /**
     * 진행 중인 작업(추출·번역)이 실패해 다시 시도해야 하는지 확인한다.
     *
     * @param contract 계약
     * @return 마지막 시도가 실패했으면 {@code true}, 그렇지 않으면 {@code false}
     */
    private boolean failed(Contract contract) {
        ProcessingResponse processing = contractProcessingService.resolve(contract);
        return processing != null && processing.status() == ProcessingStatus.FAILED;
    }

    /**
     * 주어진 상태의 계약 수를 센다.
     *
     * @param items 목록의 계약
     * @param status 상태
     * @return 계약 수
     */
    private static long count(List<Item> items, ContractStatus status) {
        return items.stream().filter(item -> item.status() == status).count();
    }

    /**
     * 회차에서 값이 있는 항목을 설명과 함께 만든다.
     *
     * @param version 회차
     * @param forWorker 근로자에게 보여 주는지 (모국어 설명)
     * @return 값이 있는 항목 (TermField 순서), 추출 전이면 빈 목록
     */
    private List<Term> terms(ContractVersion version, boolean forWorker) {
        Optional<ContractLaborTerms> terms = contractLaborTermsRepository.findById(version.getId());
        if (terms.isEmpty()) {
            return List.of();
        }
        Map<TermField, ContractTermAnnotation> annotations =
                contractTermAnnotationRepository.findWithClauseByContractVersionId(version.getId()).stream()
                        .collect(Collectors.toMap(ContractTermAnnotation::getFieldName, Function.identity()));
        Map<TermField, TranslationItem> translated = translationRepository
                .findFirstByContractVersionIdAndStatusOrderByTranslationNoDesc(version.getId(), TranslationStatus.DONE)
                .map(translation -> translationItemRepository.findByTranslationId(translation.getId()).stream()
                        .collect(Collectors.toMap(TranslationItem::getFieldName, Function.identity())))
                .orElse(Map.of());

        List<Term> result = new ArrayList<>();
        for (TermField field : TermField.values()) {
            Object value = terms.get().getValue(field);
            if (value == null) {
                continue;
            }
            TranslationItem item = translated.get(field);
            String explanation =
                    item == null ? null : forWorker ? item.getEasyExplanation() : item.getEasyExplanationKo();
            result.add(new Term(
                    field.getKey(),
                    field.getLabel(),
                    // 시각은 명세대로 "09:00"으로 보낸다. (Jackson 기본값은 "09:00:00")
                    value instanceof LocalTime ? value.toString() : value,
                    annotations.get(field).getValueSource(),
                    explanation));
        }
        return result;
    }

    /**
     * 진행 이력을 만든다. 이어진 TERM_EDITED는 한 줄로 묶고(시각은 마지막 수정, 키는 합침) 내부 ID는 내보내지 않는다.
     *
     * @param contract 계약
     * @return 일어난 순서의 진행 이력
     */
    private List<TimelineEntry> timeline(Contract contract) {
        List<TimelineEntry> timeline = new ArrayList<>();
        Set<Object> editedKeys = new LinkedHashSet<>();
        for (ContractEvent event : contractEventRepository.findWithActorByContractId(contract.getId())) {
            int last = timeline.size() - 1;
            if (event.getEventType() == ContractEventType.TERM_EDITED) {
                boolean continues = last >= 0 && timeline.get(last).type() == ContractEventType.TERM_EDITED;
                if (!continues) {
                    editedKeys = new LinkedHashSet<>();
                }
                editedKeys.addAll(listOf(event.getPayload().get("keys")));
                TimelineEntry entry = new TimelineEntry(
                        event.getEventType(),
                        event.getCreatedAt(),
                        actorLabel(contract, event.getActor()),
                        Map.of("keys", List.copyOf(editedKeys)));
                if (continues) {
                    timeline.set(last, entry);
                } else {
                    timeline.add(entry);
                }
                continue;
            }
            timeline.add(new TimelineEntry(
                    event.getEventType(),
                    event.getCreatedAt(),
                    actorLabel(contract, event.getActor()),
                    detail(contract, event)));
        }
        return timeline;
    }

    /**
     * 이벤트를 일으킨 사람을 화면에 보여 줄 이름으로 바꾼다.
     *
     * @param contract 계약
     * @param actor 행동한 회원
     * @return 고용주는 사업체명, 그 밖에는 회원 이름, 서버가 한 일이면 {@code null}
     */
    private static String actorLabel(Contract contract, Member actor) {
        if (actor == null) {
            return null;
        }
        return actor.hasRole(MemberRole.EMPLOYER) ? contract.getCompany().getName() : actor.getName();
    }

    /**
     * 화면에 의미 있는 이벤트의 상세를 만든다.
     *
     * @param contract 계약
     * @param event 이벤트
     * @return 상세, 보여 줄 내용이 없으면 {@code null}
     */
    private static Map<String, Object> detail(Contract contract, ContractEvent event) {
        Map<String, Object> payload = event.getPayload();
        Map<String, Object> detail = new LinkedHashMap<>();
        switch (event.getEventType()) {
            case WORKER_UNDERSTOOD -> detail.put("language", contract.getTargetLanguage());
            case WORKER_REPORTED -> {
                detail.put("keys", payload.get("keys"));
                detail.put("reasonKo", payload.get("reasonKo"));
            }
            case EXTRACTION_FAILED, TRANSLATION_FAILED -> detail.put("error", payload.get("error"));
            default -> {
                return null;
            }
        }
        return detail;
    }

    /**
     * 계약의 블록체인 기록을 만든다.
     *
     * @param contract 계약
     * @return 만든 순서의 블록체인 기록
     */
    private List<ChainRecordItem> chainRecords(Contract contract) {
        return chainRecordRepository.findByContractVersionContractIdOrderByIdAsc(contract.getId()).stream()
                .map(ChainRecordItem::of)
                .toList();
    }

    /**
     * 로그인한 사람이 지금 할 수 있는 일을 정한다.
     *
     * @param contract 계약
     * @param role 로그인한 사람의 역할
     * @param processing 진행 중인 작업
     * @return 할 수 있는 일
     */
    private static List<ContractAction> availableActions(
            Contract contract, MemberRole role, ProcessingResponse processing) {
        List<ContractAction> actions = new ArrayList<>();
        boolean failed = processing != null && processing.status() == ProcessingStatus.FAILED;
        if (role == MemberRole.EMPLOYER) {
            switch (contract.getStage()) {
                case REVIEWING -> actions.add(ContractAction.EDIT_DRAFT);
                case RETURNED -> actions.add(ContractAction.START_REVISION);
                case UPLOADED -> {
                    if (failed) {
                        actions.add(ContractAction.RETRY_EXTRACTION);
                    }
                }
                case TRANSLATING -> {
                    if (failed) {
                        actions.add(ContractAction.RETRY_TRANSLATION);
                    }
                }
                default -> {}
            }
        }
        if (role == MemberRole.WORKER && contract.getStage() == ContractStage.AWAITING_WORKER) {
            actions.add(ContractAction.RESPOND_REVIEW);
        }
        actions.add(ContractAction.DOWNLOAD_FILE);
        return actions;
    }

    /**
     * payload의 목록 값을 꺼낸다.
     *
     * @param value payload 값
     * @return 목록, 목록이 아니면 빈 목록
     */
    private static List<?> listOf(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }
}
