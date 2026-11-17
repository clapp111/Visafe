package com.visafe.domain.member.repository;

import com.visafe.domain.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /** 로그인 이메일은 저장 시 소문자로 정규화되므로, 조회 값도 {@link Member#normalizeEmail}을 거쳐 넘긴다. */
    Optional<Member> findByLoginEmail(String loginEmail);
}
