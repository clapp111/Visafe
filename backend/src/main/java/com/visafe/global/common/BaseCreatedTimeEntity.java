package com.visafe.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * created_at만 있는 테이블(추가 전용 기록, 한 번 쓰고 바뀌지 않는 데이터)의 공통 부모 클래스.
 */
@Getter
@MappedSuperclass
public abstract class BaseCreatedTimeEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
