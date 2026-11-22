package com.visafe.domain.contract;

import com.visafe.domain.company.entity.Company;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.entity.ContractVersion;
import com.visafe.domain.contract.enums.ContractStage;
import com.visafe.domain.member.entity.Member;

/** 저장하지 않은 계약 엔티티를 원하는 stage까지 만든다. (단위 테스트용) */
public final class ContractFixtures {

    public static final String CONTRACT_NO = "C-2026-00001";

    private ContractFixtures() {}

    /** 1회차가 붙은 UPLOADED 계약 */
    public static Contract uploaded() {
        Company company = Company.create("한빛정밀", "김민수", "000-00-00001", "경기도 안산시 단원구 데모로 1");
        Member employer = Member.employer("employer@demo.visafe.test", "hash", "김민수", company);
        Member worker = Member.worker("worker1@demo.visafe.test", "hash", "Nguyễn Văn An");
        Contract contract = Contract.create(CONTRACT_NO, company, employer, worker, "vi", null, false);
        ContractFile file = ContractFile.builder()
                .contract(contract)
                .storageKey("local/contracts/1/a.pdf")
                .fileName("계약서.pdf")
                .mimeType("application/pdf")
                .sizeBytes(10)
                .fileHash("0x" + "0".repeat(64))
                .uploadedBy(employer)
                .build();
        contract.changeCurrentVersion(ContractVersion.create(contract, 1, file));
        return contract;
    }

    /** 원하는 stage의 계약. RECORDING은 확인번호를 정한 상태로 만든다. */
    public static Contract inStage(ContractStage stage) {
        Contract contract = uploaded();
        ContractStage[] path = {ContractStage.REVIEWING, ContractStage.TRANSLATING, ContractStage.AWAITING_WORKER};
        for (ContractStage next : path) {
            if (contract.getStage() == stage) {
                return contract;
            }
            contract.changeStage(next);
        }
        if (stage == ContractStage.AWAITING_WORKER) {
            return contract;
        }
        if (stage == ContractStage.RECORDING) {
            contract.startRecording("VS-7K3F-9QXM");
            return contract;
        }
        throw new IllegalArgumentException("지원하지 않는 stage: " + stage);
    }
}
