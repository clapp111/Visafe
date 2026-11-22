package com.visafe.domain.translation.translator;

import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractClause;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractEventType;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.contract.repository.ContractClauseRepository;
import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.service.ContractEventRecorder;
import com.visafe.domain.notification.enums.NotificationType;
import com.visafe.domain.notification.service.NotificationPublisher;
import com.visafe.domain.term.entity.ContractLaborTerms;
import com.visafe.domain.term.enums.TermField;
import com.visafe.domain.term.repository.ContractLaborTermsRepository;
import com.visafe.domain.translation.TranslationHash;
import com.visafe.domain.translation.entity.Translation;
import com.visafe.domain.translation.entity.TranslationClause;
import com.visafe.domain.translation.entity.TranslationItem;
import com.visafe.domain.translation.repository.TranslationClauseRepository;
import com.visafe.domain.translation.repository.TranslationItemRepository;
import com.visafe.domain.translation.repository.TranslationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
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
 * 제출(R6)이나 번역 다시 시도(R7) 트랜잭션이 커밋된 뒤 번역을 실행하고 결과를 저장한다.
 * <p>
 * 번역은 오래 걸리므로 트랜잭션 밖에서 하고, 시도 시작·결과 저장·실패 기록만 짧은 트랜잭션으로 한다.
 * 실패하면 바로 다시 시도해 한 번역본에 최대 3회까지 시도하고, 3회 모두 실패하면 고용주에게 알린다.
 * 그사이 계약이 다음 단계로 넘어갔거나 새 번역본이 생기면 이 번역본의 결과는 버린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractTranslationWorker {

    /** 번역 실패 시 고용주에게 보여 줄 사유 (자세한 원인은 로그에 남김) */
    static final String FAILURE_MESSAGE = "계약서를 번역하지 못했습니다. 다시 시도해 주세요.";

    private final ContractRepository contractRepository;
    private final ContractClauseRepository contractClauseRepository;
    private final ContractLaborTermsRepository contractLaborTermsRepository;
    private final TranslationRepository translationRepository;
    private final TranslationClauseRepository translationClauseRepository;
    private final TranslationItemRepository translationItemRepository;
    private final ContractEventRecorder contractEventRecorder;
    private final NotificationPublisher notificationPublisher;
    private final ContractTranslator contractTranslator;
    private final TransactionTemplate transactionTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranslationRequested(TranslationRequestedEvent event) {
        translate(event);
    }

    /** 번역을 실행한다. 시도할 수 있는 동안 다시 시도하고, 끝까지 실패하면 TRANSLATION_FAILED를 남긴다. */
    public void translate(TranslationRequestedEvent event) {
        TranslationSource source = transactionTemplate.execute(status -> loadSource(event));
        if (source == null) {
            log.info(
                    "[번역] 지난 번역본이라 실행하지 않음 - contractId={}, translationId={}",
                    event.contractId(),
                    event.translationId());
            return;
        }
        boolean retry = true;
        while (retry && Boolean.TRUE.equals(transactionTemplate.execute(status -> start(event)))) {
            try {
                TranslationResult result = contractTranslator.translate(source);
                validate(source, result);
                transactionTemplate.executeWithoutResult(status -> save(event, result));
                return;
            } catch (RuntimeException e) {
                log.warn("[번역] 실패 - contractId={}, translationId={}", event.contractId(), event.translationId(), e);
                retry = Boolean.TRUE.equals(transactionTemplate.execute(status -> recordFailure(event)));
            }
        }
    }

    /** 번역할 원문 조항과 값이 있는 항목을 읽는다. 지난 번역본이면 null. */
    private TranslationSource loadSource(TranslationRequestedEvent event) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        Translation translation =
                translationRepository.findById(event.translationId()).orElseThrow();
        if (!isCurrent(contract, translation)) {
            return null;
        }
        ContractVersion version = contract.getCurrentVersion();
        List<TranslationSource.Clause> clauses = clausesOf(version).stream()
                .map(clause -> new TranslationSource.Clause(clause.getClauseNo(), clause.getTextKo()))
                .toList();
        ContractLaborTerms terms =
                contractLaborTermsRepository.findById(version.getId()).orElseThrow();
        Map<TermField, String> values = new EnumMap<>(TermField.class);
        for (TermField field : TermField.values()) {
            Object value = terms.getValue(field);
            if (value != null) {
                values.put(field, value.toString());
            }
        }
        return new TranslationSource(translation.getLanguage(), clauses, values);
    }

    /** 시도를 시작한다(시도 횟수 +1). 지난 번역본이면 false. */
    private boolean start(TranslationRequestedEvent event) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        Translation translation =
                translationRepository.findById(event.translationId()).orElseThrow();
        if (!isCurrent(contract, translation)) {
            return false;
        }
        translation.start();
        return true;
    }

    private void save(TranslationRequestedEvent event, TranslationResult result) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        Translation translation =
                translationRepository.findById(event.translationId()).orElseThrow();
        if (!isCurrent(contract, translation)) {
            log.info(
                    "[번역] 지난 번역본의 결과를 버림 - contractId={}, translationId={}", event.contractId(), event.translationId());
            return;
        }

        List<ContractClause> sourceClauses = clausesOf(contract.getCurrentVersion());
        List<TranslationClause> clauses = new ArrayList<>();
        for (int i = 0; i < sourceClauses.size(); i++) {
            clauses.add(TranslationClause.of(
                    translation, sourceClauses.get(i), result.clauses().get(i)));
        }
        List<TranslationItem> items = new ArrayList<>();
        result.items()
                .forEach((field, item) -> items.add(TranslationItem.builder()
                        .translation(translation)
                        .fieldName(field)
                        .translatedValue(item.translatedValue())
                        .easyExplanation(item.easyExplanation())
                        .easyExplanationKo(item.easyExplanationKo())
                        .build()));
        translationClauseRepository.saveAll(clauses);
        translationItemRepository.saveAll(items);

        translation.complete(TranslationHash.of(translation.getLanguage(), clauses, items), Instant.now());
        contract.changeStage(ContractStage.AWAITING_WORKER);
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.TRANSLATION_COMPLETED,
                Map.of("translationId", translation.getId(), "translationNo", translation.getTranslationNo()));
        notificationPublisher.publish(contract.getWorker(), contract, NotificationType.REVIEW_REQUESTED);
    }

    /** 실패를 기록한다. 더 시도할 수 있으면 true, 마지막 시도였으면 고용주에게 알리고 false. */
    private boolean recordFailure(TranslationRequestedEvent event) {
        Contract contract = contractRepository.findById(event.contractId()).orElseThrow();
        Translation translation =
                translationRepository.findById(event.translationId()).orElseThrow();
        if (!isCurrent(contract, translation)) {
            return false;
        }
        translation.fail(FAILURE_MESSAGE);
        if (translation.canRetry()) {
            return true;
        }
        contractEventRecorder.recordBySystem(
                contract,
                ContractEventType.TRANSLATION_FAILED,
                Map.of("translationId", translation.getId(), "error", FAILURE_MESSAGE));
        notificationPublisher.publish(contract.getEmployer(), contract, NotificationType.TRANSLATION_FAILED);
        return false;
    }

    private List<ContractClause> clausesOf(ContractVersion version) {
        return contractClauseRepository.findByContractFileIdOrderBySeqAsc(
                version.getContractFile().getId());
    }

    /** 계약이 번역 중이고 이 번역본이 현재 회차의 마지막 번역본인지. */
    private boolean isCurrent(Contract contract, Translation translation) {
        ContractVersion version = contract.getCurrentVersion();
        return contract.getStage() == ContractStage.TRANSLATING
                && version != null
                && version.getId().equals(translation.getContractVersion().getId())
                && translationRepository
                        .findFirstByContractVersionIdOrderByTranslationNoDesc(version.getId())
                        .map(latest -> latest.getId().equals(translation.getId()))
                        .orElse(false);
    }

    /** 번역 결과가 원문 조항·항목과 맞는지 검사한다. 맞지 않으면 실패한 시도로 본다. */
    private static void validate(TranslationSource source, TranslationResult result) {
        if (result.clauses().size() != source.clauses().size()) {
            throw new IllegalStateException("조항 번역 개수가 다릅니다. 원문: "
                    + source.clauses().size() + ", 번역: " + result.clauses().size());
        }
        if (!result.items().keySet().equals(source.terms().keySet())) {
            throw new IllegalStateException(
                    "번역된 항목이 원문 항목과 다릅니다: " + result.items().keySet());
        }
    }
}
