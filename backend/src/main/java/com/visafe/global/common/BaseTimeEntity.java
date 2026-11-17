package com.visafe.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * created_at, updated_at이 모두 있는 테이블(값이 바뀌는 테이블)의 공통 부모 클래스.
 * DB 트리거(set_updated_at)도 updated_at을 갱신하므로 두 값은 같은 의미를 가진다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedTimeEntity {

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
