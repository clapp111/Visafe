package com.visafe.domain.contract.service;

import com.visafe.domain.contract.dto.response.FileDownloadUrlResponse;
import com.visafe.domain.contract.entity.Contract;
import com.visafe.domain.contract.entity.ContractFile;
import com.visafe.domain.contract.enums.ContractFileType;
import com.visafe.domain.contract.repository.ContractFileRepository;
import com.visafe.domain.member.entity.Member;
import com.visafe.domain.member.entity.MemberDetails;
import com.visafe.global.crypto.Keccak256;
import com.visafe.global.exception.CustomErrorCode;
import com.visafe.global.exception.RestApiException;
import com.visafe.global.storage.FileStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractFileService {

    /** contract_files.file_name 길이 */
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final ContractFileRepository contractFileRepository;
    private final ContractAccessService contractAccessService;
    private final FileStorage fileStorage;

    // =========================================================================
    // ============================= 주요 서비스 기능 =============================
    // =========================================================================

    /**
     * 계약서 원본을 S3에 저장하고 원본 파일 행을 만든다.
     *
     * <p>형식은 파일 앞 바이트로 판별하며 PDF·JPG·PNG만 받는다. 저장한 바이트의 keccak256을 file_hash로 남기고,
     * PDF는 쪽 수를 센다(이미지는 1쪽). 호출한 쪽 트랜잭션이 롤백되면 S3에 올린 파일도 지운다.</p>
     *
     * @param contract 원본이 속한 계약 (저장되어 ID가 있어야 함)
     * @param upload 올린 파일
     * @param uploader 올린 고용주
     * @return 저장한 원본 파일
     * @throws RestApiException PDF·JPG·PNG가 아니거나 PDF를 읽을 수 없는 경우
     */
    @Transactional
    public ContractFile saveOriginal(Contract contract, MultipartFile upload, Member uploader) {
        byte[] content = readBytes(upload);
        ContractFileType type = ContractFileType.detect(content)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.FILE_UNSUPPORTED_TYPE));
        int pageCount = countPages(type, content);

        String key = "contracts/" + contract.getId() + "/" + UUID.randomUUID() + "." + type.getExtension();
        String storageKey = fileStorage.store(key, content, type.getMimeType());
        deleteOnRollback(storageKey);

        return contractFileRepository.save(ContractFile.builder()
                .contract(contract)
                .storageKey(storageKey)
                .fileName(fileName(upload, type))
                .mimeType(type.getMimeType())
                .sizeBytes(content.length)
                .pageCount(pageCount)
                .fileHash(Keccak256.hash(content))
                .uploadedBy(uploader)
                .build());
    }

    /**
     * 원본 파일을 브라우저가 S3에서 직접 받을 수 있는 URL을 발급한다.
     *
     * @param contractId 계약 ID
     * @param fileId 원본 파일 ID
     * @param inline 미리보기면 {@code true}, 내려받기면 {@code false}
     * @param member 로그인한 회원
     * @return Presigned GET URL과 파일 정보
     * @throws RestApiException 계약이 없거나 볼 권한이 없는 경우, 또는 그 계약의 파일이 아닌 경우
     */
    public FileDownloadUrlResponse getDownloadUrl(Long contractId, Long fileId, boolean inline, MemberDetails member) {
        Contract contract = contractAccessService.getAccessibleContract(contractId, member);
        ContractFile file = contractFileRepository
                .findByIdAndContractId(fileId, contract.getId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.FILE_NOT_FOUND));
        return FileDownloadUrlResponse.of(
                file,
                fileStorage.presignDownload(file.getStorageKey(), file.getFileName(), file.getMimeType(), inline));
    }

    // =========================================================================
    // ================================ 헬퍼 함수 ================================
    // =========================================================================

    /**
     * 올린 파일의 바이트를 읽는다.
     *
     * @param upload 올린 파일
     * @return 파일 바이트
     */
    private static byte[] readBytes(MultipartFile upload) {
        try {
            return upload.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 원본의 쪽 수를 센다. 암호가 걸렸거나 깨진 PDF는 받지 않는다.
     *
     * @param type 원본 형식
     * @param content 원본 바이트
     * @return 쪽 수 (이미지는 1)
     * @throws RestApiException PDF를 읽을 수 없는 경우
     */
    private static int countPages(ContractFileType type, byte[] content) {
        if (type != ContractFileType.PDF) {
            return 1;
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            throw new RestApiException(CustomErrorCode.FILE_UNSUPPORTED_TYPE, "PDF 파일을 읽을 수 없습니다. 암호가 없는 PDF로 올려 주세요.");
        }
    }

    /**
     * 트랜잭션이 롤백되면 S3에 올린 파일을 지운다. (아무도 참조하지 않는 파일 정리)
     *
     * @param storageKey 올린 파일의 저장 키
     */
    private void deleteOnRollback(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    fileStorage.delete(storageKey);
                } catch (RuntimeException e) {
                    log.warn("롤백된 원본 파일을 지우지 못함: {}", storageKey, e);
                }
            }
        });
    }

    /**
     * 저장할 파일 이름. 경로는 떼고, 없으면 형식에 맞춰 만들고, 너무 길면 확장자가 남도록 앞을 자른다.
     *
     * @param upload 올린 파일
     * @param type 원본 형식
     * @return contract_files.file_name에 저장할 이름
     */
    private static String fileName(MultipartFile upload, ContractFileType type) {
        String name = StringUtils.getFilename(upload.getOriginalFilename());
        if (!StringUtils.hasText(name)) {
            return "contract." + type.getExtension();
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(name.length() - MAX_FILE_NAME_LENGTH) : name;
    }
}
