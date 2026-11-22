package com.visafe.domain.lookup.service;

import com.visafe.domain.chain.ChainRecordHash;
import com.visafe.domain.chain.ConfirmationNumber;
import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.chain.recorder.ChainRecorder;
import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.domain.contract.dto.response.ContractDetailResponse.ChainRecordItem;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractEvent;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.enums.ContractStatus;
import com.visafe.domain.contract.enums.TerminationStatus;
import com.visafe.domain.contract.repository.ContractEventRepository;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.repository.ContractTerminationRepository;
import com.visafe.domain.lookup.WorkerNameMask;
import com.visafe.domain.lookup.dto.request.FileCheckRequest;
import com.visafe.domain.lookup.dto.response.FileCheckResponse;
import com.visafe.domain.lookup.dto.response.LookupResponse;
import com.visafe.domain.lookup.dto.response.LookupResponse.HistoryEntry;
import com.visafe.domain.lookup.dto.response.LookupResponse.Integrity;
import com.visafe.domain.lookup.dto.response.LookupResponse.IntegrityStatus;
import com.visafe.domain.lookup.dto.response.LookupResponse.KeyTerms;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.enums.MemberRole;
import com.visafe.domain.term.TermItemsHash;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.term.service.MinimumWageChecker;
import com.visafe.domain.translation.TranslationHash;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.repository.TranslationClauseRepository;
import com.visafe.domain.translation.repository.TranslationItemRepository;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LookupService {

    private static final String FILE_HASH_PATTERN = "^0x[0-9a-f]{64}$";

    /** 공개 조회에 보여 주는 이벤트 */
    private static final Set<ContractEventType> PUBLIC_EVENTS = EnumSet.of(
            ContractEventType.SUBMITTED,
            ContractEventType.WORKER_UNDERSTOOD,
            ContractEventType.AUTHORITY_APPROVED,
            ContractEventType.CONFIRMED,
            ContractEventType.TERMINATED);

    private final ContractRepository contractRepository;
    private final ContractEventRepository contractEventRepository;
    private final ContractTerminationRepository contractTerminationRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ChainRecordRepository chainRecordRepository;
    private final TranslationClauseRepository translationClauseRepository;
    private final TranslationItemRepository translationItemRepository;
    private final ChainRecorder chainRecorder;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 확인번호로 확정·종료된 계약의 공개 정보를 조회한다.
     *
     * <p>확인번호 조회 API(L1)에서 쓴다. 로그인 없이 부르므로 임금은 빼고 근로시간·계약기간만 공개하며, 근로자 이름은 가린다.
     * 이력은 확정 회차의 공개 이벤트만 보여 주고, 무결성은 DB 원본으로 다시 계산한 묶음 해시를 온체인 값과 비교해 정한다.</p>
     *
     * @param confirmationNo 사용자가 입력한 확인번호 (대소문자·하이픈 무관)
     * @return 공개 조회 결과
     * @throws RestApiException 확인번호 형식이 틀린 경우, 또는 확정·종료된 계약 중 그 번호가 없는 경우
     */
    public LookupResponse lookup(String confirmationNo) {
        Contract contract = findPublicContract(confirmationNo);
        ContractVersion version = contract.getCurrentVersion();
        ContractLaborTerms terms =
                contractLaborTermsRepository.findById(version.getId()).orElseThrow();
        Optional<ChainRecord> confirmRecord =
                chainRecordRepository.findFirstByContractVersionIdAndRecordTypeOrderByIdDesc(
                        version.getId(), RecordType.CONFIRM);

        return new LookupResponse(
                contract.getConfirmationNo(),
                contract.getStatus(),
                contract.getCompany().getName(),
                WorkerNameMask.mask(contract.getWorker().getName()),
                (LocalDate) terms.getValue(TermField.START_DATE),
                (LocalDate) terms.getValue(TermField.END_DATE),
                contract.getConfirmedAt(),
                keyTerms(terms),
                integrity(contract, version, terms, confirmRecord),
                termination(contract),
                chainRecordRepository.findByContractVersionContractIdOrderByIdAsc(contract.getId()).stream()
                        .map(ChainRecordItem::of)
                        .toList(),
                history(contract, version));
    }

    /**
     * 가지고 있는 계약서 파일의 해시를 확정 기록의 원본 파일 해시와 비교한다.
     *
     * <p>파일 대조 API(L2)에서 쓴다. 파일은 서버로 오지 않고 브라우저가 계산한 keccak256만 받는다.</p>
     *
     * @param confirmationNo 사용자가 입력한 확인번호 (대소문자·하이픈 무관)
     * @param request 브라우저가 계산한 파일 해시
     * @return 일치 여부와 두 해시
     * @throws RestApiException 확인번호 형식이 틀린 경우, 파일 해시가 0x + 소문자 16진수 64자리가 아닌 경우,
     *                          또는 확정·종료된 계약 중 그 번호가 없는 경우
     */
    public FileCheckResponse checkFile(String confirmationNo, FileCheckRequest request) {
        String normalized = normalize(confirmationNo);
        if (request.fileHash() == null || !request.fileHash().matches(FILE_HASH_PATTERN)) {
            throw new RestApiException(CustomErrorCode.FILE_HASH_INVALID_FORMAT);
        }
        Contract contract = findPublicContract(normalized);
        String recordedFileHash = chainRecordRepository
                .findFirstByContractVersionIdAndRecordTypeOrderByIdDesc(
                        contract.getCurrentVersion().getId(), RecordType.CONFIRM)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.LOOKUP_NOT_FOUND))
                .getFileHash();
        return new FileCheckResponse(recordedFileHash.equals(request.fileHash()), request.fileHash(), recordedFileHash);
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 확인번호를 정리하고 형식을 검사한다.
     *
     * @param confirmationNo 사용자가 입력한 확인번호
     * @return 정리한 확인번호 ({@code VS-XXXX-XXXX})
     * @throws RestApiException 형식이나 검사 문자가 틀린 경우
     */
    private static String normalize(String confirmationNo) {
        return ConfirmationNumber.normalize(confirmationNo)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.LOOKUP_INVALID_FORMAT));
    }

    /**
     * 확인번호로 확정(DONE)·종료(ENDED)된 계약을 찾는다.
     *
     * @param confirmationNo 사용자가 입력한 확인번호
     * @return 공개 조회할 수 있는 계약
     * @throws RestApiException 형식이 틀린 경우, 또는 확정·종료된 계약 중 그 번호가 없는 경우
     */
    private Contract findPublicContract(String confirmationNo) {
        return contractRepository
                .findByConfirmationNo(normalize(confirmationNo))
                .filter(contract ->
                        contract.getStage() == ContractStage.DONE || contract.getStage() == ContractStage.ENDED)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.LOOKUP_NOT_FOUND));
    }

    /**
     * 공개하는 근로시간 조건을 만든다.
     *
     * @param terms 확정 회차의 항목 값
     * @return 시업·종업 시각, 휴게시간, 주 근무일, 주 근로시간
     */
    private static KeyTerms keyTerms(ContractLaborTerms terms) {
        Object start = terms.getValue(TermField.WORK_START_TIME);
        Object end = terms.getValue(TermField.WORK_END_TIME);
        return new KeyTerms(
                start == null ? null : start.toString(),
                end == null ? null : end.toString(),
                (Integer) terms.getValue(TermField.BREAK_MINUTES),
                (Integer) terms.getValue(TermField.WORK_DAYS_PER_WEEK),
                (String) terms.getValue(TermField.WORK_DAYS_TEXT),
                MinimumWageChecker.weeklyHours(terms));
    }

    /**
     * 확정 기록의 무결성을 확인한다.
     *
     * <p>블록에 포함되기 전이면 PENDING이다. 포함됐으면 저장된 구성 요소 해시가 아니라 DB 원본(항목 값, 번역, 원본 파일 해시, 이력, 근로자)으로
     * 묶음 해시를 다시 계산해 온체인 값과 비교한다. 온체인에 기록이 없으면 MISMATCH, 체인을 읽지 못하면 UNAVAILABLE이다.</p>
     *
     * @param contract 계약
     * @param version 확정 회차
     * @param terms 확정 회차의 항목 값
     * @param confirmRecord 확정 기록
     * @return 무결성 상태와 확인 시각
     */
    private Integrity integrity(
            Contract contract, ContractVersion version, ContractLaborTerms terms, Optional<ChainRecord> confirmRecord) {
        Instant checkedAt = Instant.now();
        if (confirmRecord.isEmpty() || !confirmRecord.get().isMined()) {
            return new Integrity(IntegrityStatus.PENDING, checkedAt);
        }
        ChainRecord record = confirmRecord.get();
        String expected = recomputeRecordHash(contract, version, terms, record);
        try {
            IntegrityStatus status = chainRecorder
                    .readRecordHash(record.getAnchorKey())
                    .filter(expected::equals)
                    .map(onChain -> IntegrityStatus.VERIFIED)
                    .orElse(IntegrityStatus.MISMATCH);
            return new Integrity(status, checkedAt);
        } catch (RuntimeException e) {
            log.warn("[공개 조회] 온체인 기록을 읽지 못함 - contractId={}", contract.getId(), e);
            return new Integrity(IntegrityStatus.UNAVAILABLE, checkedAt);
        }
    }

    /**
     * DB 원본으로 확정 기록의 묶음 해시를 다시 계산한다.
     *
     * <p>이력 해시는 이 기록의 CHAIN_RECORD_REQUESTED 이벤트보다 앞선 이벤트로 계산한다. (기록을 시작할 때와 같은 범위)</p>
     *
     * @param contract 계약
     * @param version 확정 회차
     * @param terms 확정 회차의 항목 값
     * @param record 확정 기록
     * @return 다시 계산한 묶음 해시
     */
    private String recomputeRecordHash(
            Contract contract, ContractVersion version, ContractLaborTerms terms, ChainRecord record) {
        Long requestedEventId = contractEventRepository
                .findFirstByContractVersionIdAndEventTypeOrderByIdDesc(
                        version.getId(), ContractEventType.CHAIN_RECORD_REQUESTED)
                .map(ContractEvent::getId)
                .orElse(Long.MAX_VALUE);
        String eventsHash =
                ChainRecordHash.eventsHash(contractEventRepository.findSnapshotsByContractId(contract.getId()).stream()
                        .filter(event -> event.id() < requestedEventId)
                        .toList());
        Translation translation = record.getTranslation();
        String translationHash = TranslationHash.of(
                translation.getLanguage(),
                translationClauseRepository.findByTranslationId(translation.getId()),
                translationItemRepository.findByTranslationId(translation.getId()));
        return ChainRecordHash.confirmRecordHash(
                ChainRecordHash.anchorKey(contract.getConfirmationNo()),
                eventsHash,
                version.getContractFile().getFileHash(),
                TermItemsHash.of(terms),
                translationHash,
                ChainRecordHash.workerId(contract.getWorker().getId(), record.getNonce()));
    }

    /**
     * 종료된 계약의 종료 정보를 만든다.
     *
     * @param contract 계약
     * @return 종료 사유·종료일·종료 일시, 종료되지 않았으면 {@code null}
     */
    private LookupResponse.Termination termination(Contract contract) {
        if (contract.getStatus() != ContractStatus.TERMINATED) {
            return null;
        }
        return contractTerminationRepository
                .findFirstByContractIdAndStatusOrderByIdDesc(contract.getId(), TerminationStatus.AGREED)
                .map(termination -> new LookupResponse.Termination(
                        termination.getReasonType(), termination.getEndDate(), contract.getTerminatedAt()))
                .orElse(null);
    }

    /**
     * 확정 회차의 공개 이벤트를 만든다. 근로자 이름은 공개하지 않으므로 고용주가 한 일에만 사업체명을 붙인다.
     *
     * @param contract 계약
     * @param version 확정 회차
     * @return 일어난 순서의 공개 이벤트
     */
    private List<HistoryEntry> history(Contract contract, ContractVersion version) {
        return contractEventRepository.findVersionEvents(version.getId(), PUBLIC_EVENTS).stream()
                .map(event -> new HistoryEntry(
                        event.getEventType(),
                        event.getCreatedAt(),
                        actorLabel(contract, event.getActor()),
                        event.getEventType() == ContractEventType.WORKER_UNDERSTOOD
                                ? Map.of("language", contract.getTargetLanguage())
                                : null))
                .toList();
    }

    /**
     * 공개 이벤트에 보여 줄 행동한 쪽을 정한다.
     *
     * @param contract 계약
     * @param actor 행동한 회원
     * @return 고용주면 사업체명, 그 밖에는 {@code null}
     */
    private static String actorLabel(Contract contract, Member actor) {
        return actor != null && actor.hasRole(MemberRole.EMPLOYER)
                ? contract.getCompany().getName()
                : null;
    }
}
