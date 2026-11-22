package com.visafe.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 오래 걸리는 작업(AI 추출, 번역, 블록체인 기록)을 요청 스레드 밖에서 실행한다.
 * <p>
 * 실행기는 Spring Boot 기본 실행기를 쓰며, 크기는 application.yml의 spring.task.execution에서 정한다.
 * 작업은 상태를 바꾼 트랜잭션이 커밋된 뒤에 시작해야 하므로
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)}와 {@code @Async}를 함께 쓴다.
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /** 반환값이 없는 비동기 작업의 예외는 호출한 쪽으로 돌아가지 않으므로 로그로 남긴다. */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("비동기 작업 실패: {}.{}", method.getDeclaringClass().getSimpleName(), method.getName(), ex);
    }
}
