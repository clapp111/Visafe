package com.visafe.global.storage;

/** 파일 저장소 호출 실패. 원인은 S3 SDK 예외이며, 전역 예외 처리에서 500으로 응답한다. */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
