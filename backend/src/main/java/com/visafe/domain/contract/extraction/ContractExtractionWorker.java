package com.visafe.domain.contract.extraction;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractClause;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.repository.ContractClauseRepository;
import com.visafe.domain.contract.repository.ContractFileRepository;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.service.ContractEventRecorder;
import com.visafe.domain.contract.service.ContractProcessingService;
import com.visafe.domain.term.TermValueParser;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.entity.ContractTermAnnotation;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.enums.WarningCode;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.term.repository.ContractTermAnnotationRepository;
import com.visafe.global.storage.FileStorage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 원본을 올린 트랜잭션이 커밋된 뒤 AI 추출을 실행하고 결과를 저장한다.
 * <p>
 * 원본 읽기와 AI 호출은 오래 걸리므로 트랜잭션 밖에서 하고, 저장만 짧은 트랜잭션으로 한다.
 * 추출하는 사이에 원본이 다시 올라오면(R2) 이전 파일의 결과는 버린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractExtractionWorker {

    /** 추출 실패 시 고용주에게 보여 줄 사유 (자세한 원인은 로그에 남김) */
    static final String FAILURE_MESSAGE = "AI가 계약서를 읽지 못했습니다. 다시 시도해 주세요.";

    private final ContractRepository contractRepository;
    private final ContractFileRepository contractFileRepository;
    private final ContractClauseRepository contractClauseRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final ContractTermAnnotationRepository contractTermAnnotationRepository;
    private final ContractEventRecorder contractEventRecorder;
    private final FileStorage fileStorage;
    private final ContractExtractor contractExtractor;
    private final TransactionTemplate transactionTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExtractionRequested(ExtractionRequestedEvent event) {
        extract(event);
    }

    /** 추출을 실행한다. 실패하면 EXTRACTION_FAILED 이벤트를 남겨 고용주가 다시 시도(R3)할 수 있게 한다. */
    public void extract(ExtractionRequestedEvent event) {
        try {
            ContractFile file = contractFileRepository.findById(event.fileId()).orElseThrow();
            byte[] content = fileStorage.read(file.getStorageKey());
            ExtractionResult result = contractExtractor.extract(content, file.getMimeType());
            transactionTemplate.executeWithoutResult(status -> save(event, result));
        } catch (RuntimeException e) {
            log.warn("AI 추출 실패: contractId={}, fileId={}", event.contractId(), event.fileId(), e);
            transactionTemplate.executeWithoutResult(status -> recordFailure(event));
        }
    }

    private void save(ExtractionRequestedEvent event, ExtractionResult result) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        if (!isCurrent(contract, event)) {
            log.info("지난 원본의 추출 결과를 버림: contractId={}, fileId={}", event.contractId(), event.fileId());
            return;
        }
        ContractVersion version = contract.getCurrentVersion();

        List<ContractClause> clauses = new ArrayList<>();
        for (int i = 0; i < result.clauses().size(); i++) {
            ExtractionResult.Clause clause = result.clauses().get(i);
            clauses.add(ContractClause.create(
                    version.getContractFile(), i + 1, clause.clauseNo(), clause.pageNo(), clause.text()));
        }
        contractClauseRepository.saveAll(clauses);

        ContractLaborTerms terms = ContractLaborTerms.empty(version);
        List<ContractTermAnnotation> annotations = new ArrayList<>();
        int unreadCount = 0;
        for (TermField field : TermField.values()) {
            ExtractionResult.Term term = result.terms().get(field);
            Object value = parseOrNull(field, term, terms);
            if (value == null) {
                annotations.add(ContractTermAnnotation.unread(version, field));
                unreadCount++;
                continue;
            }
            terms.changeValue(field, value);
            ContractTermAnnotation annotation =
                    ContractTermAnnotation.read(version, field, term.value().strip(), clauseAt(clauses, term));
            if (term.needsCheck()) {
                annotation.changeWarning(WarningCode.NEEDS_CHECK);
            }
            annotations.add(annotation);
        }
        contractLaborTermsRepository.save(terms);
        contractTermAnnotationRepository.saveAll(annotations);

        contract.changeStage(ContractStage.REVIEWING);
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.EXTRACTION_COMPLETED,
                Map.of("clauseCount", clauses.size(), "unreadCount", unreadCount));
    }

    private void recordFailure(ExtractionRequestedEvent event) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        if (!isCurrent(contract, event)) {
            return;
        }
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.EXTRACTION_FAILED,
                Map.of(ContractProcessingService.ERROR_PAYLOAD_KEY, FAILURE_MESSAGE));
    }

    /** 아직 이 원본으로 추출을 기다리는 상태인지 (원본이 다시 올라왔거나 이미 검토 단계면 false). */
    private static boolean isCurrent(Contract contract, ExtractionRequestedEvent event) {
        ContractVersion version = contract.getCurrentVersion();
        return contract.getStage() == ContractStage.UPLOADED
                && version != null
                && version.getId().equals(event.versionId())
                && version.getContractFile().getId().equals(event.fileId());
    }

    /**
     * AI가 읽은 값을 항목 타입으로 바꾼다. 비었거나 형식·범위가 틀리면 읽지 못한 것으로 본다.
     * 종료일이 시작일보다 앞서면(DB CHECK 위반) 종료일을 읽지 못한 것으로 본다.
     */
    private static Object parseOrNull(TermField field, ExtractionResult.Term term, ContractLaborTerms terms) {
        if (term == null || term.value() == null) {
            return null;
        }
        Object value;
        try {
            value = TermValueParser.parse(field, term.value());
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (field == TermField.END_DATE
                && terms.getValue(TermField.START_DATE) instanceof LocalDate start
                && ((LocalDate) value).isBefore(start)) {
            return null;
        }
        return value;
    }

    private static ContractClause clauseAt(List<ContractClause> clauses, ExtractionResult.Term term) {
        Integer index = term.clauseIndex();
        return index != null && index >= 0 && index < clauses.size() ? clauses.get(index) : null;
    }
}
