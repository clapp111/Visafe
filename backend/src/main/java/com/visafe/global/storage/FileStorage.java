package com.visafe.global.storage;

/**
 * 파일 저장소. 계약서 원본을 저장하고, 브라우저가 직접 받을 수 있는 Presigned URL을 발급한다.
 * <p>
 * 저장한 바이트는 절대 바꾸지 않는다. 내려받은 원본의 keccak256이 블록체인에 기록된 file_hash와 같아야
 * 공개 조회의 파일 대조가 맞기 때문이다.
 */
public interface FileStorage {

    /**
     * 파일을 저장한다.
     *
     * @param key 환경 접두사를 뺀 경로 (예: contracts/31/1b4e….pdf). 원래 파일 이름은 넣지 않는다
     * @param content 파일 바이트
     * @param contentType MIME 형식
     * @return 환경 접두사를 붙인 저장 키. contract_files.storage_key에 그대로 저장한다
     */
    String store(String key, byte[] content, String contentType);

    /** 저장된 파일을 읽는다. (AI 추출 등 서버 안에서 원본이 필요할 때) */
    byte[] read(String storageKey);

    /**
     * 브라우저가 S3에서 직접 받는 Presigned GET URL을 발급한다.
     *
     * @param storageKey {@link #store}가 돌려준 저장 키
     * @param fileName 내려받을 때 쓸 파일 이름 (한글 가능)
     * @param contentType 응답 Content-Type
     * @param inline true면 미리보기(inline), false면 내려받기(attachment)
     */
    PresignedDownload presignDownload(String storageKey, String fileName, String contentType, boolean inline);

    /**
     * 파일을 지운다. DB 저장이 롤백되어 아무도 참조하지 않는 파일을 정리할 때만 쓴다.
     * 버전 관리가 켜진 버킷에서는 삭제 마커만 남고 이전 버전은 보존된다.
     */
    void delete(String storageKey);
}
