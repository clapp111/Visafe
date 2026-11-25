package com.visafe.infra.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.visafe.domain.chain.ChainTarget;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.chain.recorder.ChainReceipt;
import com.visafe.domain.chain.recorder.ChainTransactionRecorder;
import com.visafe.domain.chain.recorder.ChainTransactionRecorder.Write;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionDecoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

/** 실제 Hardhat EVM에서 배포 ABI, 서명, 기록 이벤트와 조회를 확인한다. */
@EnabledIfEnvironmentVariable(named = "IT_CHAIN_ADDRESS", matches = "0x[0-9a-fA-F]{40}")
@EnabledIfEnvironmentVariable(named = "IT_CHAIN_RPC_URL", matches = "http://127\\.0\\.0\\.1:[0-9]+")
class Web3jChainRecorderEvmTest {

    @Test
    void signsRecordsAndRecoversAnExistingRecordFromItsOriginalReceipt() throws Exception {
        String rpcUrl = System.getenv("IT_CHAIN_RPC_URL");
        assertThat(URI.create(rpcUrl).getHost()).isEqualTo("127.0.0.1");
        String address = System.getenv("IT_CHAIN_ADDRESS");
        HttpService service = new HttpService(rpcUrl);
        Web3j node = Web3j.build(service);
        try {
            long chainId = node.ethChainId().send().getChainId().longValueExact();
            assertThat(chainId).isEqualTo(31337L);
            ChainTarget target = new ChainTarget(chainId, address);
            Credentials testWriter = Credentials.create(Keys.createEcKeyPair());
            String administrator = node.ethAccounts().send().getAccounts().getFirst();
            ContractAnchorRegistry registry = ContractAnchorRegistry.load(
                    address,
                    node,
                    new ClientTransactionManager(node, administrator),
                    new StaticGasProvider(node.ethGasPrice().send().getGasPrice(), BigInteger.valueOf(100_000)));
            assertThat(registry.setWriter(testWriter.getAddress()).send().isStatusOK())
                    .isTrue();
            var funding = new Request<>(
                            "hardhat_setBalance",
                            List.of(testWriter.getAddress(), "0x1000000000000000"),
                            service,
                            Response.class)
                    .send();
            assertThat(funding.hasError()).isFalse();

            ChainProperties properties = new ChainProperties(
                    rpcUrl,
                    address,
                    Numeric.toHexStringWithPrefixZeroPadded(
                            testWriter.getEcKeyPair().getPrivateKey(), 64),
                    0);
            Web3jChainRecorder recorder = new Web3jChainRecorder(properties);
            try {
                assertThat(recorder.configuredTarget()).isEqualTo(new ChainTarget(11155111L, address));
                String anchor = "0x" + "1".repeat(64);
                String confirmHash = "0x" + "2".repeat(64);
                Write confirmation = new Write(target, anchor, RecordType.CONFIRM, confirmHash);
                Write termination = new Write(target, anchor, RecordType.TERMINATE, "0x" + "3".repeat(64));
                assertThat(recorder.findExisting(confirmation)).isEmpty();
                assertThatThrownBy(() -> recorder.prepare(termination))
                        .isInstanceOf(ChainTransactionRecorder.Rejected.class);

                ChainReceipt recorded = write(recorder, confirmation);
                assertThat(recorder.readRecordHash(target, anchor, RecordType.CONFIRM))
                        .contains(confirmHash);
                assertThat(recorder.findExisting(confirmation)).contains(recorded);
                assertThat(write(recorder, confirmation)).isEqualTo(recorded);
                assertThatThrownBy(() -> recorder.findExisting(
                                new Write(target, anchor, RecordType.CONFIRM, "0x" + "4".repeat(64))))
                        .isInstanceOf(ChainTransactionRecorder.Rejected.class);

                ChainReceipt ended = write(recorder, termination);
                assertThat(ended.blockNumber()).isGreaterThan(recorded.blockNumber());
                assertThat(recorder.readRecordHash(target, anchor, RecordType.TERMINATE))
                        .contains(termination.recordHash());
                assertThatThrownBy(() -> recorder.readRecordHash(
                                new ChainTarget(11155111L, address), anchor, RecordType.CONFIRM))
                        .isInstanceOf(IllegalStateException.class);

                // 교체 서명본은 같은 nonce에 더 높은 수수료를 쓰고, 그 뒤 원래 서명본은 Dropped로 거절된다.
                Write next = new Write(target, "0x" + "5".repeat(64), RecordType.CONFIRM, "0x" + "6".repeat(64));
                String original = recorder.prepare(next);
                String replacement = recorder.replace(next, original);
                RawTransaction before = TransactionDecoder.decode(original);
                RawTransaction after = TransactionDecoder.decode(replacement);
                assertThat(after.getNonce()).isEqualTo(before.getNonce());
                assertThat(after.getGasPrice()).isGreaterThan(before.getGasPrice());
                recorder.broadcast(target, replacement);
                assertThat(recorder.receipt(next, recorder.transactionHash(replacement)))
                        .isPresent();
                assertThatThrownBy(() -> recorder.broadcast(target, original))
                        .isInstanceOf(ChainTransactionRecorder.Dropped.class);
            } finally {
                recorder.close();
            }
        } finally {
            node.shutdown();
        }
    }

    private static ChainReceipt write(Web3jChainRecorder recorder, Write write) {
        String signed = recorder.prepare(write);
        recorder.broadcast(write.target(), signed);
        return recorder.receipt(write, Hash.sha3(signed)).orElseThrow();
    }
}
