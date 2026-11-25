package com.visafe.infra.chain;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.CustomError;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 5.0.3.
 */
@SuppressWarnings("rawtypes")
@Generated("org.web3j.codegen.SolidityFunctionWrapperGenerator")
public class ContractAnchorRegistry extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_GETRECORDHASH = "getRecordHash";

    public static final String FUNC_RECORD = "record";

    public static final String FUNC_SETWRITER = "setWriter";

    public static final String FUNC_WRITER = "writer";

    public static final CustomError CONFIRMATIONREQUIRED_ERROR =
            new CustomError("ConfirmationRequired", Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError INVALIDRECORD_ERROR =
            new CustomError("InvalidRecord", Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError INVALIDWRITER_ERROR =
            new CustomError("InvalidWriter", Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError RECORDALREADYWRITTEN_ERROR =
            new CustomError("RecordAlreadyWritten", Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError UNAUTHORIZEDADMIN_ERROR =
            new CustomError("UnauthorizedAdmin", Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError UNAUTHORIZEDWRITER_ERROR =
            new CustomError("UnauthorizedWriter", Arrays.<TypeReference<?>>asList());
    ;

    public static final Event RECORDWRITTEN_EVENT = new Event(
            "RecordWritten",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Bytes32>(true) {},
                    new TypeReference<Uint8>(true) {},
                    new TypeReference<Bytes32>() {},
                    new TypeReference<Address>() {}));
    ;

    public static final Event WRITERCHANGED_EVENT = new Event(
            "WriterChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    @Deprecated
    protected ContractAnchorRegistry(
            String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ContractAnchorRegistry(
            String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ContractAnchorRegistry(
            String contractAddress,
            Web3j web3j,
            TransactionManager transactionManager,
            BigInteger gasPrice,
            BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ContractAnchorRegistry(
            String contractAddress,
            Web3j web3j,
            TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<RecordWrittenEventResponse> getRecordWrittenEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList =
                staticExtractEventParametersWithLog(RECORDWRITTEN_EVENT, transactionReceipt);
        ArrayList<RecordWrittenEventResponse> responses = new ArrayList<RecordWrittenEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RecordWrittenEventResponse typedResponse = new RecordWrittenEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.anchorKey =
                    (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.recordType =
                    (BigInteger) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.recordHash =
                    (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.writer =
                    (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RecordWrittenEventResponse getRecordWrittenEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(RECORDWRITTEN_EVENT, log);
        RecordWrittenEventResponse typedResponse = new RecordWrittenEventResponse();
        typedResponse.log = log;
        typedResponse.anchorKey = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.recordType =
                (BigInteger) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.recordHash =
                (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.writer = (String) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<RecordWrittenEventResponse> recordWrittenEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRecordWrittenEventFromLog(log));
    }

    public Flowable<RecordWrittenEventResponse> recordWrittenEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RECORDWRITTEN_EVENT));
        return recordWrittenEventFlowable(filter);
    }

    public static List<WriterChangedEventResponse> getWriterChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList =
                staticExtractEventParametersWithLog(WRITERCHANGED_EVENT, transactionReceipt);
        ArrayList<WriterChangedEventResponse> responses = new ArrayList<WriterChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            WriterChangedEventResponse typedResponse = new WriterChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousWriter =
                    (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.newWriter =
                    (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static WriterChangedEventResponse getWriterChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(WRITERCHANGED_EVENT, log);
        WriterChangedEventResponse typedResponse = new WriterChangedEventResponse();
        typedResponse.log = log;
        typedResponse.previousWriter =
                (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.newWriter = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<WriterChangedEventResponse> writerChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getWriterChangedEventFromLog(log));
    }

    public Flowable<WriterChangedEventResponse> writerChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(WRITERCHANGED_EVENT));
        return writerChangedEventFlowable(filter);
    }

    public RemoteFunctionCall<String> admin() {
        final Function function = new Function(
                FUNC_ADMIN, Arrays.<Type>asList(), Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<byte[]> getRecordHash(byte[] anchorKey, BigInteger recordType) {
        final Function function = new Function(
                FUNC_GETRECORDHASH,
                Arrays.<Type>asList(
                        new org.web3j.abi.datatypes.generated.Bytes32(anchorKey),
                        new org.web3j.abi.datatypes.generated.Uint8(recordType)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> record(byte[] anchorKey, BigInteger recordType, byte[] recordHash) {
        final Function function = new Function(
                FUNC_RECORD,
                Arrays.<Type>asList(
                        new org.web3j.abi.datatypes.generated.Bytes32(anchorKey),
                        new org.web3j.abi.datatypes.generated.Uint8(recordType),
                        new org.web3j.abi.datatypes.generated.Bytes32(recordHash)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setWriter(String newWriter) {
        final Function function = new Function(
                FUNC_SETWRITER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newWriter)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> writer() {
        final Function function = new Function(
                FUNC_WRITER, Arrays.<Type>asList(), Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static ContractAnchorRegistry load(
            String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ContractAnchorRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ContractAnchorRegistry load(
            String contractAddress,
            Web3j web3j,
            TransactionManager transactionManager,
            BigInteger gasPrice,
            BigInteger gasLimit) {
        return new ContractAnchorRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ContractAnchorRegistry load(
            String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ContractAnchorRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ContractAnchorRegistry load(
            String contractAddress,
            Web3j web3j,
            TransactionManager transactionManager,
            ContractGasProvider contractGasProvider) {
        return new ContractAnchorRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class RecordWrittenEventResponse extends BaseEventResponse {
        public byte[] anchorKey;

        public BigInteger recordType;

        public byte[] recordHash;

        public String writer;
    }

    public static class WriterChangedEventResponse extends BaseEventResponse {
        public String previousWriter;

        public String newWriter;
    }
}
