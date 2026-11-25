// SPDX-License-Identifier: MIT
pragma solidity 0.8.35;

/// @notice 근로계약의 확정·종료 묶음 해시를 기록한다.
contract ContractAnchorRegistry {
    // Web3j에 전달하는 ABI 값은 CONFIRM=0, TERMINATE=1이다.
    enum RecordType {
        CONFIRM,
        TERMINATE
    }

    error UnauthorizedAdmin();
    error UnauthorizedWriter();
    error InvalidWriter();
    error InvalidRecord();
    error ConfirmationRequired();
    error RecordAlreadyWritten();

    event WriterChanged(address indexed previousWriter, address indexed newWriter);
    event RecordWritten(
        bytes32 indexed anchorKey,
        RecordType indexed recordType,
        bytes32 recordHash,
        address writer
    );

    address public immutable admin;
    address public writer;

    mapping(bytes32 => mapping(RecordType => bytes32)) private records;

    constructor() {
        admin = msg.sender;
        writer = msg.sender;
    }

    function setWriter(address newWriter) external {
        if (msg.sender != admin) revert UnauthorizedAdmin();
        if (newWriter == address(0)) revert InvalidWriter();

        address previousWriter = writer;
        writer = newWriter;
        emit WriterChanged(previousWriter, newWriter);
    }

    /// @notice 같은 키·종류·해시의 재요청은 새 이벤트를 남기지 않는다.
    function record(bytes32 anchorKey, RecordType recordType, bytes32 recordHash) external {
        if (msg.sender != writer) revert UnauthorizedWriter();
        if (anchorKey == bytes32(0) || recordHash == bytes32(0)) revert InvalidRecord();

        bytes32 existingHash = records[anchorKey][recordType];
        if (existingHash == recordHash) return;
        if (existingHash != bytes32(0)) revert RecordAlreadyWritten();
        if (recordType == RecordType.TERMINATE && records[anchorKey][RecordType.CONFIRM] == bytes32(0)) {
            revert ConfirmationRequired();
        }

        records[anchorKey][recordType] = recordHash;
        emit RecordWritten(anchorKey, recordType, recordHash, msg.sender);
    }

    /// @notice 기록이 없으면 0을 반환한다.
    function getRecordHash(bytes32 anchorKey, RecordType recordType) external view returns (bytes32) {
        return records[anchorKey][recordType];
    }
}
