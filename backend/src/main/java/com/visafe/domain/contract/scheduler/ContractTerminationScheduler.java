package com.visafe.domain.contract.scheduler;

import com.visafe.domain.contract.repository.ContractRepository;
import com.visafe.domain.contract.repository.ContractTerminationRepository;
import com.visafe.domain.contract.service.ContractEndService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 0시(한국 시간)에 종료일이 지난 계약을 끝낸다. 종료일은 마지막 근무일이므로 다음 날 0시에 끝낸다.
 * <p>
 * 합의 종료(AGREED)를 먼저 처리하고 그다음 계약 종료일이 지난 계약을 기간 만료로 처리한다.
 * 건마다 트랜잭션을 따로 써서 한 건이 실패해도 나머지는 처리하며, "종료일이 지난 것"을 찾으므로 하루를 건너뛰어도 다음 실행에서 처리된다.
 * 서버가 한 대라는 전제다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContractTerminationScheduler {

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final ContractTerminationRepository contractTerminationRepository;
    private final ContractRepository contractRepository;
    private final ContractEndService contractEndService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void run() {
        endDue(LocalDate.now(KOREA));
    }

    /** 주어진 날짜 기준으로 종료일이 지난 계약을 끝낸다. */
    public void endDue(LocalDate today) {
        for (Long terminationId : contractTerminationRepository.findDueMutualIds(today)) {
            try {
                contractEndService.endAgreed(terminationId, today);
            } catch (RuntimeException e) {
                log.error("[계약 종료] 합의 종료 처리 실패 - terminationId={}", terminationId, e);
            }
        }
        for (Long contractId : contractRepository.findExpiredIds(today)) {
            try {
                contractEndService.endExpired(contractId, today);
            } catch (RuntimeException e) {
                log.error("[계약 종료] 기간 만료 처리 실패 - contractId={}", contractId, e);
            }
        }
    }
}
