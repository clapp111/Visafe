package com.visafe.global.common;

import java.util.Objects;
import java.util.function.Function;

/**
 * 엔티티 비교 도우미. 지연 로딩 프록시와 실제 객체를 같은 엔티티로 판단하기 위해 ID로 비교한다.
 */
public final class Entities {

    private Entities() {}

    /** 같은 객체이거나, 둘 다 저장되어 ID가 같으면 true. */
    @SuppressWarnings("ReferenceEquality") // 같은 인스턴스면 ID 비교 없이 true (의도한 참조 비교)
    public static <T> boolean isSame(T a, T b, Function<T, Long> idGetter) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        Long idA = idGetter.apply(a);
        return idA != null && Objects.equals(idA, idGetter.apply(b));
    }
}
