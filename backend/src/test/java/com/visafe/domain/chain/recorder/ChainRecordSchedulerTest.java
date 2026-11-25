package com.visafe.domain.chain.recorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.visafe.domain.chain.ChainTarget;
import com.visafe.domain.chain.entity.ChainRecord;
import com.visafe.domain.chain.enums.ChainRecordStatus;
import com.visafe.domain.chain.recorder.ChainTransactionRecorder.Write;
import com.visafe.domain.chain.repository.ChainRecordRepository;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.review.entity.ReviewResponse;
import com.visafe.domain.translation.entity.Translation;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** 블록체인 기록 스케줄러의 재시도·서명본 교체·재서명 흐름을 확인한다. */
class ChainRecordSchedulerTest {

    private static final ChainTarget TARGET = new ChainTarget(11155111L, "0x" + "a".repeat(40));
    private static final String SIGNED = "0x01";
    private static final String TX_HASH = "0x" + "1".repeat(64);

    private final ChainRecordRepository repository = mock(ChainRecordRepository.class);
    private final ChainTransactionRecorder recorder = mock(ChainTransactionRecorder.class);
    private final ChainRecordWorker worker = mock(ChainRecordWorker.class);
    private final ChainRecordScheduler scheduler =
            new ChainRecordScheduler(repository, recorder, worker, new TransactionTemplate(new NoTransactions()));

    private ChainRecord record;
    private Write write;

    @BeforeEach
    void setUp() {
        record = confirmRecord();
        write = new Write(TARGET, record.getAnchorKey(), record.getRecordType(), record.getRecordHash());
        when(repository.findFirstByNetworkAndSignedTransactionIsNotNull(ChainRecord.SEPOLIA_NETWORK))
                .thenAnswer(
                        invocation -> record.getSignedTransaction() == null ? Optional.empty() : Optional.of(record));
        when(repository.findNextSepoliaRecord(any()))
                .thenAnswer(invocation ->
                        record.getStatus() == ChainRecordStatus.PENDING && record.getSignedTransaction() == null
                                ? Optional.of(record)
                                : Optional.empty());
        when(repository.findByIdForUpdate(record.getId())).thenReturn(Optional.of(record));
        when(recorder.transactionHash(SIGNED)).thenReturn(TX_HASH);
    }

    @Test
    void transientFailuresKeepTheRecordPendingWithCappedBackoff() {
        when(recorder.findExisting(write)).thenThrow(new IllegalStateException("RPC 장애"));

        for (int attempt = 1; attempt <= 12; attempt++) {
            ReflectionTestUtils.setField(record, "nextAttemptAt", null);
            Instant before = Instant.now();
            scheduler.poll();

            assertThat(record.getStatus()).isEqualTo(ChainRecordStatus.PENDING);
            assertThat(record.getRetryCount()).isEqualTo(attempt);
            assertThat(record.getNextAttemptAt())
                    .isBefore(before.plus(Duration.ofMinutes(10).plusSeconds(1)));
        }
        assertThat(record.getNextAttemptAt()).isAfter(Instant.now().plus(Duration.ofMinutes(9)));
        verify(worker, never()).fail(any());
    }

    @Test
    void onChainRejectionFailsTheRecord() {
        when(recorder.findExisting(write)).thenReturn(Optional.empty());
        when(recorder.prepare(write)).thenThrow(new ChainTransactionRecorder.Rejected("revert"));

        scheduler.poll();

        verify(worker).fail(new ChainRecordRequestedEvent(1L, record.getId()));
    }

    @Test
    void droppedTransactionIsDiscardedAndSignedAgain() {
        record.prepareTransaction(SIGNED);
        record.markSent(TX_HASH, Instant.now());
        when(recorder.receipt(write, TX_HASH)).thenReturn(Optional.empty());
        doThrow(new ChainTransactionRecorder.Dropped("nonce too low"))
                .when(recorder)
                .broadcast(TARGET, SIGNED);

        scheduler.poll();

        assertThat(record.getStatus()).isEqualTo(ChainRecordStatus.PENDING);
        assertThat(record.getSignedTransaction()).isNull();
        assertThat(record.getTxHash()).isNull();
        assertThat(record.getSentAt()).isNull();
        assertThat(record.getRetryCount()).isEqualTo(1);
        verify(worker, never()).fail(any());
    }

    @Test
    void longUnminedTransactionIsReplacedWithTheSameNonce() {
        String replacement = "0x02";
        String replacementHash = "0x" + "2".repeat(64);
        record.prepareTransaction(SIGNED);
        record.markSent(TX_HASH, Instant.now().minus(Duration.ofMinutes(5)));
        when(recorder.receipt(write, TX_HASH)).thenReturn(Optional.empty());
        when(recorder.replace(write, SIGNED)).thenReturn(replacement);
        when(recorder.transactionHash(replacement)).thenReturn(replacementHash);

        scheduler.poll();

        assertThat(record.getStatus()).isEqualTo(ChainRecordStatus.SENT);
        assertThat(record.getSignedTransaction()).isEqualTo(replacement);
        assertThat(record.getTxHash()).isEqualTo(replacementHash);
        assertThat(record.getSentAt()).isAfter(Instant.now().minusSeconds(5));
        verify(recorder).broadcast(TARGET, replacement);
        verify(recorder, never()).broadcast(TARGET, SIGNED);
    }

    @Test
    void recentlySentTransactionIsBroadcastAgainWithoutReplacement() {
        record.prepareTransaction(SIGNED);
        record.markSent(TX_HASH, Instant.now());
        when(recorder.receipt(write, TX_HASH)).thenReturn(Optional.empty());

        scheduler.poll();

        verify(recorder).broadcast(TARGET, SIGNED);
        verify(recorder, never()).replace(any(), anyString());
        assertThat(record.getSignedTransaction()).isEqualTo(SIGNED);
        assertThat(record.getNextAttemptAt()).isAfter(Instant.now());
    }

    private static ChainRecord confirmRecord() {
        Contract contract = mock(Contract.class);
        when(contract.getId()).thenReturn(1L);
        ContractVersion version = mock(ContractVersion.class);
        when(version.getContract()).thenReturn(contract);
        ReviewResponse understood = mock(ReviewResponse.class);
        when(understood.isUnderstood()).thenReturn(true);
        ChainRecord record = ChainRecord.confirmBuilder()
                .contractVersion(version)
                .recordHash("0x" + "3".repeat(64))
                .anchorKey("0x" + "4".repeat(64))
                .nonce("0x" + "5".repeat(64))
                .fileHash("0x" + "6".repeat(64))
                .itemsHash("0x" + "7".repeat(64))
                .translation(mock(Translation.class))
                .translationHash("0x" + "8".repeat(64))
                .eventsHash("0x" + "9".repeat(64))
                .reviewResponse(understood)
                .target(TARGET)
                .build();
        ReflectionTestUtils.setField(record, "id", 7L);
        return record;
    }

    /** DB 없이 TransactionTemplate 콜백만 실행한다. */
    private static final class NoTransactions extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
