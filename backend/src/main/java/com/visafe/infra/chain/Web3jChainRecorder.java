package com.visafe.infra.chain;

import com.visafe.domain.chain.ChainTarget;
import com.visafe.domain.chain.enums.RecordType;
import com.visafe.domain.chain.recorder.ChainReceipt;
import com.visafe.domain.chain.recorder.ChainTransactionRecorder;
import jakarta.annotation.PreDestroy;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionDecoder;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

/** 배포된 레지스트리의 ABI와 실제 Ethereum RPC를 연결한다. */
@Component
@ConditionalOnExpression(ChainCondition.ENABLED)
public class Web3jChainRecorder implements ChainTransactionRecorder {

    private static final String RECORD_EVENT = EventEncoder.encode(ContractAnchorRegistry.RECORDWRITTEN_EVENT);
    private static final String ZERO_HASH = "0x" + "0".repeat(64);
    private static final BigInteger LOG_WINDOW = BigInteger.valueOf(9999);
    private static final StaticGasProvider READ_ONLY_GAS = new StaticGasProvider(BigInteger.ZERO, BigInteger.ZERO);

    private final Web3j web3j;
    private final Credentials writer;
    private final BigInteger deploymentBlock;
    private final ChainTarget configuredTarget;

    public Web3jChainRecorder(ChainProperties properties) {
        configuredTarget = properties.sepoliaTarget();
        if (properties.deploymentBlock() < 0) {
            throw new IllegalArgumentException("SEPOLIA_DEPLOYMENT_BLOCK은 음수일 수 없습니다.");
        }
        deploymentBlock = BigInteger.valueOf(properties.deploymentBlock());
        writer = loadWriter(properties.writerPrivateKey());
        String rpcUrl = validateRpcUrl(properties.rpcUrl());
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(4))
                .callTimeout(Duration.ofSeconds(8))
                .build();
        web3j = Web3j.build(new HttpService(rpcUrl, httpClient, false));
    }

    @Override
    public ChainTarget configuredTarget() {
        return configuredTarget;
    }

    @PreDestroy
    void close() {
        web3j.shutdown();
    }

    @Override
    public String prepare(Write write) {
        ChainTarget target = write.target();
        requireChain(target);
        String code = rpc("컨트랙트 확인", web3j.ethGetCode(target.contractAddress(), DefaultBlockParameterName.LATEST))
                .getCode();
        if (code == null || code.equals("0x")) {
            // 주소·네트워크 설정 문제는 설정을 고친 뒤 이어서 처리되도록 재시도 대상으로 둔다.
            throw new IllegalStateException("Sepolia에 컨트랙트 코드가 없습니다.");
        }

        ContractAnchorRegistry registry = registry(target);
        String onChainWriter = call("기록 지갑 조회", () -> registry.writer().send());
        if (!writer.getAddress().equalsIgnoreCase(onChainWriter)) {
            throw new IllegalStateException("설정된 지갑에 Sepolia 기록 권한이 없습니다.");
        }

        String data = registry.record(
                        Numeric.hexStringToByteArray(write.anchorKey()),
                        typeNumber(write.recordType()),
                        Numeric.hexStringToByteArray(write.recordHash()))
                .encodeFunctionCall();
        BigInteger nonce = rpc(
                        "nonce 조회",
                        web3j.ethGetTransactionCount(writer.getAddress(), DefaultBlockParameterName.PENDING))
                .getTransactionCount();
        BigInteger gasPrice = rpc("가스 가격 조회", web3j.ethGasPrice()).getGasPrice();
        EthEstimateGas estimate = call(
                "가스 사용량 조회",
                () -> web3j.ethEstimateGas(Transaction.createEthCallTransaction(
                                writer.getAddress(), target.contractAddress(), data))
                        .send());
        if (estimate.hasError()) {
            if (isRevert(estimate.getError())) {
                throw new Rejected("Sepolia 기록 트랜잭션이 실행 전 검사에서 거절되었습니다.");
            }
            throw new IllegalStateException(
                    "Sepolia 가스 사용량 조회 RPC 오류 (" + estimate.getError().getCode() + ").");
        }
        return sign(nonce, gasPrice, estimate.getAmountUsed(), target, data);
    }

    @Override
    public String replace(Write write, String signedTransaction) {
        ChainTarget target = write.target();
        requireChain(target);
        RawTransaction previous = TransactionDecoder.decode(signedTransaction);
        if (!target.contractAddress().equalsIgnoreCase(previous.getTo())) {
            throw new IllegalStateException("교체할 서명본의 대상 컨트랙트가 기록과 다릅니다.");
        }
        // 노드는 같은 nonce의 교체 트랜잭션에 10% 이상 높은 수수료를 요구한다.
        BigInteger bumped = previous.getGasPrice()
                .multiply(BigInteger.valueOf(9))
                .divide(BigInteger.valueOf(8))
                .add(BigInteger.ONE);
        BigInteger current = rpc("가스 가격 조회", web3j.ethGasPrice()).getGasPrice();
        return sign(previous.getNonce(), bumped.max(current), previous.getGasLimit(), target, previous.getData());
    }

    @Override
    public String transactionHash(String signedTransaction) {
        return Hash.sha3(signedTransaction);
    }

    @Override
    public void broadcast(ChainTarget target, String signedTransaction) {
        requireChain(target);
        var response = call(
                "트랜잭션 전송", () -> web3j.ethSendRawTransaction(signedTransaction).send());
        if (response.hasError()) {
            String message = Optional.ofNullable(response.getError().getMessage())
                    .orElse("")
                    .toLowerCase(Locale.ROOT);
            if (message.contains("already known") || message.equals("known transaction")) {
                return;
            }
            if (message.contains("nonce too low") || message.contains("underpriced")) {
                throw new Dropped(
                        "Sepolia가 서명본을 받지 않습니다 (" + response.getError().getCode() + ").");
            }
            throw new IllegalStateException(
                    "Sepolia 트랜잭션 전송 응답 오류 (" + response.getError().getCode() + ").");
        }
        if (!Hash.sha3(signedTransaction).equalsIgnoreCase(response.getTransactionHash())) {
            throw new IllegalStateException("Sepolia 응답의 트랜잭션 해시가 서명본과 일치하지 않습니다.");
        }
    }

    @Override
    public Optional<ChainReceipt> receipt(Write write, String txHash) {
        requireChain(write.target());
        return rpc("영수증 조회", web3j.ethGetTransactionReceipt(txHash))
                .getTransactionReceipt()
                .map(receipt -> verifyReceipt(write, receipt));
    }

    @Override
    public Optional<ChainReceipt> findExisting(Write write) {
        Optional<String> existing = readRecordHash(write.target(), write.anchorKey(), write.recordType());
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        if (!existing.get().equals(write.recordHash())) {
            throw new Rejected("같은 키와 종류에 다른 해시가 이미 기록되어 있습니다.");
        }

        BigInteger end = rpc("최신 블록 조회", web3j.ethBlockNumber()).getBlockNumber();
        while (end.compareTo(deploymentBlock) >= 0) {
            BigInteger start = end.subtract(LOG_WINDOW).max(deploymentBlock);
            EthFilter filter = new EthFilter(
                    DefaultBlockParameter.valueOf(start),
                    DefaultBlockParameter.valueOf(end),
                    write.target().contractAddress());
            filter.addSingleTopic(RECORD_EVENT);
            filter.addSingleTopic(write.anchorKey());
            filter.addSingleTopic(typeTopic(write.recordType()));
            for (var result : rpc("기록 이벤트 조회", web3j.ethGetLogs(filter)).getLogs()) {
                if (result.get() instanceof Log log && matchesRecord(write, log)) {
                    TransactionReceipt original = rpc(
                                    "원래 영수증 조회", web3j.ethGetTransactionReceipt(log.getTransactionHash()))
                            .getTransactionReceipt()
                            .orElseThrow(() -> new IllegalStateException("Sepolia 원래 영수증을 기다리는 중입니다."));
                    if (hasRecordEvent(write, original)) {
                        return Optional.of(toReceipt(original));
                    }
                }
            }
            end = start.subtract(BigInteger.ONE);
        }
        throw new IllegalStateException("Sepolia에 같은 해시가 있어 원래 기록 이벤트를 기다리는 중입니다.");
    }

    @Override
    public Optional<String> readRecordHash(ChainTarget target, String anchorKey, RecordType recordType) {
        requireChain(target);
        String hash = call(
                "묶음 해시 조회",
                () -> Numeric.toHexString(registry(target)
                        .getRecordHash(Numeric.hexStringToByteArray(anchorKey), typeNumber(recordType))
                        .send()));
        return ZERO_HASH.equals(hash) ? Optional.empty() : Optional.of(hash);
    }

    private String sign(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, ChainTarget target, String data) {
        BigInteger balance = rpc(
                        "기록 지갑 잔액 조회", web3j.ethGetBalance(writer.getAddress(), DefaultBlockParameterName.PENDING))
                .getBalance();
        if (balance.compareTo(gasLimit.multiply(gasPrice)) < 0) {
            throw new IllegalStateException("Sepolia 기록 지갑의 수수료 잔액이 부족합니다.");
        }
        RawTransaction transaction =
                RawTransaction.createTransaction(nonce, gasPrice, gasLimit, target.contractAddress(), data);
        return Numeric.toHexString(TransactionEncoder.signMessage(transaction, target.chainId(), writer));
    }

    private static boolean isRevert(Response.Error error) {
        String message = Optional.ofNullable(error.getMessage()).orElse("").toLowerCase(Locale.ROOT);
        return error.getCode() == 3 || message.contains("revert");
    }

    private ChainReceipt verifyReceipt(Write write, TransactionReceipt receipt) {
        if (!receipt.isStatusOK()) {
            // estimate 뒤 다른 요청이 먼저 반영된 경우도 원래 기록을 확인한다.
            return findExisting(write).orElseThrow(() -> new Rejected("Sepolia 트랜잭션이 블록에서 거절되었습니다."));
        }
        if (hasRecordEvent(write, receipt)) {
            return toReceipt(receipt);
        }
        return findExisting(write).orElseThrow(() -> new Rejected("영수증에 일치하는 Sepolia 기록이 없습니다."));
    }

    private static boolean hasRecordEvent(Write write, TransactionReceipt receipt) {
        return receipt.isStatusOK() && receipt.getLogs().stream().anyMatch(log -> matchesRecord(write, log));
    }

    private static boolean matchesRecord(Write write, Log log) {
        var topics = log.getTopics();
        return write.target().contractAddress().equalsIgnoreCase(log.getAddress())
                && topics.size() == 3
                && RECORD_EVENT.equalsIgnoreCase(topics.get(0))
                && write.anchorKey().equalsIgnoreCase(topics.get(1))
                && typeTopic(write.recordType()).equalsIgnoreCase(topics.get(2))
                && log.getData() != null
                && log.getData().length() == 130
                && log.getData().regionMatches(true, 0, write.recordHash(), 0, 66);
    }

    private void requireChain(ChainTarget target) {
        BigInteger actual = rpc("체인 ID 조회", web3j.ethChainId()).getChainId();
        if (!BigInteger.valueOf(target.chainId()).equals(actual)) {
            throw new IllegalStateException("설정된 RPC와 기록의 체인 ID가 일치하지 않습니다.");
        }
    }

    private ContractAnchorRegistry registry(ChainTarget target) {
        return ContractAnchorRegistry.load(
                target.contractAddress(),
                web3j,
                new ReadonlyTransactionManager(web3j, writer.getAddress()),
                READ_ONLY_GAS);
    }

    private static ChainReceipt toReceipt(TransactionReceipt receipt) {
        return new ChainReceipt(
                receipt.getTransactionHash(), receipt.getBlockNumber().longValueExact());
    }

    private static BigInteger typeNumber(RecordType type) {
        return switch (type) {
            case CONFIRM -> BigInteger.ZERO;
            case TERMINATE -> BigInteger.ONE;
        };
    }

    private static String typeTopic(RecordType type) {
        return "0x" + TypeEncoder.encode(new Uint8(typeNumber(type)));
    }

    private static Credentials loadWriter(String key) {
        if (key == null || !key.matches("(?:0x)?[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("SEPOLIA_WRITER_PRIVATE_KEY 형식이 올바르지 않습니다.");
        }
        try {
            return Credentials.create(key);
        } catch (RuntimeException ignored) {
            throw new IllegalArgumentException("SEPOLIA_WRITER_PRIVATE_KEY 값이 올바르지 않습니다.");
        }
    }

    private static String validateRpcUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getHost() != null && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) {
                return url;
            }
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // URL에 포함될 수 있는 키를 예외에 남기지 않는다.
        }
        throw new IllegalArgumentException("SEPOLIA_RPC_URL 형식이 올바르지 않습니다.");
    }

    private static <T extends Response<?>> T rpc(String operation, Request<?, T> request) {
        T response = call(operation, request::send);
        if (response.hasError()) {
            throw new IllegalStateException(
                    "Sepolia " + operation + " RPC 오류 (" + response.getError().getCode() + ").");
        }
        return response;
    }

    private static <T> T call(String operation, Callable<T> action) {
        try {
            return action.call();
        } catch (Exception ignored) {
            // 외부 예외 메시지에 RPC 키 또는 서명된 트랜잭션이 포함될 수 있다.
            throw new IllegalStateException("Sepolia " + operation + " 실패.");
        }
    }
}
