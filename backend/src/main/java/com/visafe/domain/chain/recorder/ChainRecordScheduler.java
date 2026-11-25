package com.visafe.domain.chain.recorder;

import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.recorder.ChainTransactionRecorder.Write;
import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.infra.chain.ChainCondition;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DB에 저장한 서명본과 영수증을 기준으로 실제 블록체인 기록을 보내고, 중단된 기록을 이어서 처리한다.
 * <p>
 * 기록 지갑의 nonce가 꼬이지 않도록 서명본은 한 번에 하나만 둔다. 일시적인 실패는 전송 대기로 두고 간격을 늘려 계속 시도하며,
 * 온체인에서 거절된 경우만 FAILED로 끝낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression(ChainCondition.ENABLED)
public class ChainRecordScheduler {

    private static final Duration RECEIPT_POLL_DELAY = Duration.ofSeconds(12);
    /** 보낸 뒤 이 시간 동안 블록에 포함되지 않으면 수수료를 올려 같은 nonce로 다시 보낸다. */
    private static final Duration REPLACE_AFTER = Duration.ofMinutes(3);

    private static final long MAX_RETRY_DELAY_SECONDS = Duration.ofMinutes(10).toSeconds();

    private final ChainRecordRepository repository;
    private final ChainTransactionRecorder recorder;
    private final ChainRecordWorker worker;
    private final TransactionTemplate transactions;

    @Scheduled(fixedDelayString = "${visafe.chain.poll-delay:3000}")
    public void poll() {
        Instant now = Instant.now();
        Entry entry = transactions.execute(status -> repository
                .findFirstByNetworkAndSignedTransactionIsNotNull(ChainRecord.SEPOLIA_NETWORK)
                .or(() -> repository.findNextSepoliaRecord(now))
                .filter(record -> record.getNextAttemptAt() == null
                        || !record.getNextAttemptAt().isAfter(now))
                .map(Entry::of)
                .orElse(null));
        if (entry == null) {
            return;
        }

        try {
            advance(entry);
        } catch (ChainTransactionRecorder.Rejected e) {
            log.warn("[블록체인 기록 거절] id={}, reason={}", entry.event().chainRecordId(), e.getMessage());
            worker.fail(entry.event());
        } catch (RuntimeException e) {
            log.warn("[블록체인 기록 대기] id={}, reason={}", entry.event().chainRecordId(), e.getMessage());
            deferAfterFailure(entry);
        }
    }

    private void advance(Entry entry) {
        if (entry.signedTransaction() == null) {
            Optional<ChainReceipt> existing = recorder.findExisting(entry.write());
            if (existing.isPresent()) {
                worker.complete(entry.event(), existing.get());
                return;
            }
            String signed = recorder.prepare(entry.write());
            Entry prepared;
            try {
                prepared = savePrepared(entry, signed);
            } catch (DataIntegrityViolationException e) {
                // 다른 작업자가 먼저 서명본을 저장했다. 다음 실행은 그 서명본부터 처리한다.
                log.debug("[블록체인 기록 경합] id={}", entry.event().chainRecordId());
                return;
            }
            if (prepared != null) {
                sendOrComplete(prepared);
            }
        } else {
            sendOrComplete(entry);
        }
    }

    private Entry savePrepared(Entry entry, String signedTransaction) {
        return transactions.execute(status -> {
            ChainRecord record = lockedRecord(entry);
            if (record.getStatus() != ChainRecordStatus.PENDING) {
                return null;
            }
            if (record.getSignedTransaction() == null) {
                record.prepareTransaction(signedTransaction);
            }
            return Entry.of(record);
        });
    }

    private void sendOrComplete(Entry entry) {
        String signed = entry.signedTransaction();
        Optional<ChainReceipt> receipt = recorder.receipt(entry.write(), recorder.transactionHash(signed));
        if (receipt.isPresent()) {
            worker.complete(entry.event(), receipt.get());
            return;
        }

        if (entry.sentAt() != null && entry.sentAt().plus(REPLACE_AFTER).isBefore(Instant.now())) {
            String replacement = recorder.replace(entry.write(), signed);
            if (!saveReplacement(entry, signed, replacement)) {
                return;
            }
            signed = replacement;
        }

        try {
            recorder.broadcast(entry.write().target(), signed);
        } catch (ChainTransactionRecorder.Dropped e) {
            log.info("[블록체인 기록 재서명] id={}, reason={}", entry.event().chainRecordId(), e.getMessage());
            discard(entry, signed);
            return;
        }
        String sent = signed;
        String txHash = recorder.transactionHash(sent);
        transactions.executeWithoutResult(status -> {
            ChainRecord record = lockedRecord(entry);
            if (!Objects.equals(record.getSignedTransaction(), sent)) {
                return;
            }
            Instant now = Instant.now();
            if (record.getStatus() == ChainRecordStatus.PENDING) {
                record.markSent(txHash, now);
            }
            record.defer(now.plus(RECEIPT_POLL_DELAY), false);
        });
    }

    /** 오래 대기한 서명본을 수수료를 올린 서명본으로 바꾼다. 그사이 다른 작업자가 바꿨으면 false. */
    private boolean saveReplacement(Entry entry, String previous, String replacement) {
        String txHash = recorder.transactionHash(replacement);
        return Boolean.TRUE.equals(transactions.execute(status -> {
            ChainRecord record = lockedRecord(entry);
            if (record.getStatus() != ChainRecordStatus.SENT
                    || !Objects.equals(record.getSignedTransaction(), previous)) {
                return false;
            }
            record.replaceTransaction(replacement, txHash, Instant.now());
            return true;
        }));
    }

    /** 더는 기록할 수 없는 서명본을 버린다. 다음 실행은 기존 기록부터 다시 확인하고 새로 서명한다. */
    private void discard(Entry entry, String signedTransaction) {
        transactions.executeWithoutResult(status -> {
            ChainRecord record = lockedRecord(entry);
            if (!Objects.equals(record.getSignedTransaction(), signedTransaction)) {
                return;
            }
            record.discardTransaction(Instant.now().plus(RECEIPT_POLL_DELAY));
        });
    }

    private void deferAfterFailure(Entry entry) {
        transactions.executeWithoutResult(status -> {
            ChainRecord record = lockedRecord(entry);
            if (record.getStatus() != ChainRecordStatus.PENDING && record.getStatus() != ChainRecordStatus.SENT) {
                return;
            }
            // 네트워크·잔액·설정 문제는 고친 뒤 이어서 처리되도록 FAILED로 끝내지 않고 간격만 늘린다.
            long seconds = Math.min(5L << Math.min(record.getRetryCount(), 7), MAX_RETRY_DELAY_SECONDS);
            record.defer(Instant.now().plusSeconds(seconds), true);
        });
    }

    private ChainRecord lockedRecord(Entry entry) {
        return repository.findByIdForUpdate(entry.event().chainRecordId()).orElseThrow();
    }

    private record Entry(ChainRecordRequestedEvent event, Write write, String signedTransaction, Instant sentAt) {
        static Entry of(ChainRecord record) {
            return new Entry(
                    new ChainRecordRequestedEvent(
                            record.getContractVersion().getContract().getId(), record.getId()),
                    new Write(record.target(), record.getAnchorKey(), record.getRecordType(), record.getRecordHash()),
                    record.getSignedTransaction(),
                    record.getSentAt());
        }
    }
}
